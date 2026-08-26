package com.coach.plugin.trigger;

import com.coach.plugin.events.EventType;
import com.coach.plugin.events.GameEvent;
import java.util.Set;
import net.runelite.api.NPC;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;

/**
 * Fires when an NPC (optionally a specific id) spawns or despawns.
 * Used for boss spawn detection and phase-entry triggers.
 */
public class NpcSpawnTriggerEvaluator implements TriggerEvaluator
{
	private final Integer npcId; // null = any NPC
	private final boolean spawn; // true = NpcSpawned, false = NpcDespawned

	public NpcSpawnTriggerEvaluator(Integer npcId, boolean spawn)
	{
		this.npcId = npcId;
		this.spawn = spawn;
	}

	@Override
	public Set<EventType> interestedIn()
	{
		return Set.of(spawn ? EventType.NPC_SPAWNED : EventType.NPC_DESPAWNED);
	}

	@Override
	public boolean matches(GameEvent event)
	{
		Object payload = event.getPayload();
		NPC npc = null;
		if (spawn && payload instanceof NpcSpawned)
		{
			npc = ((NpcSpawned) payload).getNpc();
		}
		else if (!spawn && payload instanceof NpcDespawned)
		{
			npc = ((NpcDespawned) payload).getNpc();
		}
		if (npc == null)
		{
			return false;
		}
		return npcId == null || npc.getId() == npcId;
	}

	@Override
	public String describe()
	{
		return (spawn ? "npc_spawn" : "npc_despawn") + (npcId != null ? " " + npcId : " any");
	}
}
