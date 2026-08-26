package com.coach.plugin.trigger;

import com.coach.plugin.events.EventType;
import com.coach.plugin.events.GameEvent;
import net.runelite.api.NPC;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WaveClearedEvaluatorTest
{
	private WaveClearedEvaluator evaluator;

	private GameEvent spawn(int npcId)
	{
		return npcEvent(EventType.NPC_SPAWNED, npcId);
	}

	private GameEvent despawn(int npcId)
	{
		return npcEvent(EventType.NPC_DESPAWNED, npcId);
	}

	private GameEvent npcEvent(EventType type, int npcId)
	{
		NPC npc = mock(NPC.class);
		when(npc.getId()).thenReturn(npcId);
		Object payload = type == EventType.NPC_SPAWNED
			? new NpcSpawned(npc) : new NpcDespawned(npc);
		return new GameEvent(type, 1, payload);
	}

	@Test
	void firesOnlyWhenWholeSetSpawnedAndDied()
	{
		evaluator = new WaveClearedEvaluator(java.util.List.of(7690, 7691));

		assertFalse(evaluator.matches(spawn(7690)));
		assertFalse(evaluator.matches(spawn(7691)));
		assertFalse(evaluator.matches(despawn(7690)), "one still alive");
		assertTrue(evaluator.matches(despawn(7691)), "last death clears the wave");
	}

	@Test
	void untrackedNpcsIgnored()
	{
		evaluator = new WaveClearedEvaluator(java.util.List.of(7700));

		assertFalse(evaluator.matches(spawn(11278)));
		assertFalse(evaluator.matches(despawn(11278)));
		assertFalse(evaluator.matches(despawn(7700)), "despawn without spawn doesn't fire");
	}

	@Test
	void reArmsAfterFiring()
	{
		evaluator = new WaveClearedEvaluator(java.util.List.of(7695));

		evaluator.matches(spawn(7695));
		assertTrue(evaluator.matches(despawn(7695)));

		evaluator.matches(spawn(7695)); // e.g. mager revive or next repetition
		assertFalse(evaluator.matches(despawn(7696)));
		assertTrue(evaluator.matches(despawn(7695)), "re-armed for next round");
	}

	@Test
	void partialDeathThenMoreSpawnsStillRequiresFullClear()
	{
		evaluator = new WaveClearedEvaluator(java.util.List.of(7690, 7691));

		evaluator.matches(spawn(7690));
		evaluator.matches(spawn(7691));
		evaluator.matches(despawn(7690));
		assertFalse(evaluator.matches(spawn(7690))); // revived mid-wave
		assertFalse(evaluator.matches(despawn(7690)), "revive dead again — other mob still alive");
		assertTrue(evaluator.matches(despawn(7691)));
	}
}
