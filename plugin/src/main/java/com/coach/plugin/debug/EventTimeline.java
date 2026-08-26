package com.coach.plugin.debug;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Per-tick visual sequence: event counts per type plus trigger/callout totals
 * for the same tick. Ring buffer of the most recent ticks.
 */
public class EventTimeline
{
	public static final int MAX_TICKS = 120;

	public static final class TickEntry
	{
		public final int tick;
		public final Map<String, Integer> eventCounts;
		public int triggers;
		public int callouts;

		TickEntry(int tick)
		{
			this.tick = tick;
			this.eventCounts = new LinkedHashMap<>();
		}

		int totalEvents()
		{
			int total = 0;
			for (int count : eventCounts.values())
			{
				total += count;
			}
			return total;
		}
	}

	private final Deque<TickEntry> entries = new ArrayDeque<>();

	public synchronized void recordTick(int tick, Map<String, Integer> eventCounts,
		int triggersFired, int calloutsDelivered)
	{
		TickEntry existing = find(tick);
		if (existing == null)
		{
			if (entries.size() >= MAX_TICKS)
			{
				entries.pollFirst();
			}
			existing = new TickEntry(tick);
			entries.addLast(existing);
		}
		if (eventCounts != null)
		{
			existing.eventCounts.putAll(eventCounts);
		}
		existing.triggers += triggersFired;
		existing.callouts += calloutsDelivered;
	}

	private TickEntry find(int tick)
	{
		var iterator = entries.descendingIterator(); // recent ticks likelier
		while (iterator.hasNext())
		{
			TickEntry entry = iterator.next();
			if (entry.tick == tick)
			{
				return entry;
			}
		}
		return null;
	}

	public synchronized List<TickEntry> recent(int limit)
	{
		List<TickEntry> all = new ArrayList<>(entries);
		int from = Math.max(0, all.size() - limit);
		return all.subList(from, all.size());
	}

	public synchronized List<String> format(int limit)
	{
		List<String> lines = new ArrayList<>();
		for (TickEntry entry : recent(limit))
		{
			StringBuilder sb = new StringBuilder("t").append(entry.tick).append(": ")
				.append(entry.totalEvents()).append("ev");
			for (Map.Entry<String, Integer> e : entry.eventCounts.entrySet())
			{
				sb.append(' ').append(e.getKey()).append('=').append(e.getValue());
			}
			sb.append(" | trig=").append(entry.triggers)
				.append(" call=").append(entry.callouts);
			lines.add(sb.toString());
		}
		if (lines.isEmpty())
		{
			lines.add("(timeline empty)");
		}
		return lines;
	}

	public synchronized void clear()
	{
		entries.clear();
	}
}

