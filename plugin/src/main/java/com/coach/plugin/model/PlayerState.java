package com.coach.plugin.model;

/**
 * Immutable snapshot of the local player's state.
 * Position is stored as plain ints to keep RuneLite API types out of the engine.
 */
public final class PlayerState
{
	private final int hp;
	private final int maxHp;
	private final int posX;
	private final int posY;
	private final int plane;
	private final int animation;

	public PlayerState(int hp, int maxHp, int posX, int posY, int plane, int animation)
	{
		this.hp = hp;
		this.maxHp = maxHp;
		this.posX = posX;
		this.posY = posY;
		this.plane = plane;
		this.animation = animation;
	}

	public int getHp()
	{
		return hp;
	}

	public int getMaxHp()
	{
		return maxHp;
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
