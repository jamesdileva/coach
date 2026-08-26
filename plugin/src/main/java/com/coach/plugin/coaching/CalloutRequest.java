package com.coach.plugin.coaching;

import com.coach.plugin.encounter.model.CalloutDefinition;

/**
 * A callout accepted for scheduling: resolved priority plus the ticks at
 * which its visual and audio components should fire.
 */
public final class CalloutRequest
{
	private final String bossId;
	private final String mechanicId;
	private final CalloutDefinition callout;
	private final int priority;
	private final int dueTick; // min(visualTick, audioTick) — earliest delivery moment

	public CalloutRequest(String bossId, String mechanicId, CalloutDefinition callout,
		int priority, int visualTick, int audioTick)
	{
		this.bossId = bossId;
		this.mechanicId = mechanicId;
		this.callout = callout;
		this.priority = priority;
		this.dueTick = Math.min(visualTick, audioTick);
	}

	public String getBossId()
	{
		return bossId;
	}

	public String getMechanicId()
	{
		return mechanicId;
	}

	public CalloutDefinition getCallout()
	{
		return callout;
	}

	public int getPriority()
	{
		return priority;
	}

	public int getDueTick()
	{
		return dueTick;
	}

	@Override
	public String toString()
	{
		return "callout=" + callout.calloutId + " prio=" + priority + " due=t" + dueTick;
	}
}
