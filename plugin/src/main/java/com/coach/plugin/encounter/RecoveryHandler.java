package com.coach.plugin.encounter;

import com.coach.plugin.events.EventType;
import com.coach.plugin.events.GameEvent;
import java.util.Set;
import net.runelite.api.NPC;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.StatChanged;
import net.runelite.api.Skill;

/**
 * Decides when an encounter session must reset: boss despawn or player death.
 */
public class RecoveryHandler
{
	/**
	 * @param trackedNpcIds npc ids of bosses with active sessions
	 * @return true if this event requires resetting sessions
	 */
	public boolean shouldReset(GameEvent event, Set<Integer> trackedNpcIds)
	{
		Object payload = event.getPayload();

		if (event.getType() == EventType.NPC_DESPAWNED && payload instanceof NpcDespawned)
		{
			NPC npc = ((NpcDespawned) payload).getNpc();
			return npc != null && trackedNpcIds.contains(npc.getId());
		}

		if (event.getType() == EventType.PLAYER_STATS_CHANGED && payload instanceof StatChanged)
		{
			StatChanged stat = (StatChanged) payload;
			return stat.getSkill() == Skill.HITPOINTS && stat.getBoostedLevel() <= 0;
		}

		return false;
	}
}
