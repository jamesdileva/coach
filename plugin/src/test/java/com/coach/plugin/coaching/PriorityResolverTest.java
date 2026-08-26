package com.coach.plugin.coaching;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PriorityResolverTest
{
	private final PriorityResolver resolver = new PriorityResolver();

	@Test
	void explicitPriorityHonoredAndClamped()
	{
		com.coach.plugin.encounter.model.CalloutDefinition callout =
			new com.coach.plugin.encounter.model.CalloutDefinition();
		callout.category = "info";
		callout.priority = 75;
		assertEquals(75, resolver.resolve(callout));

		callout.priority = 500;
		assertEquals(100, resolver.resolve(callout));
		callout.priority = 0;
		assertEquals(1, resolver.resolve(callout));
	}

	@Test
	void categoryDefaultsOrderCriticalHighest()
	{
		com.coach.plugin.encounter.model.CalloutDefinition critical =
			def("critical");
		com.coach.plugin.encounter.model.CalloutDefinition warning = def("warning");
		com.coach.plugin.encounter.model.CalloutDefinition info = def("info");
		com.coach.plugin.encounter.model.CalloutDefinition transition = def("transition");

		int c = resolver.resolve(critical);
		assertTrue(c > resolver.resolve(warning));
		assertTrue(resolver.resolve(warning) > resolver.resolve(info));
		assertTrue(resolver.resolve(info) > resolver.resolve(transition));
	}

	@Test
	void unknownCategoryFallsBackToInfo()
	{
		assertEquals(resolver.resolve(def("weird")), resolver.resolve(def("info")));
	}

	private static com.coach.plugin.encounter.model.CalloutDefinition def(String category)
	{
		com.coach.plugin.encounter.model.CalloutDefinition callout =
			new com.coach.plugin.encounter.model.CalloutDefinition();
		callout.calloutId = "c-" + category;
		callout.text = "x";
		callout.category = category;
		return callout;
	}
}
