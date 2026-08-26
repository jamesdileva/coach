package com.coach.plugin.encounter;

import com.coach.plugin.encounter.model.MechanicDefinition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MechanicManagerTest
{
	private final MechanicManager manager = new MechanicManager();

	private static MechanicDefinition mechanic(String id, int cooldown)
	{
		MechanicDefinition mechanic = new MechanicDefinition();
		mechanic.mechanicId = id;
		mechanic.name = id;
		mechanic.cooldown = cooldown > 0 ? cooldown : null;
		return mechanic;
	}

	@Test
	void wasTriggeredMatchesExactAndIndexedContexts()
	{
		MechanicDefinition m = mechanic("smash", 0);

		assertTrue(manager.wasTriggered("b", m, java.util.List.of(
			new com.coach.plugin.trigger.TriggerFire(1, "b", "smash", "d"))));
		assertTrue(manager.wasTriggered("b", m, java.util.List.of(
			new com.coach.plugin.trigger.TriggerFire(1, "b", "smash#1", "d"))));
		assertFalse(manager.wasTriggered("b", m, java.util.List.of(
			new com.coach.plugin.trigger.TriggerFire(1, "b", "other", "d"))));
		assertFalse(manager.wasTriggered("otherBoss", m, java.util.List.of(
			new com.coach.plugin.trigger.TriggerFire(1, "b", "smash", "d"))));
	}

	@Test
	void cooldownBlocksUntilExpiry()
	{
		ActiveEncounter enc = new ActiveEncounter(Bosses.threePhase(), 11278, "p1", 100);
		MechanicDefinition m = mechanic("smash", 5);

		assertTrue(manager.tryActivate(enc, m, 100), "first activation");
		assertFalse(manager.tryActivate(enc, m, 101), "within cooldown");
		assertFalse(manager.tryActivate(enc, m, 104), "still within cooldown");
		assertTrue(manager.tryActivate(enc, m, 105), "cooldown expired");
	}

	@Test
	void zeroCooldownAlwaysAllows()
	{
		ActiveEncounter enc = new ActiveEncounter(Bosses.threePhase(), 11278, "p1", 100);
		MechanicDefinition m = mechanic("spam", 0);

		assertTrue(manager.tryActivate(enc, m, 100));
		assertEquals(3, countActivations(enc, m, 101, 3), "no cooldown -> every attempt fires");
	}

	private int countActivations(ActiveEncounter enc, MechanicDefinition m, int startTick, int attempts)
	{
		int count = 0;
		for (int i = 0; i < attempts; i++)
		{
			if (manager.tryActivate(enc, m, startTick + i))
			{
				count++;
			}
		}
		return count;
	}
}
