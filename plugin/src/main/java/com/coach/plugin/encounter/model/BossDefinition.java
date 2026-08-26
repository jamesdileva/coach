package com.coach.plugin.encounter.model;

import java.util.List;

/**
 * A boss definition from encounter JSON (schema v1). Populated by Gson.
 */
public class BossDefinition
{
	public String bossId;
	public String name;
	public Integer npcId;
	public String description;
	public List<PhaseDefinition> phases;
	public List<MechanicDefinition> mechanics; // shared mechanics, optional
}
