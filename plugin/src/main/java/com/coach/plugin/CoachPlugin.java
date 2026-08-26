package com.coach.plugin;

import com.coach.plugin.audio.AudioEngine;
import com.coach.plugin.config.CalloutGate;
import com.coach.plugin.coaching.CoachStateManager;
import com.coach.plugin.coaching.CoachingEngine;
import com.coach.plugin.config.CoachConfig;
import com.coach.plugin.debug.DebugOverlayV2;
import com.coach.plugin.debug.EventTimeline;
import com.coach.plugin.debug.LogExporter;
import com.coach.plugin.debug.StateInspector;
import com.coach.plugin.debug.TriggerHistory;
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
import com.coach.plugin.overlay.CoachOverlay;
import com.coach.plugin.trigger.TriggerEngine;
import com.coach.plugin.trigger.TriggerFire;
import com.coach.plugin.trigger.TriggerRegistry;
import com.google.inject.Provides;
import java.util.List;
import java.nio.file.Paths;
import java.nio.file.Path;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.ChatMessage;
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

	private EventBus coachEventBus;
	private final GameStateBridge gameStateBridge = new GameStateBridge();
	private final EventLogger eventLogger = new EventLogger(logBuffer);
	private final TriggerLogger triggerLogger = new TriggerLogger(logBuffer);
	private final CalloutLogger calloutLogger = new CalloutLogger(logBuffer);
	private EncounterEngine encounterEngine;
	private TriggerEngine triggerEngine;
	private final CoachStateManager coachStateManager = new CoachStateManager();
	private CoachingEngine coachingEngine;
	private final com.coach.plugin.overlay.OverlayManager coachOverlayManager = new com.coach.plugin.overlay.OverlayManager();
	private final AudioEngine audioEngine = new AudioEngine();
	private final com.coach.plugin.coaching.PredictionEngine predictionEngine =
		new com.coach.plugin.coaching.PredictionEngine();
	private CoachOverlay coachOverlay;
	private boolean coachOverlayAdded;

	private DebugOverlayV2 debugOverlayV2;
	private boolean debugOverlayAdded;
	private EventBus.Listener debugListener;
	private com.coach.plugin.accessibility.AccessibilityManager accessibilityManager;

	// debug tool cores (Sprint 21)
	private final TriggerHistory triggerHistory = new TriggerHistory();
	private final EventTimeline eventTimeline = new EventTimeline();
	private final StateInspector stateInspector = new StateInspector();
	private final java.util.Map<EventType, Integer> pendingEventCounts =
		new java.util.EnumMap<>(EventType.class);
	private int triggersThisTick;
	private int calloutsThisTick;

	@Override
	protected void startUp() throws Exception
	{
		runeLiteEventBus.register(this);
		coachEventBus = new EventBus();
		accessibilityManager = new com.coach.plugin.accessibility.AccessibilityManager(config);
		encounterEngine = new EncounterEngine(client);
		coachingEngine = new CoachingEngine();
		triggerEngine = new TriggerEngine(new TriggerRegistry(client));
		triggerEngine.addFireListener(this::onTriggersFired);
		coachEventBus.subscribe(triggerEngine);
		coachEventBus.subscribe(encounterEngine);
		coachEventBus.subscribe(this::onCoachingTick);
		encounterEngine.addActivationListener(coachingEngine::onActivation);
		coachingEngine.setEnabledFilter(new CalloutGate(config));
		coachingEngine.addListener(this::onCalloutDelivered);
		registerCoachOverlay();
		applyAudioAccessibility();
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
		unregisterCoachOverlay();
		audioEngine.clear();
		runeLiteEventBus.unregister(this);
		// internal bus is rebuilt on next startUp — no stale listener leaks
		triggerEngine = null;
		coachingEngine = null;
		encounterEngine = null;
		log.info("Project Coach shut down");
	}

	// ---- RuneLite event subscriptions ----

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		if (coachEventBus != null)
		{
			coachEventBus.post(new GameEvent(EventType.TICK, client.getTickCount(), null));
		}
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

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		post(EventType.CHAT_MESSAGE, event);
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
			return;
		}
		if ("muted".equals(event.getKey()) || "accessibilityMode".equals(event.getKey()))
		{
			applyAudioAccessibility();
		}
		else if ("masterVolume".equals(event.getKey()))
		{
			audioEngine.setMasterVolume(config.masterVolume());
		}
		else if ("criticalVolume".equals(event.getKey()))
		{
			audioEngine.setCategoryVolume("critical", config.criticalVolume());
		}
		else if ("warningVolume".equals(event.getKey()))
		{
			audioEngine.setCategoryVolume("warning", config.warningVolume());
		}
		else if ("infoVolume".equals(event.getKey()))
		{
			audioEngine.setCategoryVolume("info", config.infoVolume());
		}
	}

	private void applyAudioAccessibility()
	{
		// visual-only mode silences everything; muted works independently
		audioEngine.setMuted(config.muted()
			|| accessibilityManager.getMode()
				== com.coach.plugin.accessibility.AccessibilityManager.Mode.VISUAL_ONLY);
	}

	// ---- Internal bus ----

	EncounterEngine getEncounterEngine()
	{
		return encounterEngine;
	}

	private void onCoachingTick(int tick, List<GameEvent> events)
	{
		// runs after trigger + encounter engines (subscription order)
		coachStateManager.update(gameStateBridge, client, tick);
		coachingEngine.onTick(tick);
		coachOverlayManager.prune(tick);
		updateOverlayState(tick);
		if (encounterEngine != null)
		{
			coachOverlayManager.setPredictions(
				predictionEngine.predict(encounterEngine.getActiveSessions(), tick));
		}
		if (config.debugMode())
		{
			stateInspector.update(
				coachStateManager.getPlayer(),
				encounterEngine != null ? encounterEngine.getActiveSessions() : List.of());
			if (!pendingEventCounts.isEmpty())
			{
				java.util.Map<String, Integer> counts = new java.util.LinkedHashMap<>();
				pendingEventCounts.forEach((type, count) -> counts.put(type.name(), count));
				eventTimeline.recordTick(tick, counts, triggersThisTick, calloutsThisTick);
			}
			triggersThisTick = 0;
			calloutsThisTick = 0;
			pendingEventCounts.clear();
		}
	}

	private void updateOverlayState(int tick)
	{
		if (config.showStatus() && client != null && gameStateBridge != null)
		{
			try
			{
				com.coach.plugin.model.PlayerState player =
					gameStateBridge.getPlayerState(client);
				int max = Math.max(1, player.getMaxHp());
				coachOverlayManager.setPlayerHpPercent(player.getHp() * 100 / max);
			}
			catch (Exception ignored)
			{
				// overlay state is best-effort
			}
		}
		if (encounterEngine != null)
		{
			var session = encounterEngine.getActiveSessions().stream().findFirst();
			session.ifPresentOrElse(s -> {
				var boss = s.getBoss();
				int index = 0;
				for (int i = 0; i < boss.phases.size(); i++)
				{
					if (boss.phases.get(i).phaseId.equals(s.getCurrentPhaseId()))
					{
						index = i;
						break;
					}
				}
				coachOverlayManager.setCurrentBossLabel(boss.name);
				coachOverlayManager.setCurrentPhaseLabel(
					boss.phases.get(index).name + " (" + (index + 1) + "/" + boss.phases.size() + ")");
				coachOverlayManager.setPhaseProgress(
					boss.phases.size() > 1 ? index / (double) (boss.phases.size() - 1) : 1.0);
			}, () -> {
				coachOverlayManager.setCurrentBossLabel(null);
				coachOverlayManager.setCurrentPhaseLabel(null);
				coachOverlayManager.setPhaseProgress(null);
			});
		}
	}

	private void onCalloutDelivered(com.coach.plugin.coaching.CoachingEngine.DeliveredCallout delivery)
	{
		com.coach.plugin.encounter.model.CalloutDefinition callout = delivery.getCallout();
		if ("critical".equals(callout.category))
		{
			coachOverlayManager.noteCriticalDelivered(delivery.getTick());
		}
		coachOverlayManager.addVisual(delivery.getBossId(), callout, delivery.getTick());

		String packId = encounterEngine != null
			? encounterEngine.getPackIdForBoss(delivery.getBossId()).orElse(null)
			: null;
		if (callout.audioFile != null && packId != null)
		{
			boolean handled = audioEngine.play(packId, callout.audioFile, callout.category);
			log.debug("[coach] audio {}: {}", callout.audioFile,
				handled ? "playing/queued" : "unavailable");
		}

		if (config.debugMode())
		{
			calloutLogger.calloutDelivered(delivery.getTick(),
				callout.calloutId, delivery.toString());
			calloutsThisTick++;
			eventTimeline.recordTick(delivery.getTick(), null, 0, 1);
		}
	}

	private void reloadPacks(String reason)
	{
		if (encounterEngine == null)
		{
			return;
		}
		audioEngine.clear();
		Path dir = Paths.get(config.packDirectory());
		int count = encounterEngine.loadPacks(dir);
		if (triggerEngine != null)
		{
			triggerEngine.rebuild(encounterEngine.getPacks());
		}
		for (com.coach.plugin.encounter.model.EncounterPack pack : encounterEngine.getPacks())
		{
			if (pack.sourceName != null && pack.metadata != null)
			{
				audioEngine.loadFromZip(dir.resolve(pack.sourceName), pack.metadata.packId);
			}
		}
		log.info("[coach] packs reloaded ({}): {} pack(s) loaded, {} audio file(s)",
			reason, count, audioEngine.getLoadedCount());
	}

	private void registerCoachOverlay()
	{
		if (coachOverlayAdded)
		{
			return;
		}
		try
		{
			coachOverlay = new CoachOverlay(coachOverlayManager, config, accessibilityManager);
			overlayManager.add(coachOverlay);
			coachOverlayAdded = true;
		}
		catch (IllegalStateException e)
		{
			log.warn("[coach] could not register coach overlay: {}", e.getMessage());
		}
	}

	private void unregisterCoachOverlay()
	{
		if (coachOverlayAdded && coachOverlay != null)
		{
			overlayManager.remove(coachOverlay);
			coachOverlay = null;
			coachOverlayAdded = false;
		}
	}

	private void onTriggersFired(List<TriggerFire> fires)
	{
		if (config.debugMode())
		{
			for (TriggerFire fire : fires)
			{
				triggerLogger.triggerFired(fire.getTick(), fire.getContextId(), fire.getDescription());
				triggerHistory.record(fire);
			}
			triggersThisTick += fires.size();
			if (!fires.isEmpty())
			{
				eventTimeline.recordTick(fires.get(0).getTick(), null, fires.size(), 0);
			}
		}
		if (encounterEngine != null)
		{
			encounterEngine.onTriggersFired(fires);
		}
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
		if (coachEventBus != null)
		{
			if (config.debugMode())
			{
				pendingEventCounts.merge(type, 1, Integer::sum);
			}
			coachEventBus.post(new GameEvent(type, client.getTickCount(), payload));
		}
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
			debugOverlayV2 = new DebugOverlayV2(logBuffer, triggerHistory,
				eventTimeline, stateInspector, config);
			overlayManager.add(debugOverlayV2);
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
		exportDebugBundle();
		logBuffer.setFileWriter(null);

		if (debugOverlayAdded && debugOverlayV2 != null)
		{
			overlayManager.remove(debugOverlayV2);
			debugOverlayV2 = null;
			debugOverlayAdded = false;
		}
		logBuffer.clear();
		triggerHistory.clear();
		eventTimeline.clear();
	}

	/**
	 * Auto-export a debug bundle whenever debug mode is switched off, so the
	 * data from the session is preserved for analysis (roadmap Sprint 21).
	 */
	private void exportDebugBundle()
	{
		try
		{
			if (triggerHistory.size() == 0 && eventTimeline.format(1).get(0).startsWith("(timeline"))
			{
				return; // nothing collected this session
			}
			new LogExporter(RuneLite.RUNELITE_DIR.toPath()
				.resolve("coach").resolve("debug_logs"))
				.export(stateInspector, triggerHistory, eventTimeline, logBuffer);
		}
		catch (Exception e)
		{
			log.warn("[coach] debug export skipped: {}", e.getMessage());
		}
	}

	@Provides
	CoachConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CoachConfig.class);
	}
}

