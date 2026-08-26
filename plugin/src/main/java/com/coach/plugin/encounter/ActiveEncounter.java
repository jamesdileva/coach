package com.coach.plugin.encounter;

import com.coach.plugin.encounter.model.BossDefinition;
import java.util.HashMap;
import java.util.Map;

/**
 * Runtime session for one live boss fight: current phase, tick counters,
 * and per-mechanic cooldowns. Created on phase entry, dropped on recovery.
 */
public class ActiveEncounter
{
	private final BossDefinition boss;
	private final int npcId;
	private String currentPhaseId;
	private int phaseStartTick;
	private int globalTick;
	private final Map<String, Integer> mechanicCooldownUntil = new HashMap<>();

	public ActiveEncounter(BossDefinition boss, int npcId, String phaseId, int tick)
	{
		this.boss = boss;
		this.npcId = npcId;
		this.currentPhaseId = phaseId;
		this.phaseStartTick = tick;
		this.globalTick = tick;
	}

	public BossDefinition getBoss()
	{
		return boss;
	}

	public int getNpcId()
	{
		return npcId;
	}

	public String getCurrentPhaseId()
	{
		return currentPhaseId;
	}

	public void setCurrentPhaseId(String phaseId, int tick)
	{
		this.currentPhaseId = phaseId;
		this.phaseStartTick = tick;
	}

	public int getPhaseStartTick()
	{
		return phaseStartTick;
	}

	public int getGlobalTick()
	{
		return globalTick;
	}

	public void setGlobalTick(int tick)
	{
		this.globalTick = tick;
	}

	public int getPhaseTick()
	{
		return globalTick - phaseStartTick;
	}

	public boolean isOnCooldown(String mechanicId, int tick)
	{
		Integer until = mechanicCooldownUntil.get(mechanicId);
		return until != null && tick < until;
	}

	public void applyCooldown(String mechanicId, int cooldownTicks, int tick)
	{
		if (cooldownTicks > 0)
		{
			mechanicCooldownUntil.put(mechanicId, tick + cooldownTicks);
		}
	}
}
