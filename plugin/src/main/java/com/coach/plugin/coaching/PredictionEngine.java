package com.coach.plugin.coaching;

import com.coach.plugin.encounter.ActiveEncounter;
import com.coach.plugin.encounter.model.MechanicDefinition;
import com.coach.plugin.encounter.model.TriggerDefinition;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Looks ahead in active encounter timelines and predicts upcoming mechanics.
 *
 * Only tick_timer-driven mechanics are predictable (their next fire tick is
 * computable). Event-driven mechanics fire when the game fires them — there is
 * nothing honest to predict, so they are omitted rather than guessed.
 */
public class PredictionEngine
{
	public static final int HORIZON_TICKS = 10;

	public List<PredictedMechanic> predict(List<ActiveEncounter> sessions, int currentTick)
	{
		List<PredictedMechanic> predictions = new ArrayList<>();
		if (sessions == null)
		{
			return predictions;
		}
		for (ActiveEncounter session : sessions)
		{
			com.coach.plugin.encounter.model.PhaseDefinition phase =
				findPhase(session.getBoss(), session.getCurrentPhaseId());
			if (phase == null || phase.mechanics == null)
			{
				continue;
			}
			int phaseTick = session.getPhaseTick();
			for (MechanicDefinition mechanic : phase.mechanics)
			{
				Integer eta = predictMechanic(mechanic, phaseTick);
				if (eta != null && eta > 0 && eta <= HORIZON_TICKS)
				{
					predictions.add(new PredictedMechanic(
						session.getBoss().bossId, mechanic.mechanicId, eta));
				}
			}
		}
		predictions.sort(Comparator.comparingInt(PredictedMechanic::getTicksUntilFire));
		return predictions;
	}

	/**
	 * Ticks until this mechanic's earliest tick_timer trigger fires, or null
	 * if it has no predictable trigger.
	 */
	static Integer predictMechanic(MechanicDefinition mechanic, int phaseTick)
	{
		if (mechanic.triggers == null)
		{
			return null;
		}
		Integer best = null;
		for (TriggerDefinition trigger : mechanic.triggers)
		{
			if (!"tick_timer".equals(trigger.type) || trigger.tickMod == null)
			{
				continue;
			}
			int mod = Math.max(1, trigger.tickMod);
			int offset = trigger.tickOffset != null ? trigger.tickOffset : 0;
			int delta = phaseTick - offset;
			int eta;
			if (delta < 0)
			{
				eta = -delta;
			}
			else
			{
				eta = (mod - (delta % mod)) % mod;
				if (eta == 0)
				{
					eta = mod; // next occurrence, not the current one
				}
			}
			if (best == null || eta < best)
			{
				best = eta;
			}
		}
		return best;
	}

	private static com.coach.plugin.encounter.model.PhaseDefinition findPhase(
		com.coach.plugin.encounter.model.BossDefinition boss, String phaseId)
	{
		for (com.coach.plugin.encounter.model.PhaseDefinition phase : boss.phases)
		{
			if (phase.phaseId.equals(phaseId))
			{
				return phase;
			}
		}
		return null;
	}
}
