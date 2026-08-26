package com.coach.plugin.config;

import java.io.IOException;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Exports named profiles to JSON files under coach/profiles/.
 */
public class ProfileExporter
{
	private static final Logger log = LoggerFactory.getLogger(ProfileExporter.class);

	private final ProfileManager manager;
	private final ProfileStorage storage;
	private final Path directory;

	public ProfileExporter(ProfileManager manager, ProfileStorage storage, Path directory)
	{
		this.manager = manager;
		this.storage = storage;
		this.directory = directory;
	}

	/**
	 * @return the written path, or null when the profile doesn't exist / write failed
	 */
	public Path export(String name)
	{
		ConfigProfile profile = manager.listProfiles().get(name);
		if (profile == null)
		{
			log.warn("[coach] cannot export profile '{}': not found", name);
			return null;
		}
		try
		{
			Path safe = directory.resolve(name.replaceAll("[^A-Za-z0-9_-]", "_") + ".json");
			storage.write(safe, profile);
			log.info("[coach] profile '{}' exported to {}", name, safe);
			return safe;
		}
		catch (IOException e)
		{
			log.warn("[coach] could not export profile '{}': {}", name, e.getMessage());
			return null;
		}
	}
}
