package com.coach.plugin.coaching;

import com.coach.plugin.encounter.model.CalloutDefinition;

/**
 * Resolves effective callout priority: explicit definition value wins,
 * otherwise a per-category default (critical highest).
 */
public class PriorityResolver
{
	static int categoryDefault(String category)
	{
		if (category == null)
		{
			return 50;
		}
		switch (category)
		{
			case "critical":   return 90;
			case "warning":    return 70;
			case "transition": return 40;
			case "info":
			default:           return 50;
		}
	}

	public int resolve(CalloutDefinition callout)
	{
		if (callout.priority != null)
		{
			return Math.max(1, Math.min(100, callout.priority));
		}
		return categoryDefault(callout.category);
	}
}
