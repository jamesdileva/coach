package com.coach.plugin.encounter;

import com.coach.plugin.encounter.model.MechanicDefinition;
import com.coach.plugin.trigger.TriggerFire;
import java.util.List;

/**
 * Tracks mechanic activation eligibility: cooldown enforcement and
 * condition gating live here; callout emission happens in the Coaching
 * Engine (Sprint 8).
 */
public class MechanicManager
{
	/**
	 * Did any of this mechanic's triggers fire in the given fires?
	 */
	public boolean wasTriggered(String bossId, MechanicDefinition mechanic, List<TriggerFire> fires)
	{
		for (TriggerFire fire : fires)
		{
			if (!bossId.equals(fire.getBossId()))
			{
				continue;
			}
			String ctx = fire.getContextId();
			if (ctx.equals(mechanic.mechanicId) || ctx.startsWith(mechanic.mechanicId + "#"))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Attempt activation: respects the mechanic's cooldown window.
	 *
	 * @return true if the mechanic activated (and cooldown was applied)
	 */
	public boolean tryActivate(ActiveEncounter encounter, MechanicDefinition mechanic, int tick)
	{
		int cooldown = mechanic.cooldown != null ? mechanic.cooldown : 0;
		if (encounter.isOnCooldown(mechanic.mechanicId, tick))
		{
			return false;
		}
		encounter.applyCooldown(mechanic.mechanicId, cooldown, tick);
		return true;
	}
}
