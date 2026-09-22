package com.coach.plugin.performance;

import java.util.EnumMap;
import java.util.Map;

/**
 * Per-tick component timing with budgets from master-architecture §12.
 * Single-threaded by design (client-thread only); callers start/stop scopes
 * around known pipeline stages. An un-stopped scope is dropped at endTick.
 */
public class Profiler
{
	public enum Component
	{
		EVENTS(200),
		TRIGGERS(100),
		ENCOUNTER(100),
		COACHING(100),
		OVERLAY(100),
		AUDIO(50);

		public final long budgetNanos;

		Component(long budgetMillis)
		{
			this.budgetNanos = budgetMillis * 1_000_000L;
		}
	}

	public static final long TOTAL_BUDGET_MS = 600;

	/** Snapshot of one finished tick's component timings. */
	public static final class TickBreakdown
	{
		private final int tick;
		private final long[] nanos = new long[Component.values().length];
		private long totalNanos;

		TickBreakdown(int tick)
		{
			this.tick = tick;
		}

		public int getTick()
		{
			return tick;
		}

		public long getNanos(Component component)
		{
			return nanos[component.ordinal()];
		}

		public long getMillis(Component component)
		{
			return nanos[component.ordinal()] / 1_000_000L;
		}

		public long getTotalNanos()
		{
			return totalNanos;
		}

		public long getTotalMillis()
		{
			return totalNanos / 1_000_000L;
		}

		public boolean isOverBudget(Component component)
		{
			return nanos[component.ordinal()] > component.budgetNanos;
		}

		public boolean isTotalOverBudget()
		{
			return totalNanos > TOTAL_BUDGET_MS * 1_000_000L;
		}
	}

	private int currentTick = -1;
	private long tickStartNanos;
	private final Map<Component, Long> openStarts = new EnumMap<>(Component.class);
	private final long[] currentNanos = new long[Component.values().length];
	private TickBreakdown last;

	public void beginTick(int tick)
	{
		currentTick = tick;
		tickStartNanos = System.nanoTime();
		for (int i = 0; i < currentNanos.length; i++)
		{
			currentNanos[i] = 0L;
		}
		openStarts.clear();
	}

	/** Close the open tick; returns the finished breakdown (or the prior one). */
	public TickBreakdown endTick()
	{
		closeOpenScopes();
		TickBreakdown breakdown = new TickBreakdown(currentTick);
		for (Component component : Component.values())
		{
			breakdown.nanos[component.ordinal()] = currentNanos[component.ordinal()];
			breakdown.totalNanos += currentNanos[component.ordinal()];
		}
		last = breakdown;
		return breakdown;
	}

	/** Start timing a component; no-op if already open or profiler idle. */
	public void start(Component component)
	{
		if (currentTick < 0 || openStarts.containsKey(component))
		{
			return;
		}
		openStarts.put(component, System.nanoTime());
	}

	/** Stop a component; no-op if it was not open. */
	public void stop(Component component)
	{
		Long started = openStarts.remove(component);
		if (started == null)
		{
			return;
		}
		currentNanos[component.ordinal()] += System.nanoTime() - started;
	}

	/** Direct sample injection for tests and the replay harness. */
	public void addSampleNanos(Component component, long nanos)
	{
		currentNanos[component.ordinal()] += Math.max(0L, nanos);
	}

	public TickBreakdown getLast()
	{
		return last;
	}

	public int getCurrentTick()
	{
		return currentTick;
	}

	/** Multi-line summary of the last finished tick (debug overlay). */
	public java.util.List<String> formatLines()
	{
		java.util.List<String> lines = new java.util.ArrayList<>();
		if (last == null)
		{
			lines.add("(no tick profiled yet)");
			return lines;
		}
		lines.add(String.format("t%d total %dms / %dms budget",
			last.getTick(), last.getTotalMillis(), TOTAL_BUDGET_MS));
		for (Component component : Component.values())
		{
			long budgetMs = component.budgetNanos / 1_000_000L;
			lines.add(String.format("  %-9s %3dms / %3dms%s",
				component.name(), last.getMillis(component), budgetMs,
				last.isOverBudget(component) ? " OVER" : ""));
		}
		return lines;
	}

	private void closeOpenScopes()
	{
		if (openStarts.isEmpty())
		{
			return;
		}
		long now = System.nanoTime();
		for (Map.Entry<Component, Long> entry : openStarts.entrySet())
		{
			currentNanos[entry.getKey().ordinal()] += now - entry.getValue();
		}
		openStarts.clear();
	}
}
