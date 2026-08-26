package com.coach.plugin.coaching;

import com.coach.plugin.encounter.model.CalloutDefinition;

/**
 * Schedules accepted callouts into the queue at tick-precise offsets.
 * dueTick = activation tick + min(visualOffset, audioOffset).
 */
public class CalloutScheduler
{
	private final CalloutQueue queue;
	private final PriorityResolver priorityResolver;

	public CalloutScheduler(CalloutQueue queue, PriorityResolver priorityResolver)
	{
		this.queue = queue;
		this.priorityResolver = priorityResolver;
	}

	/**
	 * @param activationTick the tick the mechanic triggered
	 */
	public void schedule(String bossId, String mechanicId, CalloutDefinition callout, int activationTick)
	{
		int visualOffset = callout.visualOffset != null ? callout.visualOffset : 0;
		int audioOffset = callout.audioOffset != null ? callout.audioOffset : 0;
		int priority = priorityResolver.resolve(callout);

		queue.enqueue(new CalloutRequest(bossId, mechanicId, callout, priority,
			activationTick + visualOffset, activationTick + audioOffset));
	}
}
