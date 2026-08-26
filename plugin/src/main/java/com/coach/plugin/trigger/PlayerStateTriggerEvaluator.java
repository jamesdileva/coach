package com.coach.plugin.trigger;

import com.coach.plugin.events.EventType;
import com.coach.plugin.events.GameEvent;
import java.util.Set;
import net.runelite.api.Actor;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.StatChanged;

/**
 * Player-state trigger with two modes, chosen by which fields are present:
 * - animationId set: fires when the local player performs the animation
 * - hpThreshold/hpDirection set: fires when player HP crosses the threshold
 *   (edge-detected; evaluated on StatChanged for HITPOINTS)
 */
public class PlayerStateTriggerEvaluator implements TriggerEvaluator
{
	private final Integer animationId;
	private final Integer hpThreshold;
	private final boolean below;
	private final EdgeDetector hpEdge = new EdgeDetector();

	public PlayerStateTriggerEvaluator(Integer animationId, Integer hpThreshold, boolean below)
	{
		this.animationId = animationId;
		this.hpThreshold = hpThreshold;
		this.below = below;
	}

	@Override
	public Set<EventType> interestedIn()
	{
		return animationId != null
			? Set.of(EventType.ANIMATION_CHANGED)
			: Set.of(EventType.PLAYER_STATS_CHANGED);
	}

	@Override
	public boolean matches(GameEvent event)
	{
		if (animationId != null)
		{
			Object payload = event.getPayload();
			if (!(payload instanceof AnimationChanged))
			{
				return false;
			}
			Actor actor = ((AnimationChanged) payload).getActor();
			return actor instanceof Player && actor.getAnimation() == animationId;
		}

		Object payload = event.getPayload();
		if (!(payload instanceof StatChanged))
		{
			return false;
		}
		StatChanged stat = (StatChanged) payload;
		if (stat.getSkill() != Skill.HITPOINTS || hpThreshold == null)
		{
			return false;
		}
		boolean satisfied = below
			? stat.getBoostedLevel() <= hpThreshold
			: stat.getBoostedLevel() >= hpThreshold;
		return hpEdge.onNext(satisfied);
	}

	@Override
	public String describe()
	{
		if (animationId != null)
		{
			return "player animation " + animationId;
		}
		return "player hp " + (below ? "<=" : ">=") + hpThreshold;
	}
}
