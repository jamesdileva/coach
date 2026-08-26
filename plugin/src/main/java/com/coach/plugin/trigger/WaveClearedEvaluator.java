package com.coach.plugin.trigger;

import com.coach.plugin.events.EventType;
import com.coach.plugin.events.GameEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.runelite.api.NPC;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;

/**
 * Fires when every NPC of a tracked set has spawned and subsequently died —
 * the "wave cleared" signal for sequential wave encounters (e.g. Inferno).
 *
 * Stateful by necessity: tracks the alive subset of configured ids.
 * Re-arms automatically after firing so the same evaluator definition can
 * cover repeated compositions (e.g. triple Jad waves).
 */
public class WaveClearedEvaluator implements TriggerEvaluator
{
	private final Set<Integer> trackedIds;
	private final Set<Integer> alive = new HashSet<>();
	private boolean anySpawned;

	public WaveClearedEvaluator(List<Integer> npcIds)
	{
		this.trackedIds = new HashSet<>(npcIds);
	}

	@Override
	public Set<EventType> interestedIn()
	{
		return Set.of(EventType.NPC_SPAWNED, EventType.NPC_DESPAWNED);
	}

	@Override
	public synchronized boolean matches(GameEvent event)
	{
		Object payload = event.getPayload();
		Integer npcId = null;
		boolean spawning = false;

		if (event.getType() == EventType.NPC_SPAWNED && payload instanceof NpcSpawned)
		{
			NPC npc = ((NpcSpawned) payload).getNpc();
			npcId = npc != null ? npc.getId() : null;
			spawning = true;
		}
		else if (event.getType() == EventType.NPC_DESPAWNED && payload instanceof NpcDespawned)
		{
			NPC npc = ((NpcDespawned) payload).getNpc();
			npcId = npc != null ? npc.getId() : null;
		}

		if (npcId == null || !trackedIds.contains(npcId))
		{
			return false;
		}

		if (spawning)
		{
			alive.add(npcId);
			anySpawned = true;
			return false;
		}

		if (!alive.remove(npcId))
		{
			return false; // despawn of something we never saw spawn
		}
		if (anySpawned && alive.isEmpty())
		{
			anySpawned = false; // re-arm for the next repetition of this wave
			return true;
		}
		return false;
	}

	@Override
	public String describe()
	{
		return "wave cleared (" + trackedIds.size() + " npc types)";
	}
}
