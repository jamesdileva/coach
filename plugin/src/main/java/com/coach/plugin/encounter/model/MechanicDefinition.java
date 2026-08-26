package com.coach.plugin.encounter.model;

import java.util.List;

/**
 * A mechanic definition from encounter JSON (schema v1). Populated by Gson.
 */
public class MechanicDefinition
{
	public String mechanicId;
	public String name;
	public List<TriggerDefinition> triggers;
	public List<CalloutDefinition> callouts;
	public Integer cooldown;
	public Boolean interruptible;

	// runtime context
	public transient String bossId;
}
