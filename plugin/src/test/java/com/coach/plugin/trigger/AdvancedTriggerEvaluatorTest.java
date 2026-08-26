package com.coach.plugin.trigger;

import com.coach.plugin.events.EventType;
import com.coach.plugin.events.GameEvent;
import java.util.Iterator;
import java.util.List;
import net.runelite.api.Client;
import net.runelite.api.IndexedObjectSet;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.StatChanged;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdvancedTriggerEvaluatorTest
{
	private static GameEvent tick(int n)
	{
		return new GameEvent(EventType.TICK, n, null);
	}

	// ---- npc_spawn / npc_despawn ----

	@Test
	void npcSpawnMatchesSpecificId()
	{
		NPC nex = mock(NPC.class);
		when(nex.getId()).thenReturn(11278);

		NpcSpawnTriggerEvaluator evaluator = new NpcSpawnTriggerEvaluator(11278, true);

		assertTrue(evaluator.matches(new GameEvent(EventType.NPC_SPAWNED, 1, new NpcSpawned(nex))));
		assertFalse(evaluator.matches(new GameEvent(EventType.NPC_DESPAWNED, 1, new NpcDespawned(nex))));
		assertFalse(new NpcSpawnTriggerEvaluator(999, true)
			.matches(new GameEvent(EventType.NPC_SPAWNED, 1, new NpcSpawned(nex))));
	}

	@Test
	void npcDespawnMatches()
	{
		NPC nex = mock(NPC.class);
		when(nex.getId()).thenReturn(11278);

		NpcSpawnTriggerEvaluator evaluator = new NpcSpawnTriggerEvaluator(11278, false);

		assertTrue(evaluator.matches(new GameEvent(EventType.NPC_DESPAWNED, 1, new NpcDespawned(nex))));
	}

	// ---- tick_timer ----

	@Test
	void tickTimerFiresOnCorrectBoundaries()
	{
		TickTimerTriggerEvaluator every4 = new TickTimerTriggerEvaluator(4, 0);

		assertFalse(every4.matches(tick(1)));
		assertFalse(every4.matches(tick(3)));
		assertTrue(every4.matches(tick(4)));
		assertTrue(every4.matches(tick(8)));

		TickTimerTriggerEvaluator offset = new TickTimerTriggerEvaluator(4, 2);
		assertTrue(offset.matches(tick(2)));
		assertTrue(offset.matches(tick(6)));
		assertFalse(offset.matches(tick(4)));
		assertFalse(offset.matches(tick(1))); // negative delta never fires
	}

	// ---- hp (NPC health via client) ----

	private static Client clientWithNpcs(List<NPC> npcs)
	{
		Client client = mock(Client.class);
		WorldView worldView = mock(WorldView.class);
		when(worldView.npcs()).thenAnswer(inv -> {
			IndexedObjectSet<NPC> set = mock(IndexedObjectSet.class);
			when(set.iterator()).thenReturn((Iterator<NPC>) List.copyOf(npcs).iterator());
			return set;
		});
		when(client.getTopLevelWorldView()).thenReturn(worldView);
		return client;
	}

	@Test
	void hpTriggerFiresOnceAtCrossingNotContinuously()
	{
		NPC boss = mock(NPC.class);
		when(boss.getId()).thenReturn(11278);
		when(boss.getHealthRatio()).thenReturn(90, 50, 40, 45);
		when(boss.getHealthScale()).thenReturn(100);

		HpTriggerEvaluator evaluator = new HpTriggerEvaluator(
			clientWithNpcs(List.of(boss)), 11278, true, 50);

		assertFalse(evaluator.matches(tick(1)), "first observation never fires");
		assertTrue(evaluator.matches(tick(2)), "crossing to 50% fires");
		assertFalse(evaluator.matches(tick(3)), "still below does not refire");
		assertFalse(evaluator.matches(tick(4)), "recovering to 45% is not a crossing");
	}

	@Test
	void hpTriggerAboveDirectionAndMissingNpcReArms()
	{
		NPC boss = mock(NPC.class);
		when(boss.getId()).thenReturn(11278);
		when(boss.getHealthRatio()).thenReturn(10, 80, 90);
		when(boss.getHealthScale()).thenReturn(100);

		HpTriggerEvaluator evaluator = new HpTriggerEvaluator(
			clientWithNpcs(List.of(boss)), 11278, false, 75);

		assertFalse(evaluator.matches(tick(1)));
		assertTrue(evaluator.matches(tick(2)), "rising above 75% fires");

		HpTriggerEvaluator reArm = new HpTriggerEvaluator(
			clientWithNpcs(java.util.List.of()), 11278, true, 50);
		assertFalse(reArm.matches(tick(1)));
	}

	@Test
	void hpPercentComputesFromRatioScale()
	{
		NPC boss = mock(NPC.class);
		when(boss.getHealthRatio()).thenReturn(37);
		when(boss.getHealthScale()).thenReturn(60);

		assertEquals(62, HpTriggerEvaluator.healthPercent(boss));
	}

	// ---- player_state ----

	@Test
	void playerStateAnimationMatchesOnlyPlayers()
	{
		Player player = mock(Player.class);
		when(player.getAnimation()).thenReturn(827);
		net.runelite.api.events.AnimationChanged changed =
			new net.runelite.api.events.AnimationChanged();
		changed.setActor(player);

		PlayerStateTriggerEvaluator evaluator = new PlayerStateTriggerEvaluator(827, null, true);

		assertTrue(evaluator.matches(new GameEvent(EventType.ANIMATION_CHANGED, 1, changed)));

		NPC npc = mock(NPC.class);
		when(npc.getAnimation()).thenReturn(827);
		net.runelite.api.events.AnimationChanged npcAnim =
			new net.runelite.api.events.AnimationChanged();
		npcAnim.setActor(npc);
		assertFalse(evaluator.matches(new GameEvent(EventType.ANIMATION_CHANGED, 2, npcAnim)));
	}

	@Test
	void playerStateHpBelowCrossesWithEdge()
	{
		PlayerStateTriggerEvaluator evaluator = new PlayerStateTriggerEvaluator(null, 30, true);

		assertFalse(evaluator.matches(playerStat(Skill.HITPOINTS, 72)));
		assertTrue(evaluator.matches(playerStat(Skill.HITPOINTS, 29)));
		assertFalse(evaluator.matches(playerStat(Skill.HITPOINTS, 15)), "no refire while still low");
		assertFalse(evaluator.matches(playerStat(Skill.HITPOINTS, 99)), "recovery is not a fire");
	}

	private static GameEvent playerStat(Skill skill, int boosted)
	{
		return new GameEvent(EventType.PLAYER_STATS_CHANGED, 1, new StatChanged(skill, 0, boosted, boosted));
	}

	// ---- location ----

	@Test
	void locationFiresOnRegionEntryOnly()
	{
		Player player = mock(Player.class);
		Client client = mock(Client.class);
		when(client.getLocalPlayer()).thenReturn(player);
		when(player.getWorldLocation())
			.thenReturn(new WorldPoint(3200, 3200, 0))   // outside
			.thenReturn(new WorldPoint(3210, 3210, 0))   // inside -> fire
			.thenReturn(new WorldPoint(3215, 3215, 0));  // still inside -> no

		LocationTriggerEvaluator evaluator = new LocationTriggerEvaluator(client, 3205, 3220, 3205, 3220);

		assertFalse(evaluator.matches(tick(1)));
		assertTrue(evaluator.matches(tick(2)));
		assertFalse(evaluator.matches(tick(3)));
	}
}
