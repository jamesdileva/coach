package com.coach.plugin.debug;

import com.coach.plugin.config.CoachConfig;
import com.coach.plugin.logging.LogBuffer;
import java.awt.Dimension;
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
 * Tabbed debug overlay (roadmap Sprint 21): Events / Triggers / State /
 * Timeline. The visible tab comes from the `debugTab` config item; the
 * debug-mode toggle still opens/closes the whole overlay.
 */
public class DebugOverlayV2 extends Overlay
{
	private static final int MAX_LINES = 28;

	private final LogBuffer logBuffer;
	private final TriggerHistory triggerHistory;
	private final EventTimeline eventTimeline;
	private final StateInspector stateInspector;
	private final CoachConfig config;
	private final PanelComponent panel = new PanelComponent();

	@Inject
	public DebugOverlayV2(LogBuffer logBuffer, TriggerHistory triggerHistory,
		EventTimeline eventTimeline, StateInspector stateInspector, CoachConfig config)
	{
		this.logBuffer = logBuffer;
		this.triggerHistory = triggerHistory;
		this.eventTimeline = eventTimeline;
		this.stateInspector = stateInspector;
		this.config = config;
		setPosition(OverlayPosition.TOP_LEFT);
		setPriority(OverlayPriority.LOW);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		List<String> lines = compose();
		if (lines.isEmpty())
		{
			return null;
		}

		panel.getChildren().clear();
		int width = 0;
		int y = 6;
		graphics.setFont(FontManager.getRunescapeSmallFont());

		// header
		String header = "== coach debug: " + config.debugTab() + " ==";
		width = Math.max(width, graphics.getFontMetrics().stringWidth(header));
		panel.getChildren().add(LineComponent.builder()
			.left(header)
			.leftColor(java.awt.Color.YELLOW)
			.build());

		for (String line : lines.subList(0, Math.min(MAX_LINES, lines.size())))
		{
			width = Math.max(width, graphics.getFontMetrics().stringWidth(line));
			panel.getChildren().add(LineComponent.builder()
				.left(line)
				.build());
			y += 14;
		}

		return new Dimension(width + 12, y + 12);
	}

	List<String> compose()
	{
		List<String> lines = new ArrayList<>();
		switch (config.debugTab())
		{
			case TRIGGERS:
				lines.addAll(triggerHistory.format(null, MAX_LINES));
				break;
			case STATE:
				lines.addAll(stateInspector.format());
				break;
			case TIMELINE:
				lines.addAll(eventTimeline.format(MAX_LINES));
				break;
			case EVENTS:
			default:
			{
				var entries = logBuffer.snapshot();
				int from = Math.max(0, entries.size() - MAX_LINES);
				lines.addAll(entries.subList(from, entries.size()));
				if (lines.isEmpty())
				{
					lines.add("(no events logged)");
				}
			}
		}
		return lines;
	}
}
