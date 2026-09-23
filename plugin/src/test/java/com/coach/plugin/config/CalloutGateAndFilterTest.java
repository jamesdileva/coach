package com.coach.plugin.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.coach.plugin.encounter.model.CalloutDefinition;
import com.coach.plugin.encounter.model.VisualDefinition;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CalloutGateAndFilterTest
{
	private static CalloutDefinition callout(String category)
	{
		CalloutDefinition c = new CalloutDefinition();
		c.calloutId = "c1";
		c.text = "hello";
		c.category = category;
		return c;
	}

	private static CalloutDefinition calloutVisual(String category, Integer duration, String type)
	{
		CalloutDefinition c = callout(category);
		c.visual = new VisualDefinition();
		c.visual.durationTicks = duration;
		c.visual.type = type;
		return c;
	}

	private static CoachConfig config(
		boolean enabled, boolean essentialOnly, String disabled,
		boolean critical, boolean warning, boolean info, boolean transition)
	{
		CoachConfig config = mock(CoachConfig.class);
		when(config.enabled()).thenReturn(enabled);
		when(config.essentialOnly()).thenReturn(essentialOnly);
		when(config.disabledBosses()).thenReturn(disabled);
		when(config.criticalCallouts()).thenReturn(critical);
		when(config.warningCallouts()).thenReturn(warning);
		when(config.infoCallouts()).thenReturn(info);
		when(config.transitionCallouts()).thenReturn(transition);
		return config;
	}

	@Test
	void filterRejectsWhenDisabledOrNull()
	{
		assertFalse(CalloutFilter.isEnabled(false, false, "", true, true, true, true, "nex", callout("critical")));
		assertFalse(CalloutFilter.isEnabled(true, false, "", true, true, true, true, "nex", null));
	}

	@Test
	void filterEssentialOnlyKeepsCritical()
	{
		assertTrue(CalloutFilter.isEnabled(true, true, "", true, true, true, true, "nex", callout("critical")));
		assertFalse(CalloutFilter.isEnabled(true, true, "", true, true, true, true, "nex", callout("warning")));
		assertFalse(CalloutFilter.isEnabled(true, true, "", true, true, true, true, "nex", callout("info")));
		assertFalse(CalloutFilter.isEnabled(true, true, "", true, true, true, true, "nex", callout("transition")));
	}

	@Test
	void filterHonoursCategoryTogglesAndNullCategory()
	{
		assertFalse(CalloutFilter.isEnabled(true, false, "", false, false, false, false, "nex", callout(null)));
		assertTrue(CalloutFilter.isEnabled(true, false, "", false, false, true, false, "nex", callout(null)));
		assertFalse(CalloutFilter.isEnabled(true, false, "", false, false, false, false, "nex", callout("critical")));
		assertTrue(CalloutFilter.isEnabled(true, false, "", true, false, false, false, "nex", callout("critical")));
		assertFalse(CalloutFilter.isEnabled(true, false, "", false, false, false, false, "nex", callout("warning")));
		assertTrue(CalloutFilter.isEnabled(true, false, "", false, true, false, false, "nex", callout("warning")));
		assertFalse(CalloutFilter.isEnabled(true, false, "", false, false, false, false, "nex", callout("info")));
		assertTrue(CalloutFilter.isEnabled(true, false, "", false, false, true, false, "nex", callout("info")));
		assertFalse(CalloutFilter.isEnabled(true, false, "", false, false, false, false, "nex", callout("transition")));
		assertTrue(CalloutFilter.isEnabled(true, false, "", false, false, false, true, "nex", callout("transition")));
	}

	@Test
	void filterAppliesDisabledBossesCaseInsensitive()
	{
		assertFalse(CalloutFilter.isEnabled(true, false, "Nex , inferno", true, true, true, true, "nex", callout("critical")));
		assertTrue(CalloutFilter.isEnabled(true, false, "nex", true, true, true, true, "zulrah", callout("critical")));
		assertTrue(CalloutFilter.isEnabled(true, false, "nex", true, true, true, true, null, callout("critical")));
	}

	@Test
	void categoryEnabledAndDisabledBossesHelpers()
	{
		assertTrue(CalloutFilter.categoryEnabled(null, false, false, true, false));
		assertEquals(Set.of(), CalloutFilter.disabledBosses(null));
		assertEquals(Set.of(), CalloutFilter.disabledBosses("   "));
		assertEquals(Set.of("a", "b"), CalloutFilter.disabledBosses("a, b"));
		assertTrue(CalloutFilter.categoryEnabled("critical", true, false, false, false));
		assertFalse(CalloutFilter.categoryEnabled("warning", true, false, false, false));
	}

	@Test
	void gateDelegatesToFilterWithLiveConfig()
	{
		CoachConfig live = config(true, false, "nex", true, true, true, true);
		CalloutGate gate = new CalloutGate(live);
		assertFalse(gate.test("nex", callout("critical")));
		assertTrue(gate.test("zulrah", callout("critical")));
		assertEquals(Set.of("nex"), gate.disabledBosses());

		CoachConfig essential = config(true, true, "", true, true, true, true);
		CalloutGate essentialGate = new CalloutGate(essential);
		assertTrue(essentialGate.test("nex", callout("critical")));
		assertFalse(essentialGate.test("nex", callout("info")));
	}

	@Test
	void coachConfigDefaultsMatchSpec()
	{
		CoachConfig defaults = new CoachConfig()
		{
		};
		assertTrue(defaults.enabled());
		assertFalse(defaults.debugMode());
		assertFalse(defaults.logToFile());
		assertFalse(defaults.muted());
		assertEquals(70, defaults.masterVolume());
		assertEquals(100, defaults.criticalVolume());
		assertEquals(80, defaults.warningVolume());
		assertEquals(60, defaults.infoVolume());
		assertTrue(defaults.criticalCallouts());
		assertTrue(defaults.warningCallouts());
		assertTrue(defaults.infoCallouts());
		assertTrue(defaults.transitionCallouts());
		assertEquals("", defaults.disabledBosses());
		assertEquals("{}", defaults.profilesJson());
		assertFalse(defaults.defaultsSeeded());
		assertTrue(defaults.showPrayerIndicator());
		assertTrue(defaults.showCountdown());
		assertTrue(defaults.showTimeline());
		assertTrue(defaults.showStatus());
		assertTrue(defaults.showMiniHud());
		assertEquals(AccessibilityMode.BOTH, defaults.accessibilityMode());
		assertFalse(defaults.essentialOnly());
		assertFalse(defaults.highContrast());
		assertEquals(100, defaults.textScale());
		assertEquals(CoachConfig.DebugTab.EVENTS, defaults.debugTab());
		assertTrue(defaults.packDirectory().endsWith("encounters"));
	}
}
