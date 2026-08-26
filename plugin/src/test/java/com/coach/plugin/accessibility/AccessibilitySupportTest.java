package com.coach.plugin.accessibility;

import java.awt.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessibilitySupportTest
{
	// ---- ColorPalette: WCAG AA on dark background ----

	@Test
	void defaultPaletteMeetsWcagAaAgainstBlack()
	{
		for (Color color : ColorPalette.DEFAULT.values())
		{
			assertTrue(ColorPalette.contrastRatioAgainstBlack(color) >= 4.5,
				"default palette colour fails AA: " + color);
		}
	}

	@Test
	void highContrastPaletteMeetsWcagAaAgainstBlack()
	{
		for (Color color : ColorPalette.HIGH_CONTRAST.values())
		{
			assertTrue(ColorPalette.contrastRatioAgainstBlack(color) >= 4.5,
				"high-contrast palette colour fails AA: " + color);
		}
	}

	@Test
	void highContrastCriticalIsDistinctFromDefault()
	{
		assertTrue(!ColorPalette.HIGH_CONTRAST.get("critical")
			.equals(ColorPalette.DEFAULT.get("critical")));
	}

	@Test
	void unknownCategoryFallsBackToInfo()
	{
		assertEquals(ColorPalette.colorFor("info", false),
			ColorPalette.colorFor("weird", false));
	}

	@Test
	void contrastRatioMathSanityCheck()
	{
		assertEquals(21.0, ColorPalette.contrastRatioAgainstBlack(Color.WHITE), 0.1);
		assertEquals(1.0, ColorPalette.contrastRatioAgainstBlack(Color.BLACK), 0.01);
	}

	// ---- TextScaler ----

	@Test
	void scalesFontsByPercent()
	{
		java.awt.Font base = new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 12);
		assertEquals(18f, TextScaler.scale(base, 150).getSize2D(), 0.01f);
		assertEquals(6f, TextScaler.scale(base, 50).getSize2D(), 0.01f);
	}

	@Test
	void textScaleClampedToFiftyToTwoHundred()
	{
		java.awt.Font base = new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 10);
		assertEquals(5f, TextScaler.scale(base, 10).getSize2D(), 0.01f);
		assertEquals(20f, TextScaler.scale(base, 500).getSize2D(), 0.01f);
		assertEquals(10f, TextScaler.scale(base, 100).getSize2D(), 0.01f);
	}

	@Test
	void factorClamps()
	{
		assertEquals(0.5f, TextScaler.factor(1));
		assertEquals(2f, TextScaler.factor(999));
		assertEquals(1f, TextScaler.factor(100));
	}
}
