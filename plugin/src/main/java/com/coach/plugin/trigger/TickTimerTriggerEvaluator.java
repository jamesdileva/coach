package com.coach.plugin.trigger;

import com.coach.plugin.events.EventType;
import com.coach.plugin.events.GameEvent;
import java.util.Set;

/**
 * Fires on ticks where (tick - offset) is a multiple of the configured modulus.
 */
public class TickTimerTriggerEvaluator implements TriggerEvaluator
{
	private final int tickMod;
	private final int tickOffset;

	public TickTimerTriggerEvaluator(int tickMod, int tickOffset)
	{
		this.tickMod = Math.max(1, tickMod);
		this.tickOffset = tickOffset;
	}

	@Override
	public Set<EventType> interestedIn()
	{
		return Set.of(EventType.TICK);
	}

	@Override
	public boolean matches(GameEvent event)
	{
		int delta = event.getTick() - tickOffset;
		return delta >= 0 && delta % tickMod == 0;
	}

	@Override
	public String describe()
	{
		return "every " + tickMod + " ticks" + (tickOffset != 0 ? " offset " + tickOffset : "");
	}
}
