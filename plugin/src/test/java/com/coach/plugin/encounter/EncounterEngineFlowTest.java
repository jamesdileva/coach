package com.coach.plugin.encounter;

import com.coach.plugin.encounter.model.EncounterPack;
import com.coach.plugin.events.EventType;
import com.coach.plugin.events.GameEvent;
import com.coach.plugin.trigger.TriggerFire;
import java.util.ArrayList;
import java.util.List;
import net.runelite.api.NPC;
import net.runelite.api.events.NpcDespawned;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Full-flow integration over real pack JSON:
 * entry trigger -> mechanic activation (cooldown + condition gated)
 * -> phase transition via exit -> recovery reset on despawn -> re-entry.
 */
class EncounterEngineFlowTest
{
	private static final String PACK_JSON = "{"
		+ "\"schemaVersion\": \"1.0\","
		+ "\"metadata\": {\"packId\": \"t\", \"name\": \"T\", \"version\": \"1.0.0\", \"gameVersion\": \"x\"},"
		+ "\"bosses\": [{\"bossId\": \"b\", \"name\": \"B\", \"npcId\": 11278,"
		+ "\"phases\": [{"
		+ "  \"phaseId\": \"p1\", \"name\": \"P1\","
		+ "  \"entryTrigger\": {\"type\": \"npc_spawn\", \"npcId\": 11278},"
		+ "  \"exitTriggers\": [{\"type\": \"hp\", \"npcId\": 11278, \"hpThreshold\": 50, \"hpDirection\": \"below\"}],"
		+ "  \"mechanics\": [{"
		+ "    \"mechanicId\": \"smash\", \"name\": \"Smash\","
		+ "    \"triggers\": ["
		+ "      {\"type\": \"animation\", \"npcId\": 11278, \"animationId\": 8960},"
		+ "      {\"type\": \"projectile\", \"projectId\": 2955}"
		+ "    ],"
		+ "    \"callouts\": [],"
		+ "    \"cooldown\": 5,"
		+ "    \"conditions\": [{\"type\": \"tick_mod\", \"mod\": 3}]"
		+ "  }]"
		+ "}, {"
		+ "  \"phaseId\": \"p2\", \"name\": \"P2\","
		+ "  \"entryTrigger\": {\"type\": \"npc_spawn\", \"npcId\": 11278},"
		+ "  \"mechanics\": []"
		+ "}]}]}";

	private EncounterEngine engine;
	private List<MechanicActivation> activations;

	@BeforeEach
	void setUp() throws Exception
	{
		engine = new EncounterEngine(null);
		activations = new ArrayList<>();
		engine.addActivationListener(activations::add);

		EncounterPack pack = new EncounterLoader().parseJson(PACK_JSON, "test.zip");
		java.lang.reflect.Field field = EncounterEngine.class.getDeclaredField("packs");
		field.setAccessible(true);
		field.set(engine, List.of(pack));
	}

	@Test
	void fullFlow()
	{
		// 1. entry: phase p1 entered on entry-context fire (globalTick=100)
		engine.onTriggersFired(List.of(fire(100, "phase:p1:entry")));
		assertEquals("p1", engine.getCurrentPhaseId(11278).orElse(null));
		assertTrue(activations.isEmpty());

		// advance the tick so phaseTick=1
		engine.onTickBatch(101, List.of(new GameEvent(EventType.TICK, 101, null)));

		// 2. mechanic fires at phaseTick=1 — not divisible by 3 -> condition blocks
		engine.onTriggersFired(List.of(fire(101, "smash#0")));
		assertEquals(0, activations.size(), "tick_mod condition unmet");

		// advance so phaseTick=3
		engine.onTickBatch(103, List.of(new GameEvent(EventType.TICK, 103, null)));

		// 3. mechanic fires at phaseTick=3 -> activation, cooldown applied until t108
		engine.onTriggersFired(List.of(fire(103, "smash#0")));
		assertEquals(1, activations.size());
		assertEquals("smash", activations.get(0).getMechanic().mechanicId);
		assertEquals("p1", activations.get(0).getPhaseId());

		// 4. second trigger within cooldown -> suppressed
		engine.onTriggersFired(List.of(fire(104, "smash#1")));
		assertEquals(1, activations.size());

		// advance to phaseTick=9 (t109): cooldown expired AND tick_mod satisfied
		engine.onTickBatch(109, List.of(new GameEvent(EventType.TICK, 109, null)));
		engine.onTriggersFired(List.of(fire(109, "smash#0")));
		assertEquals(2, activations.size());

		// 5. exit trigger -> transition to p2
		engine.onTriggersFired(List.of(fire(110, "phase:p1:exit0")));
		assertEquals("p2", engine.getCurrentPhaseId(11278).orElse(null));

		// 6. p1's mechanics no longer evaluated in p2
		engine.onTriggersFired(List.of(fire(120, "smash#0")));
		assertEquals(2, activations.size());

		// 7. boss despawn -> recovery resets session
		NPC bossNpc = mock(NPC.class);
		when(bossNpc.getId()).thenReturn(11278);
		engine.onTickBatch(130, List.of(
			new GameEvent(EventType.TICK, 130, null),
			new GameEvent(EventType.NPC_DESPAWNED, 130, new NpcDespawned(bossNpc))));
		assertFalse(engine.hasActiveSession(11278));

		// 8. re-entry works cleanly after reset
		engine.onTriggersFired(List.of(fire(140, "phase:p1:entry")));
		assertTrue(engine.hasActiveSession(11278));
		assertEquals("p1", engine.getCurrentPhaseId(11278).orElse(null));
	}

	private static TriggerFire fire(int tick, String contextId)
	{
		return new TriggerFire(tick, "b", contextId, "test");
	}
}
