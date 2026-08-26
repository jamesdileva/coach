package com.coach.plugin;

import com.coach.plugin.config.CoachConfig;
import com.coach.plugin.events.EventBus;
import com.coach.plugin.events.EventType;
import com.coach.plugin.events.GameEvent;
import com.coach.plugin.events.GameStateBridge;
import com.google.inject.Provides;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GraphicChanged;
import net.runelite.api.events.GraphicsObjectCreated;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.ProjectileMoved;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PluginDescriptor(
	name = "Coach",
	description = "Real-time boss coaching with visual and audio callouts",
	tags = {"pvm", "bossing", "coaching", "overlay", "audio"}
)
public class CoachPlugin extends Plugin
{
	private static final Logger log = LoggerFactory.getLogger(CoachPlugin.class);

	@Inject
	private Client client;

	@Inject
	private CoachConfig config;

	/**
	 * RuneLite's event bus — fully qualified because our internal bus shares the name.
	 */
	@Inject
	private net.runelite.client.eventbus.EventBus runeLiteEventBus;

	private final EventBus coachEventBus = new EventBus();
	private final GameStateBridge gameStateBridge = new GameStateBridge();
	private final Map<EventType, Integer> tickEventCounts = new EnumMap<>(EventType.class);

	@Override
	protected void startUp() throws Exception
	{
		coachEventBus.subscribe(this::onTickBatch);
		runeLiteEventBus.register(this);
		log.info("Project Coach started (debug={})", config.debugMode());
	}

	@Override
	protected void shutDown() throws Exception
	{
		runeLiteEventBus.unregister(this);
		log.info("Project Coach shut down");
	}

	// ---- RuneLite event subscriptions ----

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		coachEventBus.post(new GameEvent(EventType.TICK, client.getTickCount(), null));
	}

	@Subscribe
	public void onAnimationChanged(AnimationChanged event)
	{
		post(EventType.ANIMATION_CHANGED, event);
	}

	@Subscribe
	public void onProjectileMoved(ProjectileMoved event)
	{
		post(EventType.PROJECTILE_MOVED, event);
	}

	@Subscribe
	public void onGraphicChanged(GraphicChanged event)
	{
		post(EventType.GRAPHIC_CHANGED, event);
	}

	@Subscribe
	public void onGraphicsObjectCreated(GraphicsObjectCreated event)
	{
		post(EventType.GRAPHICS_OBJECT_CREATED, event);
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		post(EventType.NPC_SPAWNED, event);
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		post(EventType.NPC_DESPAWNED, event);
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		post(EventType.PLAYER_STATS_CHANGED, event);
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		post(EventType.VARBIT_CHANGED, event);
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		post(EventType.ITEM_CONTAINER_CHANGED, event);
	}

	// ---- Internal bus ----

	EventBus getCoachEventBus()
	{
		return coachEventBus;
	}

	GameStateBridge getGameStateBridge()
	{
		return gameStateBridge;
	}

	private void post(EventType type, Object payload)
	{
		Integer count = tickEventCounts.get(type);
		tickEventCounts.put(type, count == null ? 1 : count + 1);
		coachEventBus.post(new GameEvent(type, client.getTickCount(), payload));
	}

	private void onTickBatch(int tick, List<GameEvent> events)
	{
		if (config.debugMode() && !tickEventCounts.isEmpty())
		{
			StringBuilder sb = new StringBuilder();
			for (Map.Entry<EventType, Integer> entry : tickEventCounts.entrySet())
			{
				if (sb.length() > 0)
				{
					sb.append(' ');
				}
				sb.append(entry.getKey()).append('=').append(entry.getValue());
			}
			log.info("[coach] tick {}: {} event(s): {}", tick, events.size() - 1, sb);
		}
		tickEventCounts.clear();
	}

	@Provides
	CoachConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CoachConfig.class);
	}
}
