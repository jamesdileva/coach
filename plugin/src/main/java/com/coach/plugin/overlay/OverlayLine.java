package com.coach.plugin.overlay;

import java.awt.Color;

/**
 * One line of overlay output produced by a sub-renderer. Pure data so
 * renderers stay unit-testable headless; CoachOverlay turns these into
 * LineComponents.
 */
public final class OverlayLine
{
	public enum Size
	{
		SMALL, LARGE
	}

	public final String text;
	public final Color color;
	public final Size size;

	public OverlayLine(String text, Color color, Size size)
	{
		this.text = text;
		this.color = color;
		this.size = size;
	}
}
