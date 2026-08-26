package com.coach.plugin.audio;

/**
 * Maps callout categories to audio priorities (roadmap Sprint 19).
 * Kept as a class for API stability even though the mapping is simple.
 */
public class AudioPriorityResolver
{
	public AudioCategory resolve(String calloutCategory)
	{
		return AudioCategory.fromCalloutCategory(calloutCategory);
	}
}
