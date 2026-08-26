package com.coach.plugin.trigger;

import com.coach.plugin.events.EventType;
import com.coach.plugin.events.GameEvent;
import java.util.Set;
import net.runelite.api.Actor;
import net.runelite.api.NPC;
import net.runelite.api.events.GraphicChanged;
import net.runelite.api.events.GraphicsObjectCreated;

/**
 * Fires when a specific graphic appears:
 * - on an NPC via GraphicChanged (spotanim on the actor), or
 * - as an AoE/tile effect via GraphicsObjectCreated.
 *
 * Both manifestations are common boss-mechanic signals, so one 'graphic'
 * trigger type covers both. npcId filters only the actor case.
 */
public class GraphicTriggerEvaluator implements TriggerEvaluator
{
	private final Integer npcId; // null = any NPC / any graphics object
	private final int graphicId;

	public GraphicTriggerEvaluator(Integer npcId, int graphicId)
	{
		this.npcId = npcId;
		this.graphicId = graphicId;
	}

	@Override
	public Set<EventType> interestedIn()
	{
		return Set.of(EventType.GRAPHIC_CHANGED, EventType.GRAPHICS_OBJECT_CREATED);
	}

	@Override
	public boolean matches(GameEvent event)
	{
		Object payload = event.getPayload();
		if (payload instanceof GraphicsObjectCreated)
		{
			return ((GraphicsObjectCreated) payload).getGraphicsObject().getId() == graphicId;
		}
		if (payload instanceof GraphicChanged)
		{
			Actor actor = ((GraphicChanged) payload).getActor();
			if (actor == null || actor.getGraphic() != graphicId)
			{
				return false;
			}
			if (!(actor instanceof NPC))
			{
				return false;
			}
			NPC npc = (NPC) actor;
			return npcId == null || npc.getId() == npcId;
		}
		return false;
	}

	@Override
	public String describe()
	{
		return "graphic " + graphicId + (npcId != null ? " on npc " + npcId : "");
	}
}
