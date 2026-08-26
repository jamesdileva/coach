package com.coach.plugin.events;

import javax.annotation.Nullable;

/**
 * Internal wrapper for a single RuneLite game event.
 * Carries the game tick it was received on plus the raw payload.
 */
public final class GameEvent
{
	private final EventType type;
	private final int tick;
	@Nullable
	private final Object payload;

	public GameEvent(EventType type, int tick, @Nullable Object payload)
	{
		this.type = type;
		this.tick = tick;
		this.payload = payload;
	}

	public EventType getType()
	{
		return type;
	}

	public int getTick()
	{
		return tick;
	}

	@Nullable
	public Object getPayload()
	{
		return payload;
	}

	@Override
	public String toString()
	{
		return "GameEvent{type=" + type + ", tick=" + tick + "}";
	}
}
