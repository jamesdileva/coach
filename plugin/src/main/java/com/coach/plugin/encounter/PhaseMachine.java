package com.coach.plugin.encounter;

import com.coach.plugin.encounter.model.BossDefinition;
import com.coach.plugin.encounter.model.PhaseDefinition;
import com.coach.plugin.trigger.TriggerFire;
import java.util.List;
import java.util.Optional;

/**
 * Pure phase-transition logic for one boss definition.
 *
 * Semantics: phases are entered via their entryTrigger; while a phase is
 * active, any matching exitTrigger advances to the NEXT phase in the list.
 * The last phase is terminal (its exit triggers end the encounter).
 * Cross-phase transition graphs arrive later if a boss needs them.
 */
public class PhaseMachine
{
	/**
	 * If no session exists yet, find the first phase whose entry trigger fired.
	 */
	public Optional<String> enterPhase(BossDefinition boss, List<TriggerFire> fires)
	{
		for (PhaseDefinition phase : boss.phases)
		{
			if (phase.entryTrigger != null && contextFired(fires, "phase:" + phase.phaseId + ":entry"))
			{
				return Optional.of(phase.phaseId);
			}
		}
		return Optional.empty();
	}

	/**
	 * If an exit trigger of the current phase fired, return the next phase id.
	 * Empty optional when the phase continues or the encounter completes.
	 */
	public Optional<String> advanceIfExit(ActiveEncounter encounter, List<TriggerFire> fires)
	{
		BossDefinition boss = encounter.getBoss();
		int index = indexOfPhase(boss, encounter.getCurrentPhaseId());
		if (index < 0 || index >= boss.phases.size() - 1)
		{
			return Optional.empty(); // unknown or terminal phase
		}
		String exitContext = "phase:" + encounter.getCurrentPhaseId() + ":exit";
		for (TriggerFire fire : fires)
		{
			if (boss.bossId.equals(fire.getBossId()) && fire.getContextId().startsWith(exitContext))
			{
				return Optional.of(boss.phases.get(index + 1).phaseId);
			}
		}
		return Optional.empty();
	}

	public boolean isFinalPhase(BossDefinition boss, String phaseId)
	{
		int index = indexOfPhase(boss, phaseId);
		return index == boss.phases.size() - 1;
	}

	static boolean contextFired(List<TriggerFire> fires, String contextId)
	{
		for (TriggerFire fire : fires)
		{
			if (fire.getContextId().equals(contextId))
			{
				return true;
			}
		}
		return false;
	}

	private static int indexOfPhase(BossDefinition boss, String phaseId)
	{
		for (int i = 0; i < boss.phases.size(); i++)
		{
			if (boss.phases.get(i).phaseId.equals(phaseId))
			{
				return i;
			}
		}
		return -1;
	}
}
