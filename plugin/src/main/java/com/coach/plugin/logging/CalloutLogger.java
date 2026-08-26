package com.coach.plugin.logging;

/**
 * Logs callout decisions (what was called out, at what tick). Callouts start
 * existing in Sprint 8+; the API is defined now so the debug overlay and log
 * format stay stable.
 */
public class CalloutLogger
{
	private final LogBuffer sink;

	public CalloutLogger(LogBuffer sink)
	{
		this.sink = sink;
	}

	public void calloutScheduled(int tick, String calloutId, int visualTick, int audioTick)
	{
		sink.log("t" + tick + " CALLOUT_SCHEDULED id=" + calloutId + " visual=t" + visualTick + " audio=t" + audioTick);
	}

	public void calloutDelivered(int tick, String calloutId)
	{
		sink.log("t" + tick + " CALLOUT_DELIVERED id=" + calloutId);
	}
}
