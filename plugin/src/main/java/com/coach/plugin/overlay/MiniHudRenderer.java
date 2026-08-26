package com.coach.plugin.overlay;

import com.coach.plugin.coaching.PredictedMechanic;
import java.awt.Color;
import java.util.List;

/**
 * Persistent mini HUD: current encounter context in one compact line.
 */
public final class MiniHudRenderer
{
	private MiniHudRenderer()
	{
	}

	public static OverlayLine render(String bossLabel, String phaseLabel,
		List<PredictedMechanic> predictions, Integer playerHpPercent)
	{
		if (bossLabel == null && phaseLabel == null
			&& (predictions == null || predictions.isEmpty()) && playerHpPercent == null)
		{
			return null;
		}

		StringBuilder sb = new StringBuilder();
		if (playerHpPercent != null)
		{
			sb.append("HP").append(playerHpPercent).append("%  ");
		}
		if (bossLabel != null)
		{
			sb.append(bossLabel);
		}
		if (phaseLabel != null)
		{
			sb.append(" · ").append(phaseLabel);
		}
		if (predictions != null && !predictions.isEmpty())
		{
			sb.append(" · next: ").append(predictions.get(0).getMechanicId())
				.append(' ').append(predictions.get(0).getTicksUntilFire()).append('t');
		}

		return new OverlayLine(sb.toString(), Color.LIGHT_GRAY, OverlayLine.Size.SMALL);
	}
}
