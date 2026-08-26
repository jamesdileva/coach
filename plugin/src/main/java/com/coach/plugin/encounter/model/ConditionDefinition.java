package com.coach.plugin.encounter.model;

/**
 * A condition definition from encounter JSON (schema v1). Populated by Gson.
 * Conditions gate mechanic activation and are evaluated by ConditionEvaluator.
 */
public class ConditionDefinition
{
	public String type;
	public Integer npcId;   // for npc_hp_* conditions
	public Integer threshold;
	public Integer mod;
	public String prayer;
	public Integer itemId;
	public String expression;

	// player_in_region bounds
	public Integer minX;
	public Integer maxX;
	public Integer minY;
	public Integer maxY;
}
