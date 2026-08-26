package com.coach.plugin.logging;

import com.coach.plugin.events.EventBus;
import com.coach.plugin.events.EventType;
import com.coach.plugin.events.GameEvent;
import java.util.List;
import net.runelite.api.NPC;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.StatChanged;
import net.runelite.api.Skill;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EventLoggerTest
{
	private LogBuffer sink;
	private EventLogger logger;

	@BeforeEach
	void setUp()
	{
		sink = new LogBuffer();
		logger = new EventLogger(sink);
	}

	@Test
	void formatsTickBatchWithTickNumbersAndTypes()
	{
		NPC npc = mock(NPC.class);
		when(npc.getId()).thenReturn(11278);
		when(npc.getName()).thenReturn("Nex");

		logger.onTickBatch(42, List.of(
			new GameEvent(EventType.NPC_SPAWNED, 42, new NpcSpawned(npc)),
			new GameEvent(EventType.TICK, 42, null)));

		List<String> entries = sink.snapshot();
		assertEquals(2, entries.size());
		assertEquals("t42 NPC_SPAWNED npcId=11278 name=Nex", entries.get(0));
		assertEquals("t42 TICK ", entries.get(1));
	}

	@Test
	void summarizesStatChanges()
	{
		StatChanged stat = new StatChanged(Skill.HITPOINTS, 1154, 99, 92);

		String summary = logger.summarize(new GameEvent(EventType.PLAYER_STATS_CHANGED, 7, stat));

		assertEquals("HITPOINTS=92/99", summary);
	}

	@Test
	void nullPayloadSummarizesToEmptyString()
	{
		GameEvent event = new GameEvent(EventType.TICK, 1, null);

		assertEquals("", logger.summarize(event));
		assertTrue(logger.format(event).startsWith("t1 TICK"));
	}
}
