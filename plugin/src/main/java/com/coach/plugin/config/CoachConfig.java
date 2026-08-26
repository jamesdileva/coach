package com.coach.plugin.config;

import net.runelite.client.RuneLite;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("coach")
public interface CoachConfig extends Config
{
	@ConfigItem(
		keyName = "enabled",
		name = "Enabled",
		description = "Enable Project Coach callouts",
		position = 1
	)
	default boolean enabled()
	{
		return true;
	}

	@ConfigItem(
		keyName = "debugMode",
		name = "Debug Mode",
		description = "Show the in-game debug overlay and record all game events",
		position = 2
	)
	default boolean debugMode()
	{
		return false;
	}

	@ConfigItem(
		keyName = "logToFile",
		name = "Log To File",
		description = "Write debug entries to coach/logs/coach-debug.log (requires Debug Mode)",
		position = 3
	)
	default boolean logToFile()
	{
		return false;
	}

	@ConfigItem(
		keyName = "packDirectory",
		name = "Encounter Pack Directory",
		description = "Folder containing encounter .zip packs (changes trigger a reload)",
		position = 4
	)
	default String packDirectory()
	{
		return RuneLite.RUNELITE_DIR.toPath().resolve("coach").resolve("encounters").toString();
	}

	@ConfigItem(
		keyName = "muted",
		name = "Mute All",
		description = "Suppress all audio callouts",
		position = 5
	)
	default boolean muted()
	{
		return false;
	}

	@ConfigItem(
		keyName = "masterVolume",
		name = "Master Volume",
		description = "Audio callout volume (0-100)",
		position = 6
	)
	default int masterVolume()
	{
		return 70;
	}

	@ConfigItem(
		keyName = "criticalCallouts",
		name = "Critical Callouts",
		description = "Show/speak critical callouts (immediate action required)",
		position = 10
	)
	default boolean criticalCallouts()
	{
		return true;
	}

	@ConfigItem(
		keyName = "warningCallouts",
		name = "Warning Callouts",
		description = "Show/speak warning callouts (upcoming mechanics)",
		position = 11
	)
	default boolean warningCallouts()
	{
		return true;
	}

	@ConfigItem(
		keyName = "infoCallouts",
		name = "Info Callouts",
		description = "Show/speak informational callouts",
		position = 12
	)
	default boolean infoCallouts()
	{
		return true;
	}

	@ConfigItem(
		keyName = "transitionCallouts",
		name = "Transition Callouts",
		description = "Show/speak phase-transition callouts",
		position = 13
	)
	default boolean transitionCallouts()
	{
		return true;
	}

	@ConfigItem(
		keyName = "disabledBosses",
		name = "Disabled Bosses",
		description = "Comma-separated bossIds with callouts suppressed (e.g. 'nex, inferno'). Boss ids come from the loaded packs.",
		position = 14
	)
	default String disabledBosses()
	{
		return "";
	}

	@ConfigItem(
		keyName = "profilesJson",
		name = "Profiles",
		description = "Internal: named settings profiles (managed via ProfileManager)",
		hidden = true
	)
	default String profilesJson()
	{
		return "{}";
	}
}
