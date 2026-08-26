package com.coach.plugin.config;

/**
 * A named snapshot of callout-relevant settings.
 */
public class ConfigProfile
{
	public String name;
	public boolean enabled;
	public boolean debugMode;
	public boolean muted;
	public int masterVolume;
	public boolean criticalCallouts;
	public boolean warningCallouts;
	public boolean infoCallouts;
	public boolean transitionCallouts;
	public String disabledBosses;
}
