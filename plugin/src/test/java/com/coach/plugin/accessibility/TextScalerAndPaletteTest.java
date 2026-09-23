package com.coach.plugin.accessibility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Font;
import org.junit.jupiter.api.Test;

class TextScalerAndPaletteTest
{
	@Test
	void factorClampsToRange()
	{
		assertEquals(0.5f, TextScaler.factor(0), 1e-6);
		assertEquals(0.5f, TextScaler.factor(50), 1e-6);
		assertEquals(1.0f, TextScaler.factor(100), 1e-6);
		assertEquals(2.0f, TextScaler.factor(250), 1e-6);
		assertEquals(TextScaler.MIN_PERCENT, 50);
		assertEquals(TextScaler.MAX_PERCENT, 200);
	}

	@Test
	void scaleNullReturnsNullOtherwiseDerives()
	{
		assertNull(TextScaler.scale(null, 100));
		Font base = new Font("SansSerif", Font.PLAIN, 12);
		Font scaled = TextScaler.scale(base, 150);
		assertEquals(18f, scaled.getSize2D(), 1e-4);
		Font clamped = TextScaler.scale(base, 1);
		assertEquals(TextScaler.MIN_PERCENT / 100f * 12f, clamped.getSize2D(), 1e-4);
	}

	@Test
	void colorForCategoriesAndFallbacks()
	{
		assertEquals(new Color(0xFF0000), ColorPalette.colorFor("critical", false));
		assertEquals(new Color(0xFF6666), ColorPalette.colorFor("critical", true));
		assertEquals(new Color(0xFFA500), ColorPalette.colorFor("warning", false));
		assertEquals(new Color(0x00E5FF), ColorPalette.colorFor("transition", false));
		assertEquals(Color.WHITE, ColorPalette.colorFor("info", false));
		assertEquals(Color.WHITE, ColorPalette.colorFor("text", false));
		assertEquals(Color.WHITE, ColorPalette.colorFor(null, false));
		assertEquals(Color.WHITE, ColorPalette.colorFor("bogus", false));
		assertEquals(Color.WHITE, ColorPalette.colorFor("bogus", true));
		assertTrue(ColorPalette.DEFAULT.containsKey("critical"));
		assertTrue(ColorPalette.HIGH_CONTRAST.containsKey("warning"));
	}

	@Test
	void contrastRatioAgainstBlack()
	{
		assertTrue(ColorPalette.contrastRatioAgainstBlack(Color.WHITE) > 15);
		assertTrue(ColorPalette.contrastRatioAgainstBlack(new Color(0xFF0000)) >= 4.5);
		assertTrue(ColorPalette.contrastRatioAgainstBlack(Color.BLACK) < 1.1);
		assertTrue(ColorPalette.contrastRatioAgainstBlack(new Color(10, 10, 10)) >= 1);
	}
}
