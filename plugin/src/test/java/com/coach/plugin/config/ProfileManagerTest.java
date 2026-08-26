package com.coach.plugin.config;

import com.google.gson.Gson;
import java.util.Map;
import net.runelite.client.config.ConfigManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProfileManagerTest
{
	private ConfigManager configManager;
	private CoachConfig config;
	private ProfileManager profiles;

	@BeforeEach
	void setUp()
	{
		configManager = mock(ConfigManager.class);
		config = mock(CoachConfig.class);
		when(config.profilesJson()).thenReturn("{}");
		when(config.enabled()).thenReturn(true);
		when(config.debugMode()).thenReturn(false);
		when(config.muted()).thenReturn(false);
		when(config.masterVolume()).thenReturn(70);
		when(config.criticalCallouts()).thenReturn(true);
		when(config.warningCallouts()).thenReturn(true);
		when(config.infoCallouts()).thenReturn(true);
		when(config.transitionCallouts()).thenReturn(true);
		when(config.disabledBosses()).thenReturn("");
		profiles = new ProfileManager(configManager, config);
	}

	@Test
	void savePersistsSnapshotThenApplyWritesValuesBack()
	{
		when(config.masterVolume()).thenReturn(30);
		when(config.muted()).thenReturn(true);
		when(config.disabledBosses()).thenReturn("inferno");
		profiles.saveProfile("quiet");

		ArgumentCaptor<String> stored = ArgumentCaptor.forClass(String.class);
		verify(configManager).setConfiguration(
			org.mockito.ArgumentMatchers.eq("coach"),
			org.mockito.ArgumentMatchers.eq("profilesJson"),
			stored.capture());
		assertTrue(stored.getValue().contains("\"quiet\""));

		// simulate reload: manager reads what was persisted
		ProfileManager fresh = new ProfileManager(configManager,
			configWithProfilesJson(stored.getValue()));

		// now settings drift; applying the profile should write them back
		fresh.applyProfile("quiet");

		verify(configManager).setConfiguration("coach", "masterVolume", "30");
		verify(configManager).setConfiguration("coach", "muted", "true");
		verify(configManager).setConfiguration("coach", "disabledBosses", "inferno");
	}

	@Test
	void applyUnknownProfileReturnsFalse()
	{
		assertFalse(profiles.applyProfile("nope"));
	}

	@Test
	void deleteRemovesProfile() throws Exception
	{
		profiles.saveProfile("temp");
		ArgumentCaptor<String> stored = ArgumentCaptor.forClass(String.class);
		verify(configManager).setConfiguration(
			org.mockito.ArgumentMatchers.eq("coach"),
			org.mockito.ArgumentMatchers.eq("profilesJson"),
			stored.capture());

		ProfileManager fresh = new ProfileManager(configManager,
			configWithProfilesJson(stored.getValue()));
		fresh.deleteProfile("temp");

		ArgumentCaptor<String> after = ArgumentCaptor.forClass(String.class);
		verify(configManager, org.mockito.Mockito.times(2))
			.setConfiguration(
				org.mockito.ArgumentMatchers.eq("coach"),
				org.mockito.ArgumentMatchers.eq("profilesJson"),
				after.capture());
		Map<String, Object> parsed = new Gson().fromJson(after.getValue(), java.util.Map.class);
		assertEquals(0, parsed.size(), "profile removed from storage");
	}

	private CoachConfig configWithProfilesJson(String json)
	{
		CoachConfig mockConfig = mock(CoachConfig.class);
		when(mockConfig.profilesJson()).thenReturn(json);
		return mockConfig;
	}
}
