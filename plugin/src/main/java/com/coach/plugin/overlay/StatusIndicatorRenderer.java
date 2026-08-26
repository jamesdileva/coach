package com.coach.plugin.overlay;

import java.awt.Color;

/**
 * Player status indicator: HP percent with a simple threshold colour.
 */
public final class StatusIndicatorRenderer
{
	private StatusIndicatorRenderer()
	{
	}

	public static OverlayLine render(Integer playerHpPercent)
	{
		if (playerHpPercent == null)
		{
			return null;
		}
		Color color = playerHpPercent <= 25 ? Color.RED
			: playerHpPercent <= 50 ? Color.ORANGE
			: Color.GREEN;
		return new OverlayLine("HP " + playerHpPercent + "%", color, OverlayLine.Size.SMALL);
	}
}
