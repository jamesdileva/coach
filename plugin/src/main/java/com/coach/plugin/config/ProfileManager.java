package com.coach.plugin.config;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.client.config.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Named settings profiles: save the current callout-relevant config values
 * under a name, apply them back later. Storage is the hidden
 * `coach.profilesJson` config item (RuneLite-native persistence).
 *
 * Learning / Practice / Performance style presets fall out of this by saving
 * different toggles under different names.
 */
public class ProfileManager
{
	private static final Logger log = LoggerFactory.getLogger(ProfileManager.class);
	private static final String GROUP = "coach";
	private static final Type PROFILE_MAP_TYPE =
		new TypeToken<Map<String, ConfigProfile>>() { }.getType();

	private final ConfigManager configManager;
	private final CoachConfig config;
	private final Gson gson = new Gson();

	@Inject
	public ProfileManager(ConfigManager configManager, CoachConfig config)
	{
		this.configManager = configManager;
		this.config = config;
	}

	public Map<String, ConfigProfile> listProfiles()
	{
		try
		{
			Map<String, ConfigProfile> profiles =
				gson.fromJson(config.profilesJson(), PROFILE_MAP_TYPE);
			return profiles != null ? profiles : new LinkedHashMap<>();
		}
		catch (Exception e)
		{
			log.warn("[coach] could not parse profiles: {}", e.getMessage());
			return new LinkedHashMap<>();
		}
	}

	public void saveProfile(String name)
	{
		Map<String, ConfigProfile> profiles = listProfiles();
		profiles.put(name, snapshot(name));
		persist(profiles);
		log.info("[coach] profile '{}' saved", name);
	}

	public void deleteProfile(String name)
	{
		Map<String, ConfigProfile> profiles = listProfiles();
		if (profiles.remove(name) != null)
		{
			persist(profiles);
			log.info("[coach] profile '{}' deleted", name);
		}
	}

	/**
	 * Apply a saved profile to live config. Returns false when unknown.
	 */
	public boolean applyProfile(String name)
	{
		ConfigProfile profile = listProfiles().get(name);
		if (profile == null)
		{
			log.warn("[coach] profile '{}' not found", name);
			return false;
		}
		set("enabled", profile.enabled);
		set("debugMode", profile.debugMode);
		set("muted", profile.muted);
		set("masterVolume", String.valueOf(profile.masterVolume));
		set("criticalCallouts", profile.criticalCallouts);
		set("warningCallouts", profile.warningCallouts);
		set("infoCallouts", profile.infoCallouts);
		set("transitionCallouts", profile.transitionCallouts);
		set("disabledBosses", profile.disabledBosses == null ? "" : profile.disabledBosses);
		log.info("[coach] profile '{}' applied", name);
		return true;
	}

	private ConfigProfile snapshot(String name)
	{
		ConfigProfile profile = new ConfigProfile();
		profile.name = name;
		profile.enabled = config.enabled();
		profile.debugMode = config.debugMode();
		profile.muted = config.muted();
		profile.masterVolume = config.masterVolume();
		profile.criticalCallouts = config.criticalCallouts();
		profile.warningCallouts = config.warningCallouts();
		profile.infoCallouts = config.infoCallouts();
		profile.transitionCallouts = config.transitionCallouts();
		profile.disabledBosses = config.disabledBosses();
		return profile;
	}

	private void persist(Map<String, ConfigProfile> profiles)
	{
		set("profilesJson", gson.toJson(profiles));
	}

	private void set(String key, String value)
	{
		configManager.setConfiguration(GROUP, key, value);
	}

	private void set(String key, boolean value)
	{
		set(key, String.valueOf(value));
	}
}
