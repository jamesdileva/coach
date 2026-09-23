package com.coach.plugin.overlay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.coach.plugin.coaching.PredictedMechanic;
import com.coach.plugin.encounter.model.CalloutDefinition;
import com.coach.plugin.encounter.model.VisualDefinition;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class OverlayManagerTest
{
	private static CalloutDefinition callout(String category, String text, Integer duration, String type)
	{
		CalloutDefinition c = new CalloutDefinition();
		c.calloutId = "cal-" + category;
		c.text = text;
		c.category = category;
		if (duration != null || type != null)
		{
			c.visual = new VisualDefinition();
			c.visual.durationTicks = duration;
			c.visual.type = type;
		}
		return c;
	}

	@Test
	void quietHoursAfterCritical()
	{
		OverlayManager manager = new OverlayManager();
		assertFalse(manager.isQuiet(0));
		manager.noteCriticalDelivered(10);
		assertTrue(manager.isQuiet(11));
		assertTrue(manager.isQuiet(10 + OverlayManager.QUIET_TICKS_AFTER_CRITICAL - 1));
		assertFalse(manager.isQuiet(10 + OverlayManager.QUIET_TICKS_AFTER_CRITICAL));
	}

	@Test
	void liveStateSettersAndters()
	{
		OverlayManager manager = new OverlayManager();
		manager.setPlayerHpPercent(75);
		assertEquals(75, manager.getPlayerHpPercent());
		manager.setPhaseProgress(0.5);
		assertEquals(0.5, manager.getPhaseProgress(), 1e-9);
		manager.setCurrentBossLabel("Nex");
		manager.setCurrentPhaseLabel("Smoke");
		assertEquals("Nex", manager.getCurrentBossLabel());
		assertEquals("Smoke", manager.getCurrentPhaseLabel());
		manager.setPlayerHpPercent(null);
		manager.setPhaseProgress(null);
		assertEquals(null, manager.getPlayerHpPercent());
		assertEquals(null, manager.getPhaseProgress());
	}

	@Test
	void addVisualDefaultDurationAndType()
	{
		OverlayManager manager = new OverlayManager();
		manager.addVisual("nex", callout("warning", "duck", null, null), 100);
		OverlayManager.ActiveVisual visual = manager.getActiveVisuals().get(0);
		assertEquals("nex", visual.bossId);
		assertEquals("duck", visual.text);
		assertEquals("warning", visual.category);
		assertEquals("text", visual.visualType);
		assertEquals(100 + OverlayManager.DEFAULT_DURATION_TICKS, visual.expireTick);
	}

	@Test
	void addVisualUsesPackDurationAndFallsBackToCalloutId()
	{
		OverlayManager manager = new OverlayManager();
		manager.addVisual("nex", callout("critical", null, 1, "prayer_icon"), 5);
		manager.addVisual("nex", callout("critical", null, 0, null), 5);
		assertEquals(2, manager.getActiveVisuals().size());
		assertEquals("prayer_icon", manager.getActiveVisuals().get(0).visualType);
		assertEquals("cal-critical", manager.getActiveVisuals().get(0).text);
		assertEquals(6, manager.getActiveVisuals().get(0).expireTick);
		assertEquals(6, manager.getActiveVisuals().get(1).expireTick);
	}

	@Test
	void quietSuppressesNonCriticalAndCapsAtMax()
	{
		OverlayManager manager = new OverlayManager();
		manager.noteCriticalDelivered(0);
		manager.addVisual("nex", callout("warning", "wait", null, null), 1);
		assertEquals(0, manager.getActiveVisuals().size());
		manager.addVisual("nex", callout("critical", "NOW", null, null), 1);
		assertEquals(1, manager.getActiveVisuals().size());

		for (int i = 0; i < OverlayManager.MAX_VISUALS + 3; i++)
		{
			manager.addVisual("boss", callout("info", "n" + i, 10, null), 100);
		}
		assertEquals(OverlayManager.MAX_VISUALS, manager.getActiveVisuals().size());
		assertEquals("n3", manager.getActiveVisuals().get(0).text);
	}

	@Test
	void pruneDropsExpiredVisuals()
	{
		OverlayManager manager = new OverlayManager();
		manager.addVisual("nex", callout("warning", "short", 1, null), 10);
		assertEquals(1, manager.getActiveVisuals().size());
		manager.prune(11);
		assertEquals(0, manager.getActiveVisuals().size());

		manager.addVisual("nex", callout("warning", "later", 5, null), 10);
		manager.prune(14);
		assertEquals(1, manager.getActiveVisuals().size());
		manager.prune(15);
		assertEquals(0, manager.getActiveVisuals().size());
	}

	@Test
	void predictionsCopyAndNullSafe()
	{
		OverlayManager manager = new OverlayManager();
		assertEquals(List.of(), manager.getPredictions());
		List<PredictedMechanic> source = new ArrayList<>();
		source.add(new PredictedMechanic("nex", "smoke", 3));
		manager.setPredictions(source);
		source.clear();
		assertEquals(1, manager.getPredictions().size());
		assertEquals("smoke", manager.getPredictions().get(0).getMechanicId());
		manager.setPredictions(null);
		assertEquals(List.of(), manager.getPredictions());
	}

	@Test
	void activeVisualsIsDefensiveCopy()
	{
		OverlayManager manager = new OverlayManager();
		manager.addVisual("nex", callout("info", "a", null, null), 0);
		List<OverlayManager.ActiveVisual> copy = manager.getActiveVisuals();
		assertEquals(1, copy.size());
		manager.prune(100);
		assertEquals(1, copy.size());
	}
}
