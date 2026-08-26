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
		public final int expireTick;

		ActiveVisual(String bossId, String text, String category, int expireTick)
		{
			this.bossId = bossId;
			this.text = text;
			this.category = category;
			this.expireTick = expireTick;
		}
	}

	private final List<ActiveVisual> visuals = new ArrayList<>();
	private volatile List<PredictedMechanic> predictions = List.of();

	/**
	 * Show a visual callout until tick + durationTicks (pack-defined or default).
	 */
	public void addVisual(String bossId, com.coach.plugin.encounter.model.CalloutDefinition callout, int tick)
	{
		int duration = DEFAULT_DURATION_TICKS;
		if (callout.visual != null && callout.visual.durationTicks != null)
		{
			duration = Math.max(1, callout.visual.durationTicks);
		}
		visuals.add(new ActiveVisual(bossId,
			callout.text != null ? callout.text : callout.calloutId,
			callout.category, tick + duration));

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
