package com.coach.plugin.overlay;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import javax.inject.Inject;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;

/**
 * Main visual callout overlay: renders active coaching callouts prominently
 * plus the next-mechanic prediction line.
 */
public class CoachOverlay extends Overlay
{
	private final OverlayManager overlayManager;
	private final PanelComponent panel = new PanelComponent();

	@Inject
	public CoachOverlay(OverlayManager overlayManager)
	{
		this.overlayManager = overlayManager;
		setPosition(OverlayPosition.TOP_CENTER);
		setPriority(OverlayPriority.HIGH);
		panel.setPreferredSize(new Dimension(220, 0));
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		java.util.List<OverlayManager.ActiveVisual> visuals = overlayManager.getActiveVisuals();
		java.util.List<com.coach.plugin.coaching.PredictedMechanic> predictions =
			overlayManager.getPredictions();

		if (visuals.isEmpty() && predictions.isEmpty())
		{
			return null;
		}

		panel.getChildren().clear();

		for (OverlayManager.ActiveVisual visual : visuals)
		{
			panel.getChildren().add(LineComponent.builder()
				.left(visual.text)
				.leftColor(colorFor(visual.category))
				.build());
		}

		if (!predictions.isEmpty())
		{
			com.coach.plugin.coaching.PredictedMechanic next = predictions.get(0);
			panel.getChildren().add(LineComponent.builder()
				.left("next: " + next.getMechanicId() + " (" + next.getTicksUntilFire() + "t)")
				.build());
		}

		return panel.render(graphics);
	}

	private static java.awt.Color colorFor(String category)
	{
		if (category == null)
		{
			return java.awt.Color.WHITE;
		}
		switch (category)
		{
			case "critical":   return java.awt.Color.RED;
			case "warning":    return java.awt.Color.ORANGE;
			case "transition": return java.awt.Color.CYAN;
			default:           return java.awt.Color.WHITE; // info
		}
	}
}
