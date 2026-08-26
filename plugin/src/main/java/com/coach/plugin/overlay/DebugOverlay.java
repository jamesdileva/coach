package com.coach.plugin.overlay;

import com.coach.plugin.logging.LogBuffer;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.List;
import javax.inject.Inject;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.TextComponent;

/**
 * In-game debug overlay: renders the most recent log entries
 * (events, trigger evaluations, callout decisions) top-left.
 */
public class DebugOverlay extends Overlay
{
	private static final int MAX_LINES = 30;
	private static final int LINE_HEIGHT = 14;
	private static final int PADDING = 6;

	private final LogBuffer logBuffer;
	private final java.util.function.Supplier<java.util.List<String>> contextLines;

	@Inject
	public DebugOverlay(LogBuffer logBuffer)
	{
		this(logBuffer, () -> java.util.List.of());
	}

	public DebugOverlay(LogBuffer logBuffer, java.util.function.Supplier<java.util.List<String>> contextLines)
	{
		this.logBuffer = logBuffer;
		this.contextLines = contextLines;
		setPosition(OverlayPosition.TOP_LEFT);
		setPriority(OverlayPriority.LOW);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		List<String> entries = new java.util.ArrayList<>(contextLines.get());
		entries.addAll(logBuffer.snapshot());
		if (entries.isEmpty())
		{
			return null;
		}

		int from = Math.max(0, entries.size() - MAX_LINES);
		List<String> lines = entries.subList(from, entries.size());

		graphics.setFont(FontManager.getRunescapeSmallFont());

		int width = 0;
		int y = PADDING;
		for (String line : lines)
		{
			TextComponent text = new TextComponent();
			text.setText(line);
			text.setPosition(new java.awt.Point(PADDING, y));
			text.render(graphics);

			width = Math.max(width, graphics.getFontMetrics().stringWidth(line));
			y += LINE_HEIGHT;
		}

		return new Dimension(width + PADDING * 2, y);
	}
}
