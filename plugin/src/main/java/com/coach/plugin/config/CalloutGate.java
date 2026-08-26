package com.coach.plugin.config;

import com.coach.plugin.encounter.model.CalloutDefinition;
import java.util.Set;
import java.util.function.BiPredicate;

/**
 * Live settings gate for the CoachingEngine, built from CoachConfig values.
 * Reads are cheap; the plugin re-creates this whenever it needs a fresh
 * predicate (config changes apply immediately — no restart).
 */
public class CalloutGate implements BiPredicate<String, CalloutDefinition>
{
	private final CoachConfig config;

	public CalloutGate(CoachConfig config)
	{
		this.config = config;
	}

	@Override
	public boolean test(String bossId, CalloutDefinition callout)
	{
		return CalloutFilter.isEnabled(
			config.enabled(),
			config.essentialOnly(),
			config.disabledBosses(),
			config.criticalCallouts(),
			config.warningCallouts(),
			config.infoCallouts(),
			config.transitionCallouts(),
			bossId,
			callout);
	}

	public Set<String> disabledBosses()
	{
		return CalloutFilter.disabledBosses(config.disabledBosses());
	}
}
