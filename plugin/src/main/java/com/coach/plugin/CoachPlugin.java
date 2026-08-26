package com.coach.plugin;

import com.coach.plugin.config.CoachConfig;
import com.coach.plugin.encounter.EncounterEngine;
import com.coach.plugin.events.EventBus;
import com.coach.plugin.events.EventType;
import com.coach.plugin.events.GameEvent;
import com.coach.plugin.events.GameStateBridge;
import com.coach.plugin.logging.CalloutLogger;
import com.coach.plugin.logging.EventLogger;
import com.coach.plugin.logging.FileLogWriter;
import com.coach.plugin.logging.LogBuffer;
import com.coach.plugin.logging.TriggerLogger;
import com.coach.plugin.overlay.DebugOverlay;
import com.google.inject.Provides;
import java.nio.file.Path;
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
import net.runelite.client.RuneLite;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
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

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private LogBuffer logBuffer;

	/**
	 * RuneLite's event bus — fully qualified because our internal bus shares the name.
	 */
	@Inject
	private net.runelite.client.eventbus.EventBus runeLiteEventBus;

	private final EventBus coachEventBus = new EventBus();
	private final GameStateBridge gameStateBridge = new GameStateBridge();
	private final EventLogger eventLogger = new EventLogger(logBuffer);
	private final TriggerLogger triggerLogger = new TriggerLogger(logBuffer);
	private final CalloutLogger calloutLogger = new CalloutLogger(logBuffer);
	private EncounterEngine encounterEngine;

	private DebugOverlay debugOverlay;
	private boolean debugOverlayAdded;
	private EventBus.Listener debugListener;

	@Override
	protected void startUp() throws Exception
	{
		runeLiteEventBus.register(this);
		encounterEngine = new EncounterEngine();
		reloadPacks("startup");
		log.info("Project Coach started (debug={})", config.debugMode());

		if (config.debugMode())
		{
			enableDebugging();
		}
	}

	@Override
	protected void shutDown() throws Exception
	{
		disableDebugging();
		runeLiteEventBus.unregister(this);
		encounterEngine = null;
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

	// ---- Config changes ----

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!"coach".equals(event.getGroup()))
		{
			return;
		}
		if ("packDirectory".equals(event.getKey()))
		{
			reloadPacks("config change");
			return;
		}
		if ("debugMode".equals(event.getKey()) || "logToFile".equals(event.getKey()))
		{
			if (config.debugMode())
			{
				enableDebugging();
			}
			else
			{
				disableDebugging();
			}
		}
	}

	// ---- Internal bus ----

	EncounterEngine getEncounterEngine()
	{
		return encounterEngine;
	}

	private void reloadPacks(String reason)
	{
		if (encounterEngine == null)
		{
			return;
		}
		int count = encounterEngine.loadPacks(java.nio.file.Paths.get(config.packDirectory()));
		log.info("[coach] packs reloaded ({}): {} pack(s) loaded", reason, count);
	}

	EventBus getCoachEventBus()
	{
		return coachEventBus;
	}

	GameStateBridge getGameStateBridge()
	{
		return gameStateBridge;
	}

	TriggerLogger getTriggerLogger()
	{
		return triggerLogger;
	}

	CalloutLogger getCalloutLogger()
	{
		return calloutLogger;
	}

	private void post(EventType type, Object payload)
	{
		coachEventBus.post(new GameEvent(type, client.getTickCount(), payload));
	}

	private synchronized void enableDebugging()
	{
		if (debugListener == null)
		{
			debugListener = eventLogger;
			coachEventBus.subscribe(debugListener);
		}

		if (config.logToFile())
		{
			Path logFile = RuneLite.RUNELITE_DIR.toPath().resolve("coach").resolve("logs").resolve("coach-debug.log");
			logBuffer.setFileWriter(new FileLogWriter(logFile));
		}

		if (!debugOverlayAdded)
		{
			debugOverlay = new DebugOverlay(logBuffer);
			overlayManager.add(debugOverlay);
			debugOverlayAdded = true;
		}
	}

	private synchronized void disableDebugging()
	{
		if (debugListener != null)
		{
			coachEventBus.unsubscribe(debugListener);
			debugListener = null;
		}
		logBuffer.setFileWriter(null);

		if (debugOverlayAdded && debugOverlay != null)
		{
			overlayManager.remove(debugOverlay);
			debugOverlay = null;
			debugOverlayAdded = false;
		}
		logBuffer.clear();
	}

	@Provides
	CoachConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CoachConfig.class);
	}
}
