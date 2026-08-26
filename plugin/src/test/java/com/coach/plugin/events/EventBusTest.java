package com.coach.plugin.events;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventBusTest
{
	private EventBus bus;
	private List<List<GameEvent>> batches;
	private List<Integer> batchTicks;

	@BeforeEach
	void setUp()
	{
		bus = new EventBus();
		batches = new java.util.ArrayList<>();
		batchTicks = new java.util.ArrayList<>();
		bus.subscribe((tick, events) -> {
			batchTicks.add(tick);
			batches.add(events);
		});
	}

	@Test
	void eventsPostedBeforeTickArriveInSameBatch()
	{
		bus.post(new GameEvent(EventType.ANIMATION_CHANGED, 10, "a"));
		bus.post(new GameEvent(EventType.PROJECTILE_MOVED, 10, "b"));
		bus.post(new GameEvent(EventType.TICK, 10, null));

		assertEquals(1, batches.size());
		assertEquals(3, batches.get(0).size());
		assertEquals(EventType.ANIMATION_CHANGED, batches.get(0).get(0).getType());
		assertEquals(EventType.PROJECTILE_MOVED, batches.get(0).get(1).getType());
		assertEquals(EventType.TICK, batches.get(0).get(2).getType());
		assertEquals(Integer.valueOf(10), batchTicks.get(0));
	}

	@Test
	void tickWithNoBufferedEventsYieldsSingleEventBatch()
	{
		bus.post(new GameEvent(EventType.TICK, 1, null));

		assertEquals(1, batches.size());
		assertEquals(1, batches.get(0).size());
		assertTrue(bus.pendingCount() == 0);
	}

	@Test
	void consecutiveTicksStayInSeparateBatches()
	{
		bus.post(new GameEvent(EventType.NPC_SPAWNED, 1, "npc"));
		bus.post(new GameEvent(EventType.TICK, 1, null));
		bus.post(new GameEvent(EventType.NPC_DESPAWNED, 2, "npc"));
		bus.post(new GameEvent(EventType.TICK, 2, null));

		assertEquals(2, batches.size());
		assertEquals(2, batches.get(0).size());
		assertEquals(2, batches.get(1).size());
		assertEquals(EventType.NPC_SPAWNED, batches.get(0).get(0).getType());
		assertEquals(EventType.NPC_DESPAWNED, batches.get(1).get(0).getType());
	}

	@Test
	void multipleListenersAreAllNotified()
	{
		int[] calls = {0};
		bus.subscribe((tick, events) -> calls[0]++);

		bus.post(new GameEvent(EventType.TICK, 5, null));

		assertEquals(1, batches.size());
		assertEquals(1, calls[0]);
	}

	@Test
	void bufferIsEmptyAfterFlush()
	{
		bus.post(new GameEvent(EventType.GRAPHIC_CHANGED, 3, "g"));
		assertEquals(1, bus.pendingCount());

		bus.post(new GameEvent(EventType.TICK, 3, null));

		assertEquals(0, bus.pendingCount());
	}
}
