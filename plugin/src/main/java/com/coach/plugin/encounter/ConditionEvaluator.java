package com.coach.plugin.encounter;

import com.coach.plugin.encounter.model.ConditionDefinition;
import com.coach.plugin.events.GameStateBridge;
import java.util.HashSet;
import java.util.Set;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Skill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Evaluates condition definitions from encounter JSON against live state.
 * Unknown condition types log a warning once and evaluate to false (fail-closed).
 */
public class ConditionEvaluator
{
	private static final Logger log = LoggerFactory.getLogger(ConditionEvaluator.class);

	private final Client client; // nullable in tests
	private final Set<String> warnedTypes = new HashSet<>();

	public ConditionEvaluator(Client client)
	{
		this.client = client;
	}

	/**
	 * @param phaseTick ticks elapsed in the current phase (for tick_mod)
	 */
	public boolean satisfies(ConditionDefinition condition, int phaseTick)
	{
		if (condition == null || condition.type == null)
		{
			return false;
		}
		switch (condition.type)
		{
			case "npc_hp_below":
			case "npc_hp_above":
			{
				NPC npc = findNpc(condition.npcId);
				if (npc == null)
				{
					return false;
				}
				int percent = healthPercent(npc);
				return condition.type.endsWith("below")
					? percent <= condition.threshold
					: percent >= condition.threshold;
			}
			case "player_hp_below":
			case "player_hp_above":
			{
				if (client == null)
				{
					return false;
				}
				int hp = client.getBoostedSkillLevel(Skill.HITPOINTS);
				return condition.type.endsWith("below")
					? hp <= condition.threshold
					: hp >= condition.threshold;
			}
			case "tick_mod":
				return phaseTick % Math.max(1, condition.mod) == 0;
			default:
				warnOnce(condition.type);
				return false;
		}
	}

	private void warnOnce(String type)
	{
		if (warnedTypes.add(type))
		{
			log.warn("[coach] condition type '{}' not supported yet — evaluating to false", type);
		}
	}

	private NPC findNpc(Integer npcId)
	{
		if (client == null || npcId == null)
		{
			return null;
		}
		return GameStateBridge.findNpc(client, npcId);
	}

	static int healthPercent(NPC npc)
	{
		int scale = npc.getHealthScale();
		int ratio = npc.getHealthRatio();
		if (scale <= 0)
		{
			return ratio;
		}
		return (int) Math.round(ratio * 100.0 / scale);
	}
}
