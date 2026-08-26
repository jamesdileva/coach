package com.coach.plugin.encounter.model;

import java.util.List;

/**
 * A phase definition from encounter JSON (schema v1). Populated by Gson.
 */
public class PhaseDefinition
{
	public String phaseId;
	public String name;
	public TriggerDefinition entryTrigger;
	public List<TriggerDefinition> exitTriggers;
	public List<MechanicDefinition> mechanics;

	// runtime context
	public transient String bossId;
}
