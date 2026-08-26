package com.coach.plugin.encounter;

import com.coach.plugin.trigger.TriggerFire;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhaseMachineTest
{
	private final PhaseMachine machine = new PhaseMachine();

	private TriggerFire entry(String bossId, String phaseId, int tick)
	{
		return new TriggerFire(tick, bossId, "phase:" + phaseId + ":entry", "entry");
	}

	private TriggerFire exit(String bossId, String phaseId, int index, int tick)
	{
		return new TriggerFire(tick, bossId, "phase:" + phaseId + ":exit" + index, "exit");
	}

	// Boss fixture mirrors a 3-phase pack; only context strings matter here.

	@Test
	void entryFiresOnMatchingEntryTrigger()
	{
		assertTrue(machine.enterPhase(Bosses.threePhase(), List.of(exit("b", "p1", 0, 99))).isEmpty(),
			"no entry fire -> no entry");
		assertEquals("p2", machine.enterPhase(Bosses.threePhase(),
			List.of(entry("b", "p2", 100))).get(),
			"first matching entry wins");
	}

	@Test
	void advanceRequiresMatchingExitContext()
	{
		ActiveEncounter enc = new ActiveEncounter(Bosses.threePhase(), 11278, "p1", 100);

		assertFalse(machine.advanceIfExit(enc, List.of(entry("b", "p1", 101))).isPresent());
		assertTrue(machine.advanceIfExit(enc, List.of(exit("b", "p1", 0, 102))).isPresent());
		assertEquals("p2", machine.advanceIfExit(enc, List.of(exit("b", "p1", 0, 102))).get());

		enc.setCurrentPhaseId("p2", 102);
		assertEquals("p3", machine.advanceIfExit(enc, List.of(exit("b", "p2", 0, 150))).get());
	}

	@Test
	void terminalPhaseNeverAdvances()
	{
		ActiveEncounter enc = new ActiveEncounter(Bosses.threePhase(), 11278, "p3", 200);
		assertFalse(machine.advanceIfExit(enc, List.of(exit("b", "p3", 0, 201))).isPresent());
		assertTrue(machine.isFinalPhase(Bosses.threePhase(), "p3"));
		assertFalse(machine.isFinalPhase(Bosses.threePhase(), "p1"));
	}

	@Test
	void firesForOtherBossesAreIgnored()
	{
		ActiveEncounter enc = new ActiveEncounter(Bosses.threePhase(), 11278, "p1", 100);
		assertFalse(machine.advanceIfExit(enc, List.of(exit("otherBoss", "p1", 0, 101))).isPresent());
	}
}
