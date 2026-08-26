package com.coach.plugin.trigger;

/**
 * One trigger match, ready for consumption by the Encounter Engine (Sprint 7).
 */
public final class TriggerFire
{
	private final int tick;
	private final String bossId;
	private final String contextId;   // mechanicId or "phase:<phaseId>:entry"/":exit"
	private final String description; // evaluator description

	public TriggerFire(int tick, String bossId, String contextId, String description)
	{
		this.tick = tick;
		this.bossId = bossId;
		this.contextId = contextId;
		this.description = description;
	}

	public int getTick()
	{
		return tick;
	}

	public String getBossId()
	{
		return bossId;
	}

	public String getContextId()
	{
		return contextId;
	}

	public String getDescription()
	{
		return description;
	}

	@Override
	public String toString()
	{
		return "boss=" + bossId + " ctx=" + contextId + " " + description;
	}
}
