package com.coach.plugin.accessibility;

import java.awt.Font;

/**
 * Scales overlay text by a configurable percentage (clamped 50–200%).
 */
public final class TextScaler
{
	public static final int MIN_PERCENT = 50;
	public static final int MAX_PERCENT = 200;

	private TextScaler()
	{
	}

	public static float factor(int percent)
	{
		int clamped = Math.max(MIN_PERCENT, Math.min(MAX_PERCENT, percent));
		return clamped / 100f;
	}

	public static Font scale(Font base, int percent)
	{
		if (base == null)
		{
			return null;
		}
		return base.deriveFont(base.getSize2D() * factor(percent));
	}
}
