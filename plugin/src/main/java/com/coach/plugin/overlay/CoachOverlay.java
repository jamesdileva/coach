package com.coach.plugin.overlay;

import com.coach.plugin.accessibility.ColorPalette;
import com.coach.plugin.accessibility.TextScaler;
import com.coach.plugin.config.CoachConfig;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;

/**
 * Main coach panel: composes the sub-renderers (prayer indicator, safe-tile
 * advisories, countdowns, callout text lines, timeline, status, mini HUD)
 * honouring per-overlay config toggles and accessibility settings.
 */
public class CoachOverlay extends Overlay
{
	private final OverlayManager overlayManager;
	private final CoachConfig config;
	private final com.coach.plugin.accessibility.AccessibilityManager accessibility;
	private final PanelComponent panel = new PanelComponent();

	@Inject
	public CoachOverlay(OverlayManager overlayManager, CoachConfig config,
		com.coach.plugin.accessibility.AccessibilityManager accessibility)
	{
		this.overlayManager = overlayManager;
		this.config = config;
		this.accessibility = accessibility;
		setPosition(OverlayPosition.TOP_CENTER);
		setPriority(OverlayPriority.HIGH);
		panel.setPreferredSize(new Dimension(240, 0));
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!accessibility.isVisualEnabled())
		{
			return null; // audio-only mode: hide every visual
		}

		int tick = tickFromExpiry();
		List<OverlayLine> lines = compose(tick);
		if (lines.isEmpty())
		{
			return null;
		}

		panel.getChildren().clear();
		int width = 0;
		for (OverlayLine line : lines)
		{
			Font font = line.size == OverlayLine.Size.LARGE
				? FontManager.getRunescapeBoldFont()
				: FontManager.getRunescapeSmallFont();
			font = TextScaler.scale(font, config.textScale());
			graphics.setFont(font);
			width = Math.max(width, graphics.getFontMetrics().stringWidth(line.text));

			panel.getChildren().add(LineComponent.builder()
				.left(line.text)
				.leftColor(line.color)
				.build());
		}
		panel.setPreferredSize(new Dimension(width + 16, 0));
		return panel.render(graphics);
	}

	private List<OverlayLine> compose(int tick)
	{
		var visuals = overlayManager.getActiveVisuals();
		var predictions = overlayManager.getPredictions();
		List<OverlayLine> lines = new ArrayList<>();

		if (config.showPrayerIndicator())
		{
			add(lines, PrayerIndicatorRenderer.render(visuals, tick));
		}
		if (config.showCountdown())
		{
			add(lines, CountdownRenderer.render(predictions));
		}
		add(lines, SafeTileRenderer.render(visuals, tick));

		// regular callout texts (everything not rendered by a dedicated overlay)
		boolean hc = config.highContrast();
		for (OverlayManager.ActiveVisual visual : visuals)
		{
			String type = visual.visualType;
			if ("prayer_icon".equals(type) || "safe_tile".equals(type))
			{
				continue;
			}
			lines.add(new OverlayLine(visual.text,
				ColorPalette.colorFor(visual.category, hc), OverlayLine.Size.SMALL));
		}

		if (config.showStatus())
		{
			add(lines, StatusIndicatorRenderer.render(overlayManager.getPlayerHpPercent()));
		}
		if (config.showTimeline())
		{
			add(lines, TimelineRenderer.render(
				overlayManager.getCurrentBossLabel(),
				overlayManager.getCurrentPhaseLabel(),
				overlayManager.getPhaseProgress()));
		}
		if (config.showMiniHud())
		{
			add(lines, MiniHudRenderer.render(
				overlayManager.getCurrentBossLabel(),
				overlayManager.getCurrentPhaseLabel(),
				predictions,
				config.showStatus() ? null : overlayManager.getPlayerHpPercent()));
		}
		return lines;
	}

	private static void add(List<OverlayLine> lines, OverlayLine line)
	{
		if (line != null)
		{
			lines.add(line);
		}
	}

	/**
	 * Visuals carry absolute expiry ticks; derive a "current tick" for flashing
	 * from the soonest expiry (visuals expire at tick+duration). Good enough
	 * for colour flashing without plumbing client ticks into the renderer.
	 */
	private int tickFromExpiry()
	{
		int min = Integer.MAX_VALUE;
		for (OverlayManager.ActiveVisual visual : overlayManager.getActiveVisuals())
		{
			min = Math.min(min, visual.expireTick);
		}
		return min == Integer.MAX_VALUE ? (int) (System.currentTimeMillis() / 600) : min;
	}
}
