package com.coach.plugin.accessibility;

import com.coach.plugin.config.AccessibilityMode;
import com.coach.plugin.config.CoachConfig;

/**
 * Central resolver for accessibility behaviour, reading live config:
 * - mode gates visuals vs audio delivery
 * - essential-only narrows the callout gate
 * - palette + text scale feed the overlay renderers
 */
public class AccessibilityManager
{
	public enum Mode { BOTH, AUDIO_ONLY, VISUAL_ONLY }

	private final CoachConfig config;

	public AccessibilityManager(CoachConfig config)
	{
		this.config = config;
	}

	public Mode getMode()
	{
		try
		{
			return Mode.valueOf(config.accessibilityMode().name());
		}
		catch (Exception e)
		{
			return Mode.BOTH;
		}
	}

	/** Should audio play? (visual-only silences everything) */
	public boolean isAudioEnabled()
	{
		return !config.muted() && getMode() != Mode.VISUAL_ONLY;
	}

	/** Should overlays render? (audio-only hides everything) */
	public boolean isVisualEnabled()
	{
		return getMode() != Mode.AUDIO_ONLY && config.enabled();
	}
}
