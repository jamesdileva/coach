package com.coach.plugin.debug;

import com.coach.plugin.trigger.TriggerFire;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Chronological history of trigger fires (roadmap Sprint 21), filterable by
 * boss/context substring. Capped ring buffer; thread-safe.
 */
public class TriggerHistory
{
	public static final int MAX_ENTRIES = 200;

	public static final class Entry
	{
		public final long sequence;
		public final int tick;
		public final String bossId;
		public final String contextId;
		public final String description;

		Entry(long sequence, int tick, String bossId, String contextId, String description)
		{
			this.sequence = sequence;
			this.tick = tick;
			this.bossId = bossId;
			this.contextId = contextId;
			this.description = description;
		}

		public boolean matches(String query)
		{
			return query == null || query.isEmpty()
				|| contextId.toLowerCase().contains(query)
				|| bossId.toLowerCase().contains(query)
				|| description.toLowerCase().contains(query);
		}
	}

	private final Deque<Entry> entries = new ArrayDeque<>();
	private long sequence;

	public synchronized void record(TriggerFire fire)
	{
		if (entries.size() >= MAX_ENTRIES)
		{
			entries.pollFirst();
		}
		entries.addLast(new Entry(++sequence, fire.getTick(), fire.getBossId(),
			fire.getContextId(), fire.getDescription()));
	}

	public synchronized List<Entry> filter(String query, int limit)
	{
		List<Entry> result = new ArrayList<>();
		String q = query != null ? query.toLowerCase() : "";
		var iterator = entries.descendingIterator(); // newest first
		while (iterator.hasNext() && result.size() < limit)
		{
			Entry entry = iterator.next();
			if (entry.matches(q))
			{
				result.add(entry);
			}
		}
		return result;
	}

	public synchronized int size()
	{
		return entries.size();
	}

	public synchronized void clear()
	{
		entries.clear();
	}

	public synchronized List<String> format(String query, int limit)
	{
		List<String> lines = new ArrayList<>();
		for (Entry entry : filter(query, limit))
		{
			lines.add("t" + entry.tick + " [" + entry.bossId + "/" + entry.contextId + "] "
				+ entry.description);
		}
		if (lines.isEmpty())
		{
			lines.add("(no trigger fires" + (query != null && !query.isEmpty() ? " matching '" + query + "'" : "") + ")");
		}
		return lines;
	}
}
