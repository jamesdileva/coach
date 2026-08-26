package com.coach.plugin.coaching;

import java.util.ArrayList;
import java.util.List;

/**
 * Time-ordered queue of pending callouts.
 * Ordering: earlier dueTick first; same tick -> higher priority first (§8.2).
 */
public class CalloutQueue
{
	private final List<CalloutRequest> pending = new ArrayList<>();

	public void enqueue(CalloutRequest request)
	{
		// insert sorted: dueTick asc, then priority desc
		int index = 0;
		for (CalloutRequest existing : pending)
		{
			if (existing.getDueTick() > request.getDueTick()
				|| existing.getDueTick() == request.getDueTick()
					&& existing.getPriority() < request.getPriority())
			{
				break;
			}
			index++;
		}
		pending.add(index, request);
	}

	/**
	 * Remove and return all requests whose due tick has arrived,
	 * preserving queue order (tick asc, priority desc).
	 */
	public List<CalloutRequest> drainDue(int tick)
	{
		List<CalloutRequest> due = new ArrayList<>();
		while (!pending.isEmpty() && pending.get(0).getDueTick() <= tick)
		{
			due.add(pending.remove(0));
		}
		return due;
	}

	public int size()
	{
		return pending.size();
	}

	public void clear()
	{
		pending.clear();
	}
}
