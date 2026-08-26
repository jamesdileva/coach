package com.coach.plugin.coaching;

/**
 * Suppresses duplicate callouts within a cooldown window
 * ("don't repeat the same callout within X ticks", §8.2).
 */
public class CooldownManager
{
	public static final int DEFAULT_CALLOUT_COOLDOWN_TICKS = 4;

	private final int defaultCooldownTicks;
	private final java.util.Map<String, Integer> cooldownUntil = new java.util.HashMap<>();

	public CooldownManager(int defaultCooldownTicks)
	{
		this.defaultCooldownTicks = defaultCooldownTicks;
	}

	public CooldownManager()
	{
		this(DEFAULT_CALLOUT_COOLDOWN_TICKS);
	}

	public boolean isOnCooldown(String calloutId, int tick)
	{
		Integer until = cooldownUntil.get(calloutId);
		return until != null && tick < until;
	}

	public void apply(String calloutId, int cooldownTicks, int tick)
	{
		int window = cooldownTicks > 0 ? cooldownTicks : defaultCooldownTicks;
		if (window > 0)
		{
			cooldownUntil.put(calloutId, tick + window);
		}
	}
}
