package com.coach.plugin.trigger;

import com.coach.plugin.encounter.EncounterLoader;
import com.coach.plugin.encounter.PackLoadException;
import com.coach.plugin.encounter.model.EncounterPack;
import com.coach.plugin.events.EventType;
import com.coach.plugin.events.GameEvent;
import java.util.List;
import net.runelite.api.NPC;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompositeTriggerTest
{
	private final EncounterLoader loader = new EncounterLoader();

	@Test
	void compositeAndRequiresAllChildrenToMatchSameEvent()
	{
		// AND of two TICK-based triggers: fires only on ticks satisfying both
		String json = pack("{\"type\":\"composite\",\"logic\":\"AND\",\"children\":["
			+ "{\"type\":\"tick_timer\",\"tickMod\":4},"
			+ "{\"type\":\"tick_timer\",\"tickMod\":8}"
			+ "]}");
		TriggerRegistry registry = new TriggerRegistry(null);
		TriggerEngine engine = new TriggerEngine(registry);
		engine.rebuild(List.of(parse(json)));

		engine.onTickBatch(4, List.of(new GameEvent(EventType.TICK, 4, null)));
		assertTrue(engine.getLastFires().isEmpty(), "multiple of 4 but not 8");

		engine.onTickBatch(8, List.of(new GameEvent(EventType.TICK, 8, null)));
		assertEquals(1, engine.getLastFires().size(), "multiple of both -> AND fires");

		engine.onTickBatch(12, List.of(new GameEvent(EventType.TICK, 12, null)));
		assertTrue(engine.getLastFires().isEmpty(), "no refire while unsatisfied");
	}

	@Test
	void compositeOrMatchesAnyChild()
	{
		String json = pack("{\"type\":\"composite\",\"logic\":\"OR\",\"children\":["
			+ "{\"type\":\"animation\",\"npcId\":11278,\"animationId\":8960},"
			+ "{\"type\":\"projectile\",\"projectId\":2955}"
			+ "]}");
		TriggerEngine engine = new TriggerEngine(new TriggerRegistry(null));
		engine.rebuild(List.of(parse(json)));

		engine.onTickBatch(1, List.of(projectile(2955)));
		assertEquals(1, engine.getLastFires().size());

		engine.onTickBatch(2, List.of(animation(11278, 8960)));
		assertEquals(1, engine.getLastFires().size());

		engine.onTickBatch(3, List.of(animation(11278, 1111)));
		assertTrue(engine.getLastFires().isEmpty());
	}

	@Test
	void compositeWithInvalidChildIsSkipped()
	{
		String json = pack("{\"type\":\"composite\",\"logic\":\"AND\",\"children\":["
			+ "{\"type\":\"tick_timer\"}"  // missing tickMod
			+ "]}");
		TriggerEngine engine = new TriggerEngine(new TriggerRegistry(null));
		engine.rebuild(List.of(parse(json)));

		engine.onTickBatch(1, List.of(new GameEvent(EventType.TICK, 4, null)));
		assertTrue(engine.getLastFires().isEmpty());
	}

	private EncounterPack parse(String json)
	{
		try
		{
			return loader.parseJson(json, "test.zip");
		}
		catch (PackLoadException e)
		{
			throw new IllegalStateException(e);
		}
	}

	private static String pack(String mechanicTrigger)
	{
		return "{\"schemaVersion\": \"1.0\","
			+ "\"metadata\": {\"packId\": \"t\", \"name\": \"T\", \"version\": \"1.0.0\", \"gameVersion\": \"x\"},"
			+ "\"bosses\": [{\"bossId\": \"b\", \"name\": \"B\", \"npcId\": 11278,"
			+ "\"phases\": [{\"phaseId\": \"p\", \"name\": \"P\","
			+ "\"entryTrigger\": {\"type\": \"npc_spawn\", \"npcId\": 11278},"
			+ "\"mechanics\": [{\"mechanicId\": \"m\", \"name\": \"M\","
			+ "\"triggers\": [" + mechanicTrigger + "],"
			+ "\"callouts\": []}]}]}]}";
	}

	private static GameEvent animation(int npcId, int anim)
	{
		NPC npc = org.mockito.Mockito.mock(NPC.class);
		org.mockito.Mockito.when(npc.getId()).thenReturn(npcId);
		org.mockito.Mockito.when(npc.getAnimation()).thenReturn(anim);
		net.runelite.api.events.AnimationChanged changed =
			new net.runelite.api.events.AnimationChanged();
		changed.setActor(npc);
		return new GameEvent(EventType.ANIMATION_CHANGED, 100, changed);
	}

	private static GameEvent projectile(int id)
	{
		net.runelite.api.Projectile projectile = org.mockito.Mockito.mock(net.runelite.api.Projectile.class);
		org.mockito.Mockito.when(projectile.getId()).thenReturn(id);
		net.runelite.api.events.ProjectileMoved moved =
			new net.runelite.api.events.ProjectileMoved();
		moved.setProjectile(projectile);
		return new GameEvent(EventType.PROJECTILE_MOVED, 100, moved);
	}
}
