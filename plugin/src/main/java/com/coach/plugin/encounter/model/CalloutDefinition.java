package com.coach.plugin.encounter.model;

/**
 * A callout definition from encounter JSON (schema v1). Populated by Gson.
 */
public class CalloutDefinition
{
	public String calloutId;
	public String text;
	public String audioFile;     // optional .ogg filename inside the pack
	public String category;      // critical | warning | info | transition
	public Integer priority;     // 1-100
	public Integer audioOffset;  // ticks, -5..10
	public Integer visualOffset; // ticks, -5..10
	public VisualDefinition visual;

	// runtime context
	public transient String bossId;
}
