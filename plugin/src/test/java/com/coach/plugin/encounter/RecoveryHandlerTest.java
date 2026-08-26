package com.coach.plugin.encounter;

import com.coach.plugin.events.EventType;
import com.coach.plugin.events.GameEvent;
import net.runelite.api.NPC;
import net.runelite.api.Skill;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.StatChanged;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RecoveryHandlerTest
{
	private final RecoveryHandler handler = new RecoveryHandler();

	@Test
	void bossDespawnTriggersResetForTrackedNpcOnly()
	{
		NPC nex = mock(NPC.class);
		when(nex.getId()).thenReturn(11278);

		GameEvent despawn = new GameEvent(EventType.NPC_DESPAWNED, 1, new NpcDespawned(nex));

		assertTrue(handler.shouldReset(despawn, java.util.Set.of(11278)));
		assertFalse(handler.shouldReset(despawn, java.util.Set.of(9999)));
	}

	@Test
	void playerDeathTriggersReset()
	{
		GameEvent death = new GameEvent(EventType.PLAYER_STATS_CHANGED, 1,
			new StatChanged(Skill.HITPOINTS, 0, 0, 0));

		assertTrue(handler.shouldReset(death, java.util.Set.of()));
	}

	@Test
	void normalEventsNeverReset()
	{
		GameEvent alive = new GameEvent(EventType.PLAYER_STATS_CHANGED, 1,
			new StatChanged(Skill.HITPOINTS, 55, 99, 99));

		assertFalse(handler.shouldReset(alive, java.util.Set.of()));
		assertFalse(handler.shouldReset(new GameEvent(EventType.TICK, 1, null), java.util.Set.of()));
	}
}
