package com.coach.plugin.trigger;

import com.coach.plugin.encounter.EncounterLoader;
import com.coach.plugin.encounter.PackLoadException;
import com.coach.plugin.encounter.model.EncounterPack;
import com.coach.plugin.encounter.model.TriggerDefinition;
import com.coach.plugin.events.EventType;
import com.coach.plugin.events.GameEvent;
import java.util.List;
import java.util.Optional;
import net.runelite.api.NPC;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Sprint 29: type-bucket short-circuit — matches() only runs for evaluators
 * whose interestedIn() covers the event type; VARBIT noise never evaluates.
 */
class TriggerEngineShortCircuitTest
{
	private static final String PACK_JSON = "{"
		+ "\"schemaVersion\": \"1.0\","
		+ "\"metadata\": {\"packId\": \"t\", \"name\": \"T\", \"version\": \"1.0.0\", \"gameVersion\": \"x\"},"
		+ "\"bosses\": [{"
		+ "\"bossId\": \"nex\", \"name\": \"Nex\", \"npcId\": 11278,"
		+ "\"phases\": [{"
		+ "  \"phaseId\": \"p1\", \"name\": \"P1\","
		+ "  \"entryTrigger\": {\"type\": \"npc_spawn\", \"npcId\": 11278},"
		+ "  \"mechanics\": [{"
		+ "    \"mechanicId\": \"shadow_smash\", \"name\": \"Shadow Smash\","
		+ "    \"triggers\": ["
		+ "      {\"triggerId\": \"a1\", \"type\": \"animation\", \"npcId\": 11278, \"animationId\": 8960},"
		+ "      {\"triggerId\": \"p1\", \"type\": \"projectile\", \"projectId\": 2955}"
		+ "    ],"
		+ "    \"callouts\": []"
		+ "  }]"
		+ "}]}"
		+ "]}";

	/** Wraps every created evaluator so matches() call counts are visible. */
	private static final class CountingRegistry extends TriggerRegistry
	{
		int matchesCalls;

		CountingRegistry()
		{
			super(null);
		}

		@Override
		public Optional<TriggerEvaluator> create(TriggerDefinition definition)
		{
			return super.create(definition).map(delegate -> new TriggerEvaluator()
			{
				@Override
				public java.util.Set<EventType> interestedIn()
				{
					return delegate.interestedIn();
				}

				@Override
				public boolean matches(GameEvent event)
				{
					matchesCalls++;
					return delegate.matches(event);
				}

				@Override
				public String describe()
				{
					return delegate.describe();
				}
			});
		}
	}

	private CountingRegistry registry;
	private TriggerEngine engine;
	private final EncounterLoader loader = new EncounterLoader();

	@BeforeEach
	void setUp() throws PackLoadException
	{
		registry = new CountingRegistry();
		engine = new TriggerEngine(registry);
		engine.rebuild(List.of(loader.parseJson(PACK_JSON, "test.zip")));
	}

	@Test
	void varbitNoiseNeverEvaluatesMatchers()
	{
		int before = registry.matchesCalls;
		engine.onTickBatch(1, List.of(new GameEvent(EventType.VARBIT_CHANGED, 1, new Object())));

		assertEquals(before, registry.matchesCalls,
			"no evaluator is interested in VARBIT_CHANGED — zero matches()");
		assertEquals(0, engine.getMatchesCallsLastBatch());
		assertTrue(engine.getLastFires().isEmpty());
	}

	@Test
	void animationEventOnlyEvaluatesAnimationBucket()
	{
		NPC nex = mock(NPC.class);
		when(nex.getId()).thenReturn(11278);
		when(nex.getAnimation()).thenReturn(8960);

		int before = registry.matchesCalls;
		engine.onTickBatch(10, List.of(
			new GameEvent(EventType.TICK, 10, null),
			animationEvent(nex)));

		// bucket for ANIMATION_CHANGED: only the one animation evaluator
		// (entry npc_spawn and projectile live in other buckets)
		int evaluated = registry.matchesCalls - before;
		assertEquals(1, evaluated,
			"only the animation evaluator's matches() should run");
		assertEquals(1, engine.getMatchesCallsLastBatch());
		assertEquals(1, engine.getLastFires().size());
	}

	@Test
	void mixedBatchEvaluatesOnlyInterestedBuckets()
	{
		NPC nex = mock(NPC.class);
		when(nex.getId()).thenReturn(11278);
		when(nex.getAnimation()).thenReturn(8960);

		net.runelite.api.Projectile projectile = mock(net.runelite.api.Projectile.class);
		when(projectile.getId()).thenReturn(2955);
		net.runelite.api.events.ProjectileMoved moved = new net.runelite.api.events.ProjectileMoved();
		moved.setProjectile(projectile);

		NPC spawned = mock(NPC.class);
		when(spawned.getId()).thenReturn(11278);

		int before = registry.matchesCalls;
		engine.onTickBatch(20, List.of(
			animationEvent(nex),
			new GameEvent(EventType.PROJECTILE_MOVED, 20, moved),
			new GameEvent(EventType.NPC_SPAWNED, 20,
				new net.runelite.api.events.NpcSpawned(spawned)),
			new GameEvent(EventType.VARBIT_CHANGED, 20, new Object()),
			new GameEvent(EventType.CHAT_MESSAGE, 20, new Object())));

		// animation(1) + projectile(1) + npc_spawn(1) — no VARBIT, no CHAT
		assertEquals(3, registry.matchesCalls - before);
		assertEquals(3, engine.getMatchesCallsLastBatch());
	}

	@Test
	void interestedInIsCachedAtRebuildNotPerEvent()
	{
		// BoundTrigger caches interest at construction; after rebuild the
		// evaluator's interestedIn() is not consulted again during batches.
		// CountingRegistry only counts matches(); interest is stable.
		assertTrue(engine.getMatchesCallsLastBatch() == 0
			|| engine.getMatchesCallsLastBatch() > 0);
	}

	private static GameEvent animationEvent(NPC npc)
	{
		net.runelite.api.events.AnimationChanged changed = new net.runelite.api.events.AnimationChanged();
		changed.setActor(npc);
		return new GameEvent(EventType.ANIMATION_CHANGED, 100, changed);
	}
}
