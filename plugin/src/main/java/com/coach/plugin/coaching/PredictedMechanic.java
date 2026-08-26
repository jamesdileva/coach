package com.coach.plugin.coaching;

import java.util.List;

/**
 * A mechanic the PredictionEngine expects to fire soon.
 */
public final class PredictedMechanic
{
	private final String bossId;
	private final String mechanicId;
	private final int ticksUntilFire;

	public PredictedMechanic(String bossId, String mechanicId, int ticksUntilFire)
	{
		this.bossId = bossId;
		this.mechanicId = mechanicId;
		this.ticksUntilFire = ticksUntilFire;
	}

	public String getBossId()
	{
		return bossId;
	}

	public String getMechanicId()
	{
		return mechanicId;
	}

	public int getTicksUntilFire()
	{
		return ticksUntilFire;
	}

	@Override
	public String toString()
	{
		return mechanicId + " in " + ticksUntilFire + "t";
	}
}
