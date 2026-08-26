package com.coach.plugin.config;

import com.coach.plugin.encounter.model.CalloutDefinition;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Pure decision function for whether a callout may fire — the settings gate
 * between packs and the coaching engine (rule 2: player decides).
 *
 * Kept static and dependency-free so it's trivially testable; CoachPlugin
 * feeds it live config values on every activation.
 */
public final class CalloutFilter
{
	private CalloutFilter()
	{
	}

	public static boolean isEnabled(
		boolean pluginEnabled,
		boolean essentialOnly,
		String disabledBossesCsv,
		boolean critical, boolean warning, boolean info, boolean transition,
		String bossId, CalloutDefinition callout)
	{
		if (!pluginEnabled || callout == null)
		{
			return false;
		}
		if (essentialOnly && !"critical".equals(callout.category))
		{
			return false;
		}
		if (!categoryEnabled(callout.category, critical, warning, info, transition))
		{
			return false;
		}
		return !disabledBosses(disabledBossesCsv).contains(normalizeBossId(bossId));
	}

	static boolean categoryEnabled(String category,
		boolean critical, boolean warning, boolean info, boolean transition)
	{
		if (category == null)
		{
			return info; // uncategorised treated as informational
		}
		switch (category)
		{
			case "critical":   return critical;
			case "warning":    return warning;
			case "transition": return transition;
			default:           return info;
		}
	}

	static Set<String> disabledBosses(String csv)
	{
		if (csv == null || csv.trim().isEmpty())
		{
			return Set.of();
		}
		return new HashSet<>(Arrays.asList(csv.toLowerCase().split("\\s*,\\s*")));
	}

	private static String normalizeBossId(String bossId)
	{
		return bossId != null ? bossId.toLowerCase() : "";
	}
}
