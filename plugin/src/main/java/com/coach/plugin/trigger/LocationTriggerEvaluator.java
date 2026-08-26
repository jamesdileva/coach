package com.coach.plugin.trigger;

import com.coach.plugin.events.EventType;
import com.coach.plugin.events.GameEvent;
import java.util.Set;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;

/**
 * Fires when the local player ENTERS a rectangular world region
 * (inclusive bounds, edge-triggered — not continuously while inside).
 */
public class LocationTriggerEvaluator implements TriggerEvaluator
{
	private final Client client;
	private final int minX;
	private final int maxX;
	private final int minY;
	private final int maxY;
	private final EdgeDetector edge = new EdgeDetector();

	public LocationTriggerEvaluator(Client client, int minX, int maxX, int minY, int maxY)
	{
		this.client = client;
		this.minX = Math.min(minX, maxX);
		this.maxX = Math.max(minX, maxX);
		this.minY = Math.min(minY, maxY);
		this.maxY = Math.max(minY, maxY);
	}

	@Override
	public Set<EventType> interestedIn()
	{
		return Set.of(EventType.TICK);
	}

	@Override
	public boolean matches(GameEvent event)
	{
		Player player = client != null ? client.getLocalPlayer() : null;
		if (player == null)
		{
			edge.reset();
			return false;
		}
		WorldPoint pos = player.getWorldLocation();
		boolean inside = pos.getX() >= minX && pos.getX() <= maxX
			&& pos.getY() >= minY && pos.getY() <= maxY;
		return edge.onNext(inside);
	}

	@Override
	public String describe()
	{
		return "region x[" + minX + ".." + maxX + "] y[" + minY + ".." + maxY + "]";
	}
}
