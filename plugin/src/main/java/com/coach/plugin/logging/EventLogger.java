package com.coach.plugin.logging;

import com.coach.plugin.events.EventBus;
import com.coach.plugin.events.EventType;
import com.coach.plugin.events.GameEvent;
import java.util.List;
import net.runelite.api.Actor;
import net.runelite.api.GraphicsObject;
import net.runelite.api.NPC;
import net.runelite.api.Projectile;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GraphicChanged;
import net.runelite.api.events.GraphicsObjectCreated;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.ProjectileMoved;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.VarbitChanged;

/**
 * Logs every game event received from the internal EventBus,
 * with tick number, event type and a short payload summary.
 */
public class EventLogger implements EventBus.Listener
{
	private final LogBuffer sink;

	public EventLogger(LogBuffer sink)
	{
		this.sink = sink;
	}

	@Override
	public void onTickBatch(int tick, List<GameEvent> events)
	{
		for (GameEvent event : events)
		{
			sink.log(format(event));
		}
	}

	String format(GameEvent event)
	{
		return "t" + event.getTick() + " " + event.getType() + " " + summarize(event);
	}

	static String summarize(GameEvent event)
	{
		Object payload = event.getPayload();
		if (payload == null)
		{
			return "";
		}
		try
		{
			switch (event.getType())
			{
				case ANIMATION_CHANGED:
				{
					Actor actor = ((AnimationChanged) payload).getActor();
					return actorDesc(actor) + " anim=" + actor.getAnimation();
				}
				case GRAPHIC_CHANGED:
				{
					Actor actor = ((GraphicChanged) payload).getActor();
					return actorDesc(actor) + " graphic=" + actor.getGraphic();
				}
				case GRAPHICS_OBJECT_CREATED:
				{
					GraphicsObject go = ((GraphicsObjectCreated) payload).getGraphicsObject();
					return "id=" + go.getId();
				}
				case PROJECTILE_MOVED:
				{
					Projectile p = ((ProjectileMoved) payload).getProjectile();
					return "projectId=" + p.getId();
				}
				case NPC_SPAWNED:
				case NPC_DESPAWNED:
				{
					NPC npc = event.getType() == EventType.NPC_SPAWNED
						? ((NpcSpawned) payload).getNpc()
						: ((NpcDespawned) payload).getNpc();
					return "npcId=" + npc.getId() + " name=" + npc.getName();
				}
				case PLAYER_STATS_CHANGED:
				{
					StatChanged stat = (StatChanged) payload;
					return stat.getSkill() + "=" + stat.getBoostedLevel() + "/" + stat.getLevel();
				}
				case VARBIT_CHANGED:
				{
					VarbitChanged varbit = (VarbitChanged) payload;
					return "varbit=" + varbit.getVarbitId() + " value=" + varbit.getValue();
				}
				case ITEM_CONTAINER_CHANGED:
				{
					ItemContainerChanged container = (ItemContainerChanged) payload;
					return "container=" + container.getContainerId();
				}
				default:
					return payload.getClass().getSimpleName();
			}
		}
		catch (Exception e)
		{
			// Summaries are best-effort; never let formatting break logging.
			return payload.getClass().getSimpleName();
		}
	}

	private static String actorDesc(Actor actor)
	{
		if (actor == null)
		{
			return "actor=null";
		}
		return actor.getName() != null ? actor.getName() : "actor@" + Integer.toHexString(System.identityHashCode(actor));
	}
}
