package com.coach.plugin.config;

import com.coach.plugin.encounter.model.CalloutDefinition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CalloutFilterTest
{
	private static CalloutDefinition callout(String category)
	{
		CalloutDefinition callout = new CalloutDefinition();
		callout.calloutId = "c1";
		callout.text = "x";
		callout.category = category;
		return callout;
	}

	private static boolean test(String category, String disabledCsv)
	{
		return CalloutFilter.isEnabled(
			true, false, disabledCsv,
			true, true, true, true,
			"nex", callout(category));
	}

	@Test
	void allCategoriesPassWhenTogglesOn()
	{
		assertTrue(test("critical", ""));
		assertTrue(test("warning", ""));
		assertTrue(test("info", ""));
		assertTrue(test("transition", ""));
	}

	@Test
	void essentialOnlyPassesCriticalOnly()
	{
		var critical = callout("critical");
		var warning = callout("warning");

		assertTrue(CalloutFilter.isEnabled(true, true, "", true, true, true, true, "nex", critical));
		assertFalse(CalloutFilter.isEnabled(true, true, "", true, true, true, true, "nex", warning));
		assertFalse(CalloutFilter.isEnabled(true, true, "", true, true, true, true, "nex", callout("info")));
	}

	@Test
	void categoryToggleBlocksItsCategoryOnly()
	{
		assertFalse(CalloutFilter.isEnabled(true, false, "", false, true, true, true, "nex", callout("critical")));
		assertTrue(CalloutFilter.isEnabled(true, false, "", false, true, true, true, "nex", callout("warning")));

		assertTrue(CalloutFilter.isEnabled(true, false, "", true, false, true, true, "nex", callout("critical")));
		assertFalse(CalloutFilter.isEnabled(true, false, "", true, false, true, true, "nex", callout("warning")));
	}

	@Test
	void disablingBossBlocksAllCalloutsForThatBoss()
	{
		assertFalse(test("critical", "nex"));
		assertFalse(test("info", "Nex, inferno"), "case-insensitive csv match");
		assertTrue(test("critical", "inferno, tob_sotetseg"), "other bosses unaffected");
	}

	@Test
	void masterKillSwitchBlocksEverything()
	{
		assertFalse(CalloutFilter.isEnabled(false, false, "", true, true, true, true, "nex", callout("critical")));
	}

	@Test
	void nullCategoryFallsBackToInfoToggle()
	{
		assertFalse(CalloutFilter.isEnabled(true, false, "", true, true, false, true, "nex", callout(null)));
		assertTrue(CalloutFilter.isEnabled(true, false, "", true, true, true, true, "nex", callout(null)));
	}
}

