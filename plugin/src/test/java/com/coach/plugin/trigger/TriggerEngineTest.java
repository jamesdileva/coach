package com.coach.plugin.trigger;

import com.coach.plugin.encounter.EncounterLoader;
import com.coach.plugin.encounter.PackLoadException;
import com.coach.plugin.encounter.model.EncounterPack;
import com.coach.plugin.events.EventType;
import com.coach.plugin.events.GameEvent;
import java.util.List;
import net.runelite.api.NPC;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TriggerEngineTest
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

	private final EncounterLoader loader = new EncounterLoader();
	private TriggerEngine engine;

	@BeforeEach
	void setUp() throws PackLoadException
	{
		engine = new TriggerEngine(new TriggerRegistry(null));
		EncounterPack pack = loader.parseJson(PACK_JSON, "test.zip");
		engine.rebuild(List.of(pack));
	}

	@Test
	void firesOnMatchingAnimationEvent()
	{
		NPC nex = mock(NPC.class);
		when(nex.getId()).thenReturn(11278);
		when(nex.getAnimation()).thenReturn(8960);

		engine.onTickBatch(500, List.of(
			new GameEvent(EventType.TICK, 500, null),
			animationEvent(nex)));

		assertEquals(1, engine.getLastFires().size());
		TriggerFire fire = engine.getLastFires().get(0);
		assertEquals("nex", fire.getBossId());
		assertEquals("shadow_smash#0", fire.getContextId());
		assertEquals(500, fire.getTick());
	}

	@Test
	void doesNotFireForWrongNpcOrWrongTickEvents()
	{
		NPC other = mock(NPC.class);
		when(other.getId()).thenReturn(2097); // not Nex
		when(other.getAnimation()).thenReturn(8960);

		engine.onTickBatch(1, List.of(animationEvent(other)));
		assertTrue(engine.getLastFires().isEmpty());

		// unrelated event type entirely
		engine.onTickBatch(2, List.of(new GameEvent(EventType.VARBIT_CHANGED, 2, new Object())));
		assertTrue(engine.getLastFires().isEmpty());
	}

	@Test
	void oneFirePerEvaluatorPerEvent_noDuplicates()
	{
		NPC nex = mock(NPC.class);
		when(nex.getId()).thenReturn(11278);
		when(nex.getAnimation()).thenReturn(8960);
		GameEvent evt = animationEvent(nex);

		// same event object twice in a batch = still one fire (per-event evaluation)
		engine.onTickBatch(10, List.of(evt));
		int count = engine.getLastFires().size();

		engine.onTickBatch(10, List.of(evt, evt));
		// each event occurrence is evaluated separately: 2 events -> 2 fires
		assertEquals(count * 2, engine.getLastFires().size());
	}

	@Test
	void phaseEntryTriggersAreRegisteredToo()
	{
		// npc_spawn entry trigger for p1 — but npc_spawn has no evaluator until
		// Sprint 6; it should be skipped with a warning, not crash.
		assertTrue(engine.getLastFires().isEmpty());

		// rebuild must have logged skipped=1 for the unsupported npc_spawn trigger
		List<TriggerFire> fires = engine.getLastFires();
		assertTrue(fires.isEmpty());
	}

	@Test
	void rebuildClearsPreviousTriggers() throws PackLoadException
	{
		NPC nex = mock(NPC.class);
		when(nex.getId()).thenReturn(11278);
		when(nex.getAnimation()).thenReturn(8960);

		engine.rebuild(List.of(loader.parseJson(PACK_JSON, "test.zip")));
		engine.onTickBatch(1, List.of(animationEvent(nex)));
		assertEquals(1, engine.getLastFires().size());

		engine.rebuild(List.of()); // unload all packs
		engine.onTickBatch(2, List.of(animationEvent(nex)));
		assertTrue(engine.getLastFires().isEmpty());
	}

	@Test
	void fireListenersAreNotified()
	{
		final List<TriggerFire>[] received = new List[1];
		engine.addFireListener(fires -> received[0] = fires);

		NPC nex = mock(NPC.class);
		when(nex.getId()).thenReturn(11278);
		when(nex.getAnimation()).thenReturn(8960);

		engine.onTickBatch(77, List.of(animationEvent(nex)));

		assertEquals(engine.getLastFires(), received[0]);
		assertEquals(77, received[0].get(0).getTick());
	}

	private static GameEvent animationEvent(NPC npc)
	{
		net.runelite.api.events.AnimationChanged changed = new net.runelite.api.events.AnimationChanged();
		changed.setActor(npc);
		return new GameEvent(EventType.ANIMATION_CHANGED, 100, changed);
	}
}
