package com.coach.plugin.trigger;

import com.coach.plugin.encounter.model.BossDefinition;
import com.coach.plugin.encounter.model.EncounterPack;
import com.coach.plugin.encounter.model.MechanicDefinition;
import com.coach.plugin.encounter.model.PhaseDefinition;
import com.coach.plugin.encounter.model.TriggerDefinition;
import com.coach.plugin.events.EventBus;
import com.coach.plugin.events.EventType;
import com.coach.plugin.events.GameEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Coordinates trigger evaluation per tick batch.
 *
 * Rebuilt whenever encounter packs change; evaluates only evaluators
 * interested in each event's type, and produces at most one fire per
 * evaluator per event (§8.4: triggers are stateless, fire-once-per-event).
 */
public class TriggerEngine implements EventBus.Listener
{
	private static final Logger log = LoggerFactory.getLogger(TriggerEngine.class);

	/**
	 * Receives trigger fires after each tick batch is evaluated.
	 */
	public interface FireListener
	{
		void onTriggersFired(List<TriggerFire> fires);
	}

	private static final class BoundTrigger
	{
		final String bossId;
		final String contextId;
		final TriggerEvaluator evaluator;

		BoundTrigger(String bossId, String contextId, TriggerEvaluator evaluator)
		{
			this.bossId = bossId;
			this.contextId = contextId;
			this.evaluator = evaluator;
		}
	}

	private final TriggerRegistry registry = new TriggerRegistry();
	private final List<BoundTrigger> triggers = new ArrayList<>();
	private final List<FireListener> fireListeners = new ArrayList<>();
	private volatile List<TriggerFire> lastFires = Collections.emptyList();

	public void addFireListener(FireListener listener)
	{
		fireListeners.add(listener);
	}

	public void removeFireListener(FireListener listener)
	{
		fireListeners.remove(listener);
	}

	/**
	 * Rebuild the trigger list from loaded packs (called on pack load/reload).
	 */
	public synchronized void rebuild(List<EncounterPack> packs)
	{
		triggers.clear();
		int skipped = 0;
		for (EncounterPack pack : packs)
		{
			for (BossDefinition boss : pack.bosses)
			{
				for (PhaseDefinition phase : boss.phases)
				{
					skipped += add(boss.bossId, "phase:" + phase.phaseId + ":entry", phase.entryTrigger);
					if (phase.exitTriggers != null)
					{
						int i = 0;
						for (TriggerDefinition exit : phase.exitTriggers)
						{
							skipped += add(boss.bossId, "phase:" + phase.phaseId + ":exit" + i++, exit);
						}
					}
					if (phase.mechanics != null)
					{
						for (MechanicDefinition mechanic : phase.mechanics)
						{
							addMechanic(boss.bossId, mechanic);
						}
					}
				}
				// shared boss-level mechanics
				if (boss.mechanics != null)
				{
					for (MechanicDefinition mechanic : boss.mechanics)
					{
						addMechanic(boss.bossId, mechanic);
					}
				}
			}
		}
		log.info("[coach] trigger engine rebuilt: {} active trigger evaluator(s)", triggers.size());
		if (skipped > 0)
		{
			log.warn("[coach] {} trigger(s) skipped (unsupported or misconfigured types)", skipped);
		}
	}

	private void addMechanic(String bossId, MechanicDefinition mechanic)
	{
		if (mechanic.triggers == null)
		{
			return;
		}
		int i = 0;
		for (TriggerDefinition trigger : mechanic.triggers)
		{
			String context = mechanic.mechanicId + (mechanic.triggers.size() > 1 ? "#" + i++ : "");
			add(bossId, context, trigger);
		}
	}

	private int add(String bossId, String contextId, TriggerDefinition definition)
	{
		if (definition == null)
		{
			return 0;
		}
		return registry.create(definition)
			.map(evaluator -> {
				triggers.add(new BoundTrigger(bossId, contextId, evaluator));
				return 1;
			})
			.orElse(0);
	}

	@Override
	public void onTickBatch(int tick, List<GameEvent> events)
	{
		List<TriggerFire> fires = new ArrayList<>();
		synchronized (this)
		{
			for (GameEvent event : events)
			{
				for (BoundTrigger bound : triggers)
				{
					if (!bound.evaluator.interestedIn().contains(event.getType()))
					{
						continue;
					}
					if (bound.evaluator.matches(event))
					{
						fires.add(new TriggerFire(tick, bound.bossId, bound.contextId,
							bound.evaluator.describe()));
					}
				}
			}
		}
		lastFires = Collections.unmodifiableList(fires);
		if (!fires.isEmpty())
		{
			for (FireListener listener : fireListeners.toArray(new FireListener[0]))
			{
				listener.onTriggersFired(lastFires);
			}
		}
	}

	/**
	 * Fires produced by the most recent tick batch (for tests/debug).
	 */
	public List<TriggerFire> getLastFires()
	{
		return lastFires;
	}
}
