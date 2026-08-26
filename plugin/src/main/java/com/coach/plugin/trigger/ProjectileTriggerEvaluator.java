package com.coach.plugin.trigger;

import com.coach.plugin.events.EventType;
import com.coach.plugin.events.GameEvent;
import java.util.Set;
import net.runelite.api.Actor;
import net.runelite.api.NPC;
import net.runelite.api.Projectile;
import net.runelite.api.events.ProjectileMoved;

/**
 * Fires when a projectile with the configured id is seen moving
 * (ProjectileMoved also fires on spawn — the projectile's first movement).
 * Optional srcNpcId restricts to projectiles fired by a specific NPC.
 */
public class ProjectileTriggerEvaluator implements TriggerEvaluator
{
	private final int projectileId;
	private final Integer srcNpcId; // null = any source

	public ProjectileTriggerEvaluator(int projectileId, Integer srcNpcId)
	{
		this.projectileId = projectileId;
		this.srcNpcId = srcNpcId;
	}

	@Override
	public Set<EventType> interestedIn()
	{
		return Set.of(EventType.PROJECTILE_MOVED);
	}

	@Override
	public boolean matches(GameEvent event)
	{
		Object payload = event.getPayload();
		if (!(payload instanceof ProjectileMoved))
		{
			return false;
		}
		Projectile projectile = ((ProjectileMoved) payload).getProjectile();
		if (projectile.getId() != projectileId)
		{
			return false;
		}
		if (srcNpcId == null)
		{
			return true;
		}
		Actor source = projectile.getSourceActor();
		return source instanceof NPC && ((NPC) source).getId() == srcNpcId;
	}

	@Override
	public String describe()
	{
		return "projectile " + projectileId + (srcNpcId != null ? " from npc " + srcNpcId : "");
	}
}
