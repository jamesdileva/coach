package com.coach.plugin.performance;

import com.coach.plugin.coaching.CoachingEngine;
import com.coach.plugin.coaching.PredictionEngine;
import com.coach.plugin.encounter.EncounterEngine;
import com.coach.plugin.encounter.EncounterLoader;
import com.coach.plugin.encounter.PackLoadException;
import com.coach.plugin.encounter.model.EncounterPack;
import com.coach.plugin.events.EventBus;
import com.coach.plugin.events.EventType;
import com.coach.plugin.events.GameEvent;
import com.coach.plugin.overlay.OverlayManager;
import com.coach.plugin.trigger.TriggerEngine;
import com.coach.plugin.trigger.TriggerRegistry;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Headless tick-replay harness (Sprint 29 / 30b): wires the production
 * EventBus → TriggerEngine → EncounterEngine → CoachingEngine pipeline with
 * synthetic NPC/projectile/animation events, and measures wall time + memory
 * growth. No RuneLite client required; NPC and Projectile payloads are
 * JDK dynamic proxies.
 */
public class TickReplayHarness
{
	public static final int DEFAULT_BOSSED_NPC_ID = 11278;
	private static final int ANIMATION_ID = 8960;
	private static final int PROJECTILE_ID = 2955;

	/** Summary of one {@link #replay} run. */
	public static final class Result
	{
		public final int ticks;
		public final long memoryGrowthBytes;
		public final long totalFires;
		public final long totalActivations;
		public final long totalCallouts;
		public final long worstTickNanos;
		public final MapSnapshot maxComponentNanos;

		Result(int ticks, long memoryGrowthBytes, long totalFires,
			long totalActivations, long totalCallouts, long worstTickNanos,
			ComponentTotals components)
		{
			this.ticks = ticks;
			this.memoryGrowthBytes = memoryGrowthBytes;
			this.totalFires = totalFires;
			this.totalActivations = totalActivations;
			this.totalCallouts = totalCallouts;
			this.worstTickNanos = worstTickNanos;
			this.maxComponentNanos = new MapSnapshot(components);
		}

		public long getWorstTickMillis()
		{
			return worstTickNanos / 1_000_000L;
		}

		public boolean withinLooseBudgets()
		{
			return worstTickNanos < Profiler.TOTAL_BUDGET_MS * 1_000_000L;
		}

		public boolean anyComponentOverBudget()
		{
			return maxComponentNanos.anyOverBudget();
		}
	}

	private static final class ComponentTotals
	{
		final long[] worst = new long[Profiler.Component.values().length];

		void observe(Profiler.TickBreakdown breakdown)
		{
			for (Profiler.Component component : Profiler.Component.values())
			{
				long nanos = breakdown.getNanos(component);
				if (nanos > worst[component.ordinal()])
				{
					worst[component.ordinal()] = nanos;
				}
			}
		}
	}

	/** Read-only view of worst-case per-component nanos. */
	public static final class MapSnapshot
	{
		private final long[] nanos;

		MapSnapshot(ComponentTotals totals)
		{
			this.nanos = totals.worst.clone();
		}

		public long getNanos(Profiler.Component component)
		{
			return nanos[component.ordinal()];
		}

		boolean anyOverBudget()
		{
			for (Profiler.Component component : Profiler.Component.values())
			{
				if (nanos[component.ordinal()] > component.budgetNanos)
				{
					return true;
				}
			}
			return false;
		}
	}

	private final EventBus bus = new EventBus();
	private final TriggerEngine triggerEngine;
	private final EncounterEngine encounterEngine;
	private final CoachingEngine coachingEngine = new CoachingEngine();
	private final OverlayManager overlayManager = new OverlayManager();
	private final PredictionEngine predictionEngine = new PredictionEngine();
	private final Profiler profiler = new Profiler();
	private final MemoryMonitor memoryMonitor = new MemoryMonitor();

	private long fires;
	private long activations;
	private long callouts;

	public TickReplayHarness(List<EncounterPack> packs) throws PackLoadException
	{
		this(packs, new TriggerRegistry(null));
	}

	TickReplayHarness(List<EncounterPack> packs, TriggerRegistry registry)
	{
		this.triggerEngine = new TriggerEngine(registry);
		this.encounterEngine = new EncounterEngine(null);
		encounterEngine.setPacks(packs);
		encounterEngine.setProfiler(profiler);
		triggerEngine.setProfiler(profiler);
		bus.setProfiler(profiler);

		triggerEngine.addFireListener(fires -> {
			this.fires += fires.size();
			encounterEngine.onTriggersFired(fires);
		});
		encounterEngine.addActivationListener(activation -> {
			this.activations++;
			coachingEngine.onActivation(activation);
		});
		coachingEngine.addListener(delivery -> this.callouts++);

		bus.subscribe(triggerEngine);
		bus.subscribe(encounterEngine);
		bus.subscribe((tick, events) -> {
			profiler.start(Profiler.Component.COACHING);
			profiler.start(Profiler.Component.OVERLAY);
			coachingEngine.onTick(tick);
			overlayManager.prune(tick);
			overlayManager.setPredictions(
				predictionEngine.predict(encounterEngine.getActiveSessions(), tick));
			profiler.stop(Profiler.Component.OVERLAY);
			profiler.stop(Profiler.Component.COACHING);
		});

		triggerEngine.rebuild(packs);
		memoryMonitor.reset();
	}

	/** Parse one or more encounter.json files into packs. */
	public static List<EncounterPack> fromJson(String... jsons) throws PackLoadException
	{
		EncounterLoader loader = new EncounterLoader();
		List<EncounterPack> packs = new ArrayList<>(jsons.length);
		for (int i = 0; i < jsons.length; i++)
		{
			packs.add(loader.parseJson(jsons[i], "pack" + i + ".json"));
		}
		return packs;
	}

	/** Load every encounter.json directly under {@code directory} (non-recursive). */
	public static List<EncounterPack> fromDirectory(Path directory) throws IOException, PackLoadException
	{
		EncounterLoader loader = new EncounterLoader();
		List<EncounterPack> packs = new ArrayList<>();
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "encounter.json"))
		{
			for (Path file : stream)
			{
				packs.add(loader.parseJson(Files.readString(file),
					file.getParent().getFileName().toString()));
			}
		}
		return packs;
	}

	/**
	 * Replay {@code tickCount} synthetic ticks starting at tick 1.
	 *
	 * @param noiseEventsPerTick VARBIT_CHANGED noise events mixed in per tick
	 */
	public Result replay(int tickCount, int noiseEventsPerTick)
	{
		fires = 0;
		activations = 0;
		callouts = 0;
		ComponentTotals components = new ComponentTotals();
		long worstTick = 0L;
		memoryMonitor.reset();

		for (int tick = 1; tick <= tickCount; tick++)
		{
			long start = System.nanoTime();

			profiler.beginTick(tick);
			List<GameEvent> events = buildEvents(tick, noiseEventsPerTick);
			for (GameEvent event : events)
			{
				bus.post(event);
			}
			bus.post(new GameEvent(EventType.TICK, tick, null));
			Profiler.TickBreakdown breakdown = profiler.endTick();
			components.observe(breakdown);

			long elapsed = System.nanoTime() - start;
			if (elapsed > worstTick)
			{
				worstTick = elapsed;
			}

			if (tick % 50 == 0)
			{
				memoryMonitor.sample();
			}
		}
		memoryMonitor.sample();

		return new Result(tickCount, memoryMonitor.getGrowthBytes(),
			fires, activations, callouts, worstTick, components);
	}

	public Profiler getProfiler()
	{
		return profiler;
	}

	public MemoryMonitor getMemoryMonitor()
	{
		return memoryMonitor;
	}

	private static List<GameEvent> buildEvents(int tick, int noiseEventsPerTick)
	{
		List<GameEvent> events = new ArrayList<>(4 + Math.max(0, noiseEventsPerTick));
		if (tick == 1 || tick % 20 == 0)
		{
			events.add(new GameEvent(EventType.NPC_SPAWNED, tick,
				npcSpawnedPayload(DEFAULT_BOSSED_NPC_ID)));
		}
		if (tick % 3 == 0)
		{
			events.add(new GameEvent(EventType.ANIMATION_CHANGED, tick,
				animationPayload(DEFAULT_BOSSED_NPC_ID, ANIMATION_ID)));
		}
		if (tick % 7 == 0)
		{
			events.add(new GameEvent(EventType.PROJECTILE_MOVED, tick,
				projectilePayload(PROJECTILE_ID)));
		}
		for (int i = 0; i < noiseEventsPerTick; i++)
		{
			events.add(new GameEvent(EventType.VARBIT_CHANGED, tick, new Object()));
		}
		return events;
	}

	private static Object npcSpawnedPayload(int npcId)
	{
		return new net.runelite.api.events.NpcSpawned(
			(net.runelite.api.NPC) npc(npcId, -1));
	}

	private static Object animationPayload(int npcId, int animationId)
	{
		net.runelite.api.events.AnimationChanged changed =
			new net.runelite.api.events.AnimationChanged();
		changed.setActor((net.runelite.api.Actor) npc(npcId, animationId));
		return changed;
	}

	private static Object projectilePayload(int projectileId)
	{
		net.runelite.api.Projectile projectile =
			(net.runelite.api.Projectile) Proxy.newProxyInstance(
				TickReplayHarness.class.getClassLoader(),
				new Class<?>[]{net.runelite.api.Projectile.class},
				(inner, innerMethod, innerArgs) -> {
					if ("getId".equals(innerMethod.getName()))
					{
						return projectileId;
					}
					return defaultValue(innerMethod.getReturnType());
				});
		net.runelite.api.events.ProjectileMoved moved =
			new net.runelite.api.events.ProjectileMoved();
		moved.setProjectile(projectile);
		return moved;
	}

	/** Dynamic-proxy NPC implementing the subset trigger evaluators call. */
	static Object npc(int npcId, int animationId)
	{
		return Proxy.newProxyInstance(
			TickReplayHarness.class.getClassLoader(),
			new Class<?>[]{net.runelite.api.NPC.class},
			(proxy, method, args) -> {
				switch (method.getName())
				{
					case "getId":
						return npcId;
					case "getAnimation":
						return animationId;
					case "getName":
						return "synthetic-boss";
					default:
						return defaultValue(method.getReturnType());
				}
			});
	}

	private static Object defaultValue(Class<?> type)
	{
		if (!type.isPrimitive() || type == void.class)
		{
			return null;
		}
		if (type == boolean.class)
		{
			return Boolean.FALSE;
		}
		if (type == char.class)
		{
			return (char) 0;
		}
		if (type == byte.class || type == short.class || type == int.class || type == long.class)
		{
			return 0;
		}
		if (type == float.class || type == double.class)
		{
			return 0d;
		}
		return null;
	}
}
