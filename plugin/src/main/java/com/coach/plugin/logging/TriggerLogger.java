package com.coach.plugin.logging;

/**
 * Logs trigger evaluations. Trigger fires start existing in Sprint 5+;
 * the API is defined now so the debug overlay and log format stay stable.
 */
public class TriggerLogger
{
	private final LogBuffer sink;

	public TriggerLogger(LogBuffer sink)
	{
		this.sink = sink;
	}

	public void triggerFired(int tick, String triggerId, String mechanicId)
	{
		sink.log("t" + tick + " TRIGGER_FIRED trigger=" + triggerId + " mechanic=" + mechanicId);
	}

	public void triggerEvaluated(int tick, String triggerId, boolean matched)
	{
		if (matched)
		{
			sink.log("t" + tick + " TRIGGER_MATCH trigger=" + triggerId);
		}
	}
}
