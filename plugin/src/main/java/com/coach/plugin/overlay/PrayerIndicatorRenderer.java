package com.coach.plugin.overlay;

import java.awt.Color;
import java.util.List;

/**
 * Renders the prayer guidance line from prayer_icon visuals — the highest
 * priority action on screen. Colorblind-safe: colour is secondary to text.
 */
public final class PrayerIndicatorRenderer
{
	private PrayerIndicatorRenderer()
	{
	}

	/**
	 * @return the most important prayer_icon visual as an overlay line, or null
	 */
	public static OverlayLine render(List<OverlayManager.ActiveVisual> visuals, int tick)
	{
		for (OverlayManager.ActiveVisual visual : visuals)
		{
			if (!isPrayerVisual(visual))
			{
				continue;
			}
			Color color = tick % 2 == 0 ? Color.RED : Color.WHITE; // flash
			return new OverlayLine("[PRAY] " + visual.text, color, OverlayLine.Size.LARGE);
		}
		return null;
	}

	static boolean isPrayerVisual(OverlayManager.ActiveVisual visual)
	{
		return "prayer_icon".equals(visual.visualType);
	}
}
