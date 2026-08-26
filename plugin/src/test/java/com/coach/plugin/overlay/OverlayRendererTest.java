package com.coach.plugin.overlay;

import com.coach.plugin.coaching.PredictedMechanic;
import java.awt.Color;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OverlayRendererTest
{
	private static OverlayManager.ActiveVisual visual(String visualType, String category, String text)
	{
		return new OverlayManager.ActiveVisual("boss", text, category, visualType, 100);
	}

	@Test
	void prayerRendererPicksPrayerIconVisualAndFlashes()
	{
		var visuals = List.of(
			visual("text", "info", "noise"),
			visual("prayer_icon", "critical", "Pray Magic"));

		OverlayLine line = PrayerIndicatorRenderer.render(visuals, 4);
		assertEquals("[PRAY] Pray Magic", line.text);
		assertEquals(OverlayLine.Size.LARGE, line.size);
		Color first = line.color;

		Color second = PrayerIndicatorRenderer.render(visuals, 5).color;
		assertTrue(!first.equals(second), "flashes between two colours");
	}

	@Test
	void prayerRendererIgnoresNonPrayerVisuals()
	{
		assertNull(PrayerIndicatorRenderer.render(
			List.of(visual("text", "critical", "plain")), 0));
	}

	@Test
	void countdownRendersOnlyWithinFiveTicks()
	{
		assertNull(CountdownRenderer.render(List.of(new PredictedMechanic("b", "m", 6))));

		OverlayLine near = CountdownRenderer.render(List.of(new PredictedMechanic("b", "smash", 3)));
		assertEquals("smash in 3!", near.text);

		OverlayLine imminent = CountdownRenderer.render(List.of(new PredictedMechanic("b", "smash", 1)));
		assertEquals(Color.RED, imminent.color);
	}

	@Test
	void timelineFormatsBarWithPhaseLabels()
	{
		OverlayLine line = TimelineRenderer.render("Olm", "Phase 2 (2/4)", 0.5);
		assertTrue(line.text.contains("Olm · Phase 2 (2/4)"));
		assertTrue(line.text.contains("▰▰▰▰▱▱▱▱"), line.text);
	}

	@Test
	void statusColoursByHpThreshold()
	{
		assertEquals(Color.RED, StatusIndicatorRenderer.render(20).color);
		assertEquals(Color.ORANGE, StatusIndicatorRenderer.render(40).color);
		assertEquals(Color.GREEN, StatusIndicatorRenderer.render(90).color);
		assertNull(StatusIndicatorRenderer.render(null));
	}

	@Test
	void safeTileRendersMoveAdvisory()
	{
		OverlayLine line = SafeTileRenderer.render(List.of(
			visual("safe_tile", "critical", "Containment! Move!")), 3);
		assertEquals("[MOVE] Containment! Move!", line.text);
	}

	@Test
	void miniHudComposesCompactSummary()
	{
		OverlayLine hud = MiniHudRenderer.render("Nex", "Blood (3/5)",
			List.of(new PredictedMechanic("nex", "siphon", 4)), 62);

		assertTrue(hud.text.startsWith("HP62%"), hud.text);
		assertTrue(hud.text.contains("Nex · Blood (3/5)"));
		assertTrue(hud.text.contains("next: siphon 4t"));
	}
}
