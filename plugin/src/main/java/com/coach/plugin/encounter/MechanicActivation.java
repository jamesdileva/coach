package com.coach.plugin.encounter;

import com.coach.plugin.encounter.model.CalloutDefinition;
import com.coach.plugin.encounter.model.MechanicDefinition;
import java.util.List;

/**
 * One fired mechanic, ready for consumption by the Coaching Engine (Sprint 8).
 */
public final class MechanicActivation
{
	private final int tick;
	private final String bossId;
	private final String phaseId;
	private final MechanicDefinition mechanic;
	private final List<CalloutDefinition> callouts;

	public MechanicActivation(int tick, String bossId, String phaseId,
		MechanicDefinition mechanic, List<CalloutDefinition> callouts)
	{
		this.tick = tick;
		this.bossId = bossId;
		this.phaseId = phaseId;
		this.mechanic = mechanic;
		this.callouts = callouts;
	}

	public int getTick()
	{
		return tick;
	}

	public String getBossId()
	{
		return bossId;
	}

	public String getPhaseId()
	{
		return phaseId;
	}

	public MechanicDefinition getMechanic()
	{
		return mechanic;
	}

	public List<CalloutDefinition> getCallouts()
	{
		return callouts;
	}

	@Override
	public String toString()
	{
		return "boss=" + bossId + " phase=" + phaseId + " mechanic=" + mechanic.mechanicId + " t" + tick;
	}
}
