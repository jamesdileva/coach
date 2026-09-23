package com.coach.plugin.accessibility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.coach.plugin.config.AccessibilityMode;
import com.coach.plugin.config.CoachConfig;
import org.junit.jupiter.api.Test;

class AccessibilityManagerTest
{
	private static CoachConfig config(AccessibilityMode mode, boolean muted, boolean enabled)
	{
		CoachConfig config = mock(CoachConfig.class);
		when(config.accessibilityMode()).thenReturn(mode);
		when(config.muted()).thenReturn(muted);
		when(config.enabled()).thenReturn(enabled);
		return config;
	}

	@Test
	void modesAndAudioVisualGates()
	{
		AccessibilityManager both = new AccessibilityManager(config(AccessibilityMode.BOTH, false, true));
		assertEquals(AccessibilityManager.Mode.BOTH, both.getMode());
		assertTrue(both.isAudioEnabled());
		assertTrue(both.isVisualEnabled());

		AccessibilityManager audioOnly = new AccessibilityManager(config(AccessibilityMode.AUDIO_ONLY, false, true));
		assertEquals(AccessibilityManager.Mode.AUDIO_ONLY, audioOnly.getMode());
		assertTrue(audioOnly.isAudioEnabled());
		assertFalse(audioOnly.isVisualEnabled());

		AccessibilityManager visualOnly = new AccessibilityManager(config(AccessibilityMode.VISUAL_ONLY, false, true));
		assertEquals(AccessibilityManager.Mode.VISUAL_ONLY, visualOnly.getMode());
		assertFalse(visualOnly.isAudioEnabled());
		assertTrue(visualOnly.isVisualEnabled());
	}

	@Test
	void mutedAndDisabledConfig()
	{
		AccessibilityManager muted = new AccessibilityManager(config(AccessibilityMode.BOTH, true, true));
		assertFalse(muted.isAudioEnabled());
		assertTrue(muted.isVisualEnabled());

		AccessibilityManager disabled = new AccessibilityManager(config(AccessibilityMode.BOTH, false, false));
		assertTrue(disabled.isAudioEnabled());
		assertFalse(disabled.isVisualEnabled());
	}

	@Test
	void invalidModeFallsBackToBoth()
	{
		CoachConfig weird = mock(CoachConfig.class);
		when(weird.accessibilityMode()).thenReturn(null);
		when(weird.muted()).thenReturn(false);
		when(weird.enabled()).thenReturn(true);
		AccessibilityManager manager = new AccessibilityManager(weird);
		assertEquals(AccessibilityManager.Mode.BOTH, manager.getMode());
		assertTrue(manager.isAudioEnabled());
		assertTrue(manager.isVisualEnabled());
	}
}
