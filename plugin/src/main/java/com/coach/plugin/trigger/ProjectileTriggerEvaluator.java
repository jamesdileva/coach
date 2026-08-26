package com.coach.plugin.trigger;

import com.coach.plugin.events.EventType;
import com.coach.plugin.events.GameEvent;
import java.util.Set;
import net.runelite.api.Projectile;
import net.runelite.api.events.ProjectileMoved;

/**
 * Fires when a projectile with the configured id is seen moving
 * (ProjectileMoved also fires on spawn — the projectile's first movement).
 *
 * Note: source-NPC matching is deferred until Sprint 6+ where the origin tile
 * can be compared against live NPC positions via the client.
 */
public class ProjectileTriggerEvaluator implements TriggerEvaluator
{
	private final int projectileId;

	public ProjectileTriggerEvaluator(int projectileId)
	{
		this.projectileId = projectileId;
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
		return projectile.getId() == projectileId;
	}

	@Override
	public String describe()
	{
		return "projectile " + projectileId;
	}
}
