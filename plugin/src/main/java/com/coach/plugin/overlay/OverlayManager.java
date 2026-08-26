package com.coach.plugin.overlay;

import com.coach.plugin.coaching.PredictedMechanic;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Holds the current visual callout state: active callout texts (with expiry
 * ticks) plus upcoming-mechanic predictions. CoachOverlay renders whatever
 * is in here each frame; all mutation happens on the client thread.
 */
public class OverlayManager
{
	public static final int DEFAULT_DURATION_TICKS = 3;
	public static final int MAX_VISUALS = 5;

	public static final class ActiveVisual
	{
		public final String bossId;
		public final String text;
		public final String category;
		public final String visualType; // prayer_icon | countdown | text | safe_tile | ...
		public final int expireTick;

		ActiveVisual(String bossId, String text, String category, String visualType, int expireTick)
		{
			this.bossId = bossId;
			this.text = text;
			this.category = category;
			this.visualType = visualType;
			this.expireTick = expireTick;
		}
	}

	private final List<ActiveVisual> visuals = new ArrayList<>();
	private volatile List<PredictedMechanic> predictions = List.of();
	private volatile int quietUntilTick = -1;

	// live state fed by the plugin each tick (all optional)
	private volatile Integer playerHpPercent = null;
	private volatile Double phaseProgress = null;      // 0..1, null when idle
	private volatile String currentBossLabel = null;
	private volatile String currentPhaseLabel = null;

	/**
	 * Quiet hours: after a critical callout, suppress non-critical visuals for
	 * a few ticks so the critical message isn't buried (§8.5).
	 */
	public static final int QUIET_TICKS_AFTER_CRITICAL = 4;

	public void noteCriticalDelivered(int tick)
	{
		quietUntilTick = tick + QUIET_TICKS_AFTER_CRITICAL;
	}

	public boolean isQuiet(int tick)
	{
		return tick < quietUntilTick;
	}

	public void setPlayerHpPercent(Integer percent)
	{
		this.playerHpPercent = percent;
	}

	public Integer getPlayerHpPercent()
	{
		return playerHpPercent;
	}

	public void setPhaseProgress(Double fraction)
	{
		this.phaseProgress = fraction;
	}

	public Double getPhaseProgress()
	{
		return phaseProgress;
	}

	public void setCurrentBossLabel(String label)
	{
		this.currentBossLabel = label;
	}

	public void setCurrentPhaseLabel(String label)
	{
		this.currentPhaseLabel = label;
	}

	public String getCurrentBossLabel()
	{
		return currentBossLabel;
	}

	public String getCurrentPhaseLabel()
	{
		return currentPhaseLabel;
	}

	/**
	 * Show a visual callout until tick + durationTicks (pack-defined or default).
	 * Non-critical visuals are suppressed during quiet hours (§8.5).
	 */
	public void addVisual(String bossId, com.coach.plugin.encounter.model.CalloutDefinition callout, int tick)
	{
		boolean critical = "critical".equals(callout.category);
		if (!critical && isQuiet(tick))
		{
			return;
		}
		int duration = DEFAULT_DURATION_TICKS;
		if (callout.visual != null && callout.visual.durationTicks != null)
		{
			duration = Math.max(1, callout.visual.durationTicks);
		}
		String visualType = callout.visual != null && callout.visual.type != null
			? callout.visual.type : "text";
		visuals.add(new ActiveVisual(bossId,
			callout.text != null ? callout.text : callout.calloutId,
			callout.category, visualType, tick + duration));

		while (visuals.size() > MAX_VISUALS)
		{
			visuals.remove(0); // drop oldest
		}
		prune(tick);
	}

	public void setPredictions(List<PredictedMechanic> predictions)
	{
		this.predictions = predictions != null ? List.copyOf(predictions) : List.of();
	}

	/**
	 * Drop expired visuals. Called per tick and defensively before render.
	 */
	public void prune(int tick)
	{
		Iterator<ActiveVisual> iterator = visuals.iterator();
		while (iterator.hasNext())
		{
			if (iterator.next().expireTick <= tick)
			{
				iterator.remove();
			}
		}
	}

	public List<ActiveVisual> getActiveVisuals()
	{
		return List.copyOf(visuals);
	}

	public List<PredictedMechanic> getPredictions()
	{
		return predictions;
	}
}
