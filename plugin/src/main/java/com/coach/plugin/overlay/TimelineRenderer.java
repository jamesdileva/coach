package com.coach.plugin.overlay;

import java.awt.Color;

/**
 * Boss phase progress line: "Olm ▰▰▰▱▱ phase 3/5" style.
 */
public final class TimelineRenderer
{
	private static final int SEGMENTS = 8;

	private TimelineRenderer()
	{
	}

	public static OverlayLine render(String bossLabel, String phaseLabel, Double progress)
	{
		if (bossLabel == null || progress == null)
		{
			return null;
		}
		double clamped = Math.max(0.0, Math.min(1.0, progress));
		int filled = (int) Math.round(clamped * SEGMENTS);

		StringBuilder bar = new StringBuilder();
		for (int i = 0; i < SEGMENTS; i++)
		{
			bar.append(i < filled ? '▰' : '▱');
		}

		String text = bossLabel
			+ (phaseLabel != null ? " · " + phaseLabel : "")
			+ "  " + bar;
		return new OverlayLine(text, Color.LIGHT_GRAY, OverlayLine.Size.SMALL);
	}
}
