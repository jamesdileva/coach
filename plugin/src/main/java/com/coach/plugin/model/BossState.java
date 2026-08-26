package com.coach.plugin.model;

import javax.annotation.Nullable;

/**
 * Immutable snapshot of an NPC's (boss's) state.
 *
 * Health is stored as ratio/scale exactly as reported by the client
 * (RuneLite does not expose absolute NPC max HP).
 */
public final class BossState
{
	private final int npcId;
	@Nullable
	private final String name;
	private final int healthRatio;
	private final int healthScale;
	private final int posX;
	private final int posY;
	private final int plane;
	private final int animation;

	public BossState(int npcId, @Nullable String name, int healthRatio, int healthScale,
		int posX, int posY, int plane, int animation)
	{
		this.npcId = npcId;
		this.name = name;
		this.healthRatio = healthRatio;
		this.healthScale = healthScale;
		this.posX = posX;
		this.posY = posY;
		this.plane = plane;
		this.animation = animation;
	}

	public int getNpcId()
	{
		return npcId;
	}

	@Nullable
	public String getName()
	{
		return name;
	}

	public int getHealthRatio()
	{
		return healthRatio;
	}

	public int getHealthScale()
	{
		return healthScale;
	}

	public int getPosX()
	{
		return posX;
	}

	public int getPosY()
	{
		return posY;
	}

	public int getPlane()
	{
		return plane;
	}

	public int getAnimation()
	{
		return animation;
	}
}
