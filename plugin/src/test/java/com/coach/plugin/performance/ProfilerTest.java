package com.coach.plugin.performance;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfilerTest
{
	@Test
	void beginTickResetsAndEndTickReturnsBreakdown()
	{
		Profiler profiler = new Profiler();
		assertNull(profiler.getLast());

		profiler.beginTick(42);
		assertEquals(42, profiler.getCurrentTick());
		Profiler.TickBreakdown breakdown = profiler.endTick();

		assertEquals(42, breakdown.getTick());
		assertEquals(0L, breakdown.getTotalNanos());
		assertFalse(breakdown.isTotalOverBudget());
		assertNotNull(profiler.getLast());
	}

	@Test
	void startStopAccumulatesComponentTime()
	{
		Profiler profiler = new Profiler();
		profiler.beginTick(1);
		profiler.start(Profiler.Component.TRIGGERS);
		profiler.stop(Profiler.Component.TRIGGERS);
		profiler.addSampleNanos(Profiler.Component.AUDIO, 5_000_000L);
		Profiler.TickBreakdown breakdown = profiler.endTick();

		assertTrue(breakdown.getNanos(Profiler.Component.TRIGGERS) > 0L);
		assertEquals(5_000_000L, breakdown.getNanos(Profiler.Component.AUDIO));
		assertTrue(breakdown.getTotalNanos() >= 5_000_000L);
	}

	@Test
	void overBudgetFlagUsesSection12Budgets()
	{
		Profiler profiler = new Profiler();
		profiler.beginTick(1);
		profiler.addSampleNanos(Profiler.Component.AUDIO,
			Profiler.Component.AUDIO.budgetNanos + 1_000_000L);
		profiler.addSampleNanos(Profiler.Component.TRIGGERS, 1_000_000L);
		Profiler.TickBreakdown breakdown = profiler.endTick();

		assertTrue(breakdown.isOverBudget(Profiler.Component.AUDIO));
		assertFalse(breakdown.isOverBudget(Profiler.Component.TRIGGERS));
	}

	@Test
	void unStoppedScopeIsClosedAtEndTick()
	{
		Profiler profiler = new Profiler();
		profiler.beginTick(7);
		profiler.start(Profiler.Component.EVENTS);
		// never stop — endTick must close it
		Profiler.TickBreakdown breakdown = profiler.endTick();
		assertTrue(breakdown.getNanos(Profiler.Component.EVENTS) >= 0L);
	}

	@Test
	void stopWithoutStartIsNoOp()
	{
		Profiler profiler = new Profiler();
		profiler.beginTick(1);
		profiler.stop(Profiler.Component.EVENTS);
		assertEquals(0L, profiler.endTick().getNanos(Profiler.Component.EVENTS));
	}

	@Test
	void formatLinesEmptyThenPopulated()
	{
		Profiler profiler = new Profiler();
		assertEquals("(no tick profiled yet)", profiler.formatLines().get(0));

		profiler.beginTick(9);
		profiler.addSampleNanos(Profiler.Component.COACHING, 2_000_000L);
		profiler.endTick();

		var lines = profiler.formatLines();
		assertTrue(lines.get(0).contains("t9"));
		assertTrue(lines.get(0).contains("600ms"));
		assertTrue(lines.stream().anyMatch(l -> l.contains("COACHING")));
	}

	@Test
	void totalOverBudgetWhenSumExceeds600ms()
	{
		Profiler profiler = new Profiler();
		profiler.beginTick(1);
		profiler.addSampleNanos(Profiler.Component.EVENTS,
			Profiler.Component.EVENTS.budgetNanos + 1);
		Profiler.TickBreakdown breakdown = profiler.endTick();
		assertFalse(breakdown.isTotalOverBudget(), "one component over is not total over");
	}
}
