package com.coach.plugin.overlay;

import java.awt.Color;
import java.util.List;

/**
 * Safe-tile advisories: for v1 these render as advisory lines ("avoid the
 * marked area") rather than tile highlights — true tile rendering needs
 * scene-space projection and is tracked as a future engine feature.
 */
public final class SafeTileRenderer
{
	private SafeTileRenderer()
	{
	}

	public static OverlayLine render(List<OverlayManager.ActiveVisual> visuals, int tick)
	{
		for (OverlayManager.ActiveVisual visual : visuals)
		{
			if (!"safe_tile".equals(visual.visualType))
			{
				continue;
			}
			boolean urgent = tick % 2 == 0;
			return new OverlayLine(
				"[MOVE] " + visual.text,
				urgent ? Color.RED : Color.WHITE,
				OverlayLine.Size.LARGE);
		}
		return null;
	}
}
