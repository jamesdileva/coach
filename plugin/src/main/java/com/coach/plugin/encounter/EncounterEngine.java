package com.coach.plugin.encounter;

import com.coach.plugin.encounter.model.BossDefinition;
import com.coach.plugin.encounter.model.CalloutDefinition;
import com.coach.plugin.encounter.model.EncounterPack;
import com.coach.plugin.encounter.model.MechanicDefinition;
import com.coach.plugin.encounter.model.PhaseDefinition;
import com.coach.plugin.events.EventBus;
import com.coach.plugin.events.EventType;
import com.coach.plugin.events.GameEvent;
import com.coach.plugin.trigger.TriggerFire;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.runelite.api.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds loaded packs AND the runtime state of live encounters:
 * phase machine, mechanic tracking (cooldowns + conditions), recovery.
 *
 * Consumes trigger fires from the TriggerEngine and emits
 * {@link MechanicActivation}s to listeners (the future Coaching Engine).
 */
public class EncounterEngine implements EventBus.Listener
{
	private static final Logger log = LoggerFactory.getLogger(EncounterEngine.class);

	/**
	 * Receives mechanic activations as they fire.
	 */
	public interface ActivationListener
	{
		void onActivation(MechanicActivation activation);
	}

	private final EncounterLoader loader = new EncounterLoader();
	private final PhaseMachine phaseMachine = new PhaseMachine();
	private final MechanicManager mechanicManager = new MechanicManager();
	private final RecoveryHandler recoveryHandler = new RecoveryHandler();
	private final ConditionEvaluator conditionEvaluator;
	private final List<ActivationListener> activationListeners = new ArrayList<>();

	private volatile List<EncounterPack> packs = Collections.emptyList();
	private final Map<Integer, ActiveEncounter> sessions = new HashMap<>(); // npcId -> session

	public EncounterEngine(Client client)
	{
		this.conditionEvaluator = new ConditionEvaluator(client);
	}

	public void addActivationListener(ActivationListener listener)
	{
		activationListeners.add(listener);
	}

	// ---- pack loading ----

	/**
	 * Scan the directory for *.zip packs, validate and load each one.
	 * Invalid packs are logged and skipped, never fatal (rule 4).
	 *
	 * @return number of successfully loaded packs
	 */
	public synchronized int loadPacks(Path directory)
	{
		List<EncounterPack> loaded = new ArrayList<>();

		if (directory == null || !Files.isDirectory(directory))
		{
			log.info("[coach] no encounter pack directory at {} — nothing loaded",
				directory);
			packs = Collections.emptyList();
			return 0;
		}

		try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*.zip"))
		{
			for (Path zip : stream)
			{
				try
				{
					loaded.add(loader.loadZip(zip));
				}
				catch (PackLoadException e)
				{
					log.warn("[coach] rejected encounter pack: {}", e.getMessage());
				}
			}
		}
		catch (IOException e)
		{
			log.warn("[coach] could not scan pack directory {}: {}", directory, e.getMessage());
		}

		packs = Collections.unmodifiableList(loaded);
		int bossCount = packs.stream().mapToInt(p -> p.bosses.size()).sum();
		log.info("[coach] loaded {} encounter pack(s) covering {} boss(es)", packs.size(), bossCount);
		return packs.size();
	}

	public List<EncounterPack> getPacks()
	{
		return packs;
	}

	/**
	 * Find the boss definition matching a live NPC id, if any pack defines it.
	 */
	public Optional<BossDefinition> getBossForNpcId(int npcId)
	{
		for (EncounterPack pack : packs)
		{
			for (BossDefinition boss : pack.bosses)
			{
				if (boss.npcId != null && boss.npcId == npcId)
				{
					return Optional.of(boss);
				}
			}
		}
		return Optional.empty();
	}

	// ---- runtime state ----

	public synchronized Optional<String> getCurrentPhaseId(int npcId)
	{
		ActiveEncounter session = sessions.get(npcId);
		return session != null ? Optional.of(session.getCurrentPhaseId()) : Optional.empty();
	}

	public synchronized boolean hasActiveSession(int npcId)
	{
		return sessions.containsKey(npcId);
	}

	/**
	 * Consume trigger fires: phase entry/exit + mechanic activations.
	 * Called by the TriggerEngine's fire listeners after each tick batch.
	 */
	public void onTriggersFired(List<TriggerFire> fires)
	{
		Set<String> processedBosses = new HashSet<>();
		for (TriggerFire fire : fires)
		{
			if (!processedBosses.add(fire.getBossId()))
			{
				continue; // process each boss once per batch
			}
			BossDefinition boss = getBossForNpcIdByBoss(fire.getBossId());
			if (boss == null || boss.npcId == null)
			{
				continue;
			}
			synchronized (this)
			{
				handleFires(boss, boss.npcId, fire.getTick(), fires);
			}
		}
	}

	private void handleFires(BossDefinition boss, int npcId, int tick, List<TriggerFire> fires)
	{
		ActiveEncounter session = sessions.get(npcId);

		if (session == null)
		{
			Optional<String> entering = phaseMachine.enterPhase(boss, fires);
			if (entering.isPresent())
			{
				session = new ActiveEncounter(boss, npcId, entering.get(), tick);
				sessions.put(npcId, session);
				log.info("[coach] encounter started: {} ({}) entered phase {} at t{}",
					boss.name, boss.bossId, entering.get(), tick);
			}
			return;
		}

		Optional<String> nextPhase = phaseMachine.advanceIfExit(session, fires);
		if (nextPhase.isPresent())
		{
			String from = session.getCurrentPhaseId();
			session.setCurrentPhaseId(nextPhase.get(), tick);
			log.info("[coach] phase transition: {} {} -> {} at t{}",
				boss.bossId, from, nextPhase.get(), tick);
		}

		checkMechanics(session, tick, fires);
	}

	private void checkMechanics(ActiveEncounter session, int tick, List<TriggerFire> fires)
	{
		BossDefinition boss = session.getBoss();
		PhaseDefinition currentPhase = findPhase(boss, session.getCurrentPhaseId());
		if (currentPhase == null)
		{
			return;
		}
		if (currentPhase.mechanics == null)
		{
			return;
		}
		for (MechanicDefinition mechanic : currentPhase.mechanics)
		{
			if (!mechanicManager.wasTriggered(boss.bossId, mechanic, fires))
			{
				continue;
			}
			if (!conditionsSatisfied(mechanic, session))
			{
				log.debug("[coach] mechanic {} conditions unmet — skipped", mechanic.mechanicId);
				continue;
			}
			if (!mechanicManager.tryActivate(session, mechanic, tick))
			{
				log.debug("[coach] mechanic {} on cooldown — skipped", mechanic.mechanicId);
				continue;
			}
			MechanicActivation activation = new MechanicActivation(tick, boss.bossId,
				session.getCurrentPhaseId(), mechanic,
				mechanic.callouts != null ? mechanic.callouts : Collections.emptyList());
			log.info("[coach] MECHANIC ACTIVATION: {}", activation);
			for (ActivationListener listener : activationListeners.toArray(new ActivationListener[0]))
			{
				listener.onActivation(activation);
			}
		}
	}

	private boolean conditionsSatisfied(MechanicDefinition mechanic, ActiveEncounter session)
	{
		if (mechanic.conditions == null)
		{
			return true;
		}
		for (com.coach.plugin.encounter.model.ConditionDefinition condition : mechanic.conditions)
		{
			if (!conditionEvaluator.satisfies(condition, session.getPhaseTick()))
			{
				return false;
			}
		}
		return true;
	}

	// ---- EventBus.Listener: tick counters + recovery ----

	@Override
	public void onTickBatch(int tick, List<GameEvent> events)
	{
		Set<Integer> tracked;
		synchronized (this)
		{
			tracked = new HashSet<>(sessions.keySet());
			for (ActiveEncounter session : sessions.values())
			{
				session.setGlobalTick(tick);
			}
		}

		for (GameEvent event : events)
		{
			if (recoveryHandler.shouldReset(event, tracked))
			{
				resetSessions(event.getType() == EventType.PLAYER_STATS_CHANGED ? "player death" : "boss despawn");
			}
		}
	}

	private synchronized void resetSessions(String reason)
	{
		if (!sessions.isEmpty())
		{
			log.info("[coach] resetting {} encounter session(s): {}", sessions.size(), reason);
			sessions.clear();
		}
	}

	private static PhaseDefinition findPhase(BossDefinition boss, String phaseId)
	{
		for (PhaseDefinition phase : boss.phases)
		{
			if (phase.phaseId.equals(phaseId))
			{
				return phase;
			}
		}
		return null;
	}

	private BossDefinition getBossForNpcIdByBoss(String bossId)
	{
		for (EncounterPack pack : packs)
		{
			for (BossDefinition boss : pack.bosses)
			{
				if (boss.bossId.equals(bossId))
				{
					return boss;
				}
			}
		}
		return null;
	}
}
