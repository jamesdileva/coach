package com.coach.plugin.overlay;

import com.coach.plugin.coaching.PredictedMechanic;
import java.awt.Color;
import java.util.List;

/**
 * Big numeric countdown for the nearest predicted mechanic (≤ 5 ticks).
 */
public final class CountdownRenderer
{
	private CountdownRenderer()
	{
	}

	public static OverlayLine render(List<PredictedMechanic> predictions)
	{
		if (predictions == null || predictions.isEmpty())
		{
			return null;
		}
		PredictedMechanic next = predictions.get(0);
		int ticks = next.getTicksUntilFire();
		if (ticks > 5)
		{
			return null;
		}
		Color color = ticks <= 2 ? Color.RED : Color.YELLOW;
		return new OverlayLine(
			next.getMechanicId() + " in " + ticks + "!",
			color, OverlayLine.Size.LARGE);
	}
}
