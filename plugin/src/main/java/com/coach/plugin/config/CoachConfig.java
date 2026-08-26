package com.coach.plugin.config;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("coach")
public interface CoachConfig extends Config
{
	@ConfigItem(
		keyName = "enabled",
		name = "Enabled",
		description = "Enable Project Coach callouts"
	)
	default boolean enabled()
	{
		return true;
	}

	@ConfigItem(
		keyName = "debugMode",
		name = "Debug Mode",
		description = "Show the in-game debug overlay and record all game events"
	)
	default boolean debugMode()
	{
		return false;
	}

	@ConfigItem(
		keyName = "logToFile",
		name = "Log To File",
		description = "Write debug entries to coach/logs/coach-debug.log (requires Debug Mode)"
	)
	default boolean logToFile()
	{
		return false;
	}
}
