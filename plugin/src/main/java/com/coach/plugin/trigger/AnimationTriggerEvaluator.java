package com.coach.plugin.trigger;

import com.coach.plugin.events.EventType;
import com.coach.plugin.events.GameEvent;
import java.util.Set;
import net.runelite.api.Actor;
import net.runelite.api.NPC;
import net.runelite.api.events.AnimationChanged;

/**
 * Fires when an NPC plays a specific animation.
 * npcId == null matches any NPC. Player animations are not matched here
 * (that will be a player_state trigger).
 */
public class AnimationTriggerEvaluator implements TriggerEvaluator
{
	private final Integer npcId; // null = any NPC
	private final int animationId;

	public AnimationTriggerEvaluator(Integer npcId, int animationId)
	{
		this.npcId = npcId;
		this.animationId = animationId;
	}

	@Override
	public Set<EventType> interestedIn()
	{
		return Set.of(EventType.ANIMATION_CHANGED);
	}

	@Override
	public boolean matches(GameEvent event)
	{
		Object payload = event.getPayload();
		if (!(payload instanceof AnimationChanged))
		{
			return false;
		}
		Actor actor = ((AnimationChanged) payload).getActor();
		if (!(actor instanceof NPC))
		{
			return false;
		}
		NPC npc = (NPC) actor;
		if (npc.getAnimation() != animationId)
		{
			return false;
		}
		return npcId == null || npc.getId() == npcId;
	}

	@Override
	public String describe()
	{
		return "animation " + animationId + (npcId != null ? " from npc " + npcId : "");
	}
}
