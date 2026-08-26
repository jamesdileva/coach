package com.coach.plugin.audio;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AudioInterruptManagerTest
{
	private AudioInterruptManager manager;
	private List<String> started;

	@BeforeEach
	void setUp()
	{
		manager = new AudioInterruptManager();
		started = new ArrayList<>();
	}

	private boolean submit(String label, AudioCategory category)
	{
		return manager.submit(category, () -> started.add(label));
	}

	@Test
	void idleRequestStartsImmediately()
	{
		assertTrue(submit("critical1", AudioCategory.CRITICAL));
		assertEquals(List.of("critical1"), started);
		assertEquals(0, manager.queuedCount());
	}

	@Test
	void sameCategoryQueuesBehindCurrent()
	{
		submit("crit1", AudioCategory.CRITICAL);
		assertFalse(submit("crit2", AudioCategory.CRITICAL), "same priority queues");
		assertEquals(1, manager.queuedCount());
		assertEquals(List.of("crit1"), started, "current not interrupted");
	}

	@Test
	void lowerPriorityQueues()
	{
		submit("warn", AudioCategory.WARNING);
		assertFalse(submit("info", AudioCategory.INFO));
		assertTrue(manager.queuedCount() == 1);
	}

	@Test
	void higherPriorityInterrupts()
	{
		submit("info", AudioCategory.INFO);
		assertTrue(submit("critical", AudioCategory.CRITICAL), "interrupts");
		assertEquals(List.of("info", "critical"), started);
	}

	@Test
	void finishDrainsHighestPriorityThenFifo()
	{
		submit("crit-playing", AudioCategory.CRITICAL);
		submit("info-a", AudioCategory.INFO);
		submit("warn-b", AudioCategory.WARNING);
		submit("info-c", AudioCategory.INFO);

		manager.onPlaybackFinished(); // critical done

		assertEquals(List.of("crit-playing", "warn-b"), started,
			"warning outranks queued infos");
		manager.onPlaybackFinished();
		assertEquals(List.of("crit-playing", "warn-b", "info-a"), started,
			"same category drains FIFO");
		manager.onPlaybackFinished();
		assertEquals(List.of("crit-playing", "warn-b", "info-a", "info-c"), started);
		assertEquals(0, manager.queuedCount());
	}

	@Test
	void resetClearsQueue()
	{
		submit("playing", AudioCategory.CRITICAL);
		submit("queued", AudioCategory.INFO);
		manager.reset();

		assertEquals(0, manager.queuedCount());
		manager.onPlaybackFinished();
		assertEquals(List.of("playing"), started, "reset drops queued requests");
	}
}
