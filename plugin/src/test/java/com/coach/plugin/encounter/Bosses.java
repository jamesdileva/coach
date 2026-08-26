package com.coach.plugin.encounter;

import com.coach.plugin.encounter.model.BossDefinition;
import com.coach.plugin.encounter.model.PhaseDefinition;
import com.coach.plugin.encounter.model.TriggerDefinition;
import java.util.List;

/**
 * Shared boss fixtures for encounter engine tests.
 */
public final class Bosses
{
	private Bosses()
	{
	}

	public static BossDefinition threePhase()
	{
		BossDefinition boss = new BossDefinition();
		boss.bossId = "b";
		boss.name = "Test Boss";
		boss.npcId = 11278;
		boss.phases = List.of(phase("p1"), phase("p2"), phase("p3"));
		return boss;
	}

	static PhaseDefinition phase(String id)
	{
		PhaseDefinition phase = new PhaseDefinition();
		phase.phaseId = id;
		phase.name = id;
		phase.entryTrigger = new TriggerDefinition();
		return phase;
	}
}
