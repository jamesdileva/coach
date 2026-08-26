package com.coach.plugin.encounter;

import com.coach.plugin.encounter.model.ConditionDefinition;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Skill;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConditionEvaluatorTest
{
	@Test
	void tickModChecksPhaseTick()
	{
		ConditionEvaluator evaluator = new ConditionEvaluator(null);
		ConditionDefinition condition = new ConditionDefinition();
		condition.type = "tick_mod";
		condition.mod = 4;

		assertTrue(evaluator.satisfies(condition, 8));
		assertFalse(evaluator.satisfies(condition, 9));
	}

	@Test
	void playerHpConditionsNeedClient()
	{
		Client client = mock(Client.class);
		when(client.getBoostedSkillLevel(Skill.HITPOINTS)).thenReturn(28);

		ConditionEvaluator evaluator = new ConditionEvaluator(client);
		ConditionDefinition below = new ConditionDefinition();
		below.type = "player_hp_below";
		below.threshold = 30;

		ConditionDefinition above = new ConditionDefinition();
		above.type = "player_hp_above";
		above.threshold = 50;

		assertTrue(evaluator.satisfies(below, 0));
		assertFalse(evaluator.satisfies(above, 0));
	}

	@Test
	void npcHpConditionsResolveViaClient()
	{
		NPC boss = mock(NPC.class);
		when(boss.getId()).thenReturn(11278);
		when(boss.getHealthRatio()).thenReturn(40);
		when(boss.getHealthScale()).thenReturn(100);

		Client client = mock(Client.class);
		net.runelite.api.WorldView worldView = mock(net.runelite.api.WorldView.class);
		net.runelite.api.IndexedObjectSet<NPC> set = mock(net.runelite.api.IndexedObjectSet.class);
		when(set.iterator()).thenReturn((java.util.Iterator<NPC>) java.util.List.of(boss).iterator());
		org.mockito.Mockito.doReturn(set).when(worldView).npcs();
		when(client.getTopLevelWorldView()).thenReturn(worldView);

		ConditionEvaluator evaluator = new ConditionEvaluator(client);
		ConditionDefinition below = new ConditionDefinition();
		below.type = "npc_hp_below";
		below.npcId = 11278;
		below.threshold = 50;

		assertTrue(evaluator.satisfies(below, 0));
	}

	@Test
	void unknownConditionTypeFailsClosed()
	{
		ConditionEvaluator evaluator = new ConditionEvaluator(null);
		ConditionDefinition unknown = new ConditionDefinition();
		unknown.type = "moon_is_full";

		assertFalse(evaluator.satisfies(unknown, 0));
	}
}
