package com.coach.plugin.encounter.model;

/**
 * Visual rendering config for a callout (schema v1). Populated by Gson.
 */
public class VisualDefinition
{
	public String type;      // prayer_icon | countdown | text | safe_tile | status_bar | timeline | mini_hud
	public String color;     // #RRGGBB
	public Float opacity;    // 0..1
	public Integer durationTicks;
}
