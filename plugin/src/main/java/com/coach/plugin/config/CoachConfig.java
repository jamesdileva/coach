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
		keyName = "criticalVolume",
		name = "Critical Volume",
		description = "Volume multiplier for critical callouts (0-100)",
		position = 7
	)
	default int criticalVolume()
	{
		return 100;
	}

	@ConfigItem(
		keyName = "warningVolume",
		name = "Warning Volume",
		description = "Volume multiplier for warning callouts (0-100)",
		position = 8
	)
	default int warningVolume()
	{
		return 80;
	}

	@ConfigItem(
		keyName = "infoVolume",
		name = "Info Volume",
		description = "Volume multiplier for info callouts (0-100)",
		position = 9
	)
	default int infoVolume()
	{
		return 60;
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

	@ConfigItem(
		keyName = "showPrayerIndicator",
		name = "Prayer Indicator",
		description = "Large flashing prayer guidance overlay",
		position = 20
	)
	default boolean showPrayerIndicator()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showCountdown",
		name = "Countdowns",
		description = "Countdown to the next predicted mechanic (5 ticks or fewer)",
		position = 21
	)
	default boolean showCountdown()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showTimeline",
		name = "Phase Timeline",
		description = "Boss phase progress bar",
		position = 22
	)
	default boolean showTimeline()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showStatus",
		name = "Status Indicator",
		description = "Show your HP percentage in the coach panel",
		position = 23
	)
	default boolean showStatus()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showMiniHud",
		name = "Mini HUD",
		description = "Compact persistent encounter summary line",
		position = 24
	)
	default boolean showMiniHud()
	{
		return true;
	}

	@ConfigItem(
		keyName = "accessibilityMode",
		name = "Accessibility Mode",
		description = "Both = normal. Audio-only hides all visuals. Visual-only silences all audio",
		position = 30
	)
	default AccessibilityMode accessibilityMode()
	{
		return AccessibilityMode.BOTH;
	}

	@ConfigItem(
		keyName = "essentialOnly",
		name = "Essential Only",
		description = "Only critical callouts (silence warnings, info, transitions)",
		position = 31
	)
	default boolean essentialOnly()
	{
		return false;
	}

	@ConfigItem(
		keyName = "highContrast",
		name = "High Contrast Palette",
		description = "WCAG AA-compliant overlay colours (4.5:1+ on dark backgrounds)",
		position = 32
	)
	default boolean highContrast()
	{
		return false;
	}

	@ConfigItem(
		keyName = "textScale",
		name = "Text Scale (%)",
		description = "Overlay text scaling in percent (50-200)",
		position = 33
	)
	default int textScale()
	{
		return 100;
	}

	@ConfigItem(
		keyName = "debugTab",
		name = "Debug Tab",
		description = "Which debug view to show (requires Debug Mode)",
		position = 40
	)
	default DebugTab debugTab()
	{
		return DebugTab.EVENTS;
	}

	enum DebugTab { EVENTS, TRIGGERS, STATE, TIMELINE }
}
