package com.coach.plugin.audio;

/**
 * Callout audio categories with priority ranks — higher rank interrupts lower.
 */
public enum AudioCategory
{
	TRANSITION(1),
	INFO(2),
	WARNING(3),
	CRITICAL(4);

	public final int rank;

	AudioCategory(int rank)
	{
		this.rank = rank;
	}

	/**
	 * Map a callout category string; unknown/null falls back to INFO.
	 */
	public static AudioCategory fromCalloutCategory(String category)
	{
		if (category == null)
		{
			return INFO;
		}
		switch (category)
		{
			case "critical":   return CRITICAL;
			case "warning":    return WARNING;
			case "transition": return TRANSITION;
			default:           return INFO;
		}
	}
}
