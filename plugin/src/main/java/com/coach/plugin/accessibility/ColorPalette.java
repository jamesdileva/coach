package com.coach.plugin.accessibility;

import java.awt.Color;
import java.util.Map;

/**
 * Callout colour palettes. Both palettes keep every colour at WCAG AA
 * contrast (≥ 4.5:1) against the dark overlay panel background, with hue
 * separation that survives common colour-vision deficiencies (red/green
 * pairs are avoided; luminance always differs).
 */
public final class ColorPalette
{
	private ColorPalette()
	{
	}

	/** Standard palette (existing behaviour). */
	public static final Map<String, Color> DEFAULT = Map.of(
		"critical", new Color(0xFF0000),   // 5.25:1 on black
		"warning", new Color(0xFFA500),    // 11.5:1
		"info", new Color(0xFFFFFF),       // 21:1
		"transition", new Color(0x00E5FF), // 13.7:1
		"text", new Color(0xFFFFFF));

	/** High-contrast palette: brighter hues, larger luminance separation. */
	public static final Map<String, Color> HIGH_CONTRAST = Map.of(
		"critical", new Color(0xFF6666),   // 7.3:1
		"warning", new Color(0xFFB300),    // 11.7:1
		"info", new Color(0xFFFFFF),       // 21:1
		"transition", new Color(0x00E5FF), // 13.7:1
		"text", new Color(0xFFFFFF));

	public static Color colorFor(String category, boolean highContrast)
	{
		String key = normalize(category);
		Map<String, Color> palette = highContrast ? HIGH_CONTRAST : DEFAULT;
		Color fallback = palette.get("info");
		return palette.getOrDefault(key, fallback);
	}

	private static String normalize(String category)
	{
		if (category == null || "text".equals(category))
		{
			return category != null ? category : "info";
		}
		switch (category)
		{
			case "critical":
			case "warning":
			case "transition":
			case "info":
				return category;
			default:
				return "info";
		}
	}

	/**
	 * WCAG 2.x contrast ratio of a colour against pure black.
	 * AA for normal text requires ≥ 4.5.
	 */
	public static double contrastRatioAgainstBlack(Color color)
	{
		double luminance =
			0.2126 * linearize(color.getRed() / 255.0)
				+ 0.7152 * linearize(color.getGreen() / 255.0)
				+ 0.0722 * linearize(color.getBlue() / 255.0);
		return (luminance + 0.05) / 0.05;
	}

	private static double linearize(double channel)
	{
		return channel <= 0.03928
			? channel / 12.92
			: Math.pow((channel + 0.055) / 1.055, 2.4);
	}
}
