package com.coach.plugin.encounter.model;

import java.util.List;

/**
 * A trigger definition from encounter JSON (schema v1).
 * Type-specific fields are nullable; which fields matter depends on {@link #type}.
 */
public class TriggerDefinition
{
	public String triggerId;
	public String type;

	// type-specific fields
	public Integer npcId;
	public List<Integer> npcIds;    // multi-NPC form (spawn sets, wave_cleared)
	public Integer animationId;
	public Integer projectId;
	public Integer srcNpcId;      // projectile: optional source NPC filter
	public Integer graphicId;
	public Integer hpThreshold;
	public String hpDirection;   // "below" | "above"
	public Integer tickMod;
	public Integer tickOffset;
	public String containsText;  // shout: chat message substring (case-insensitive)
	public String senderName;    // shout: optional sender filter
	public List<TriggerDefinition> children; // composite only
	public String logic;                        // composite: "AND" | "OR"

	// location trigger region (inclusive bounds)
	public Integer minX;
	public Integer maxX;
	public Integer minY;
	public Integer maxY;

	// runtime: set by the loader to the owning mechanic/phase for diagnostics
	public transient String contextId;
}
