package com.coach.plugin.overlay;

import com.coach.plugin.encounter.model.CalloutDefinition;
import com.coach.plugin.encounter.model.VisualDefinition;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OverlayManagerTest
{
	private final OverlayManager manager = new OverlayManager();

	private static CalloutDefinition callout(String text, Integer duration)
	{
		CalloutDefinition callout = new CalloutDefinition();
		callout.calloutId = "c1";
		callout.text = text;
		callout.category = "critical";
		if (duration != null)
		{
			callout.visual = new VisualDefinition();
			callout.visual.durationTicks = duration;
		}
		return callout;
	}

	@Test
	void visualsExpireAfterDuration()
	{
		manager.addVisual("boss", callout("Pray Ranged!", null), 100); // default 3 ticks

		assertEquals(1, manager.getActiveVisuals().size());
		manager.prune(102);
		assertEquals(1, manager.getActiveVisuals().size(), "still within duration");
		manager.prune(103);
		assertTrue(manager.getActiveVisuals().isEmpty());
	}

	@Test
	void packDefinedDurationHonored()
	{
		manager.addVisual("boss", callout("Long", 10), 100);
		manager.prune(109);
		assertEquals(1, manager.getActiveVisuals().size());
	}

	@Test
	void cappedAtMaxVisualsDroppingOldest()
	{
		for (int i = 0; i < OverlayManager.MAX_VISUALS + 2; i++)
		{
			manager.addVisual("boss", callout("c" + i, 50), 100);
		}
		assertEquals(OverlayManager.MAX_VISUALS, manager.getActiveVisuals().size());
		assertEquals("c2", manager.getActiveVisuals().get(0).text, "oldest dropped");
	}

	@Test
	void predictionsSnapshot()
	{
		com.coach.plugin.coaching.PredictedMechanic prediction =
			new com.coach.plugin.coaching.PredictedMechanic("boss", "m1", 3);
		manager.setPredictions(List.of(prediction));

		assertEquals(1, manager.getPredictions().size());
		manager.setPredictions(null);
		assertTrue(manager.getPredictions().isEmpty());
	}
}
