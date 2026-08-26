package com.coach.plugin.config;

import java.io.IOException;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Imports profiles from JSON files, validating before they touch live config.
 */
public class ProfileImporter
{
	private static final Logger log = LoggerFactory.getLogger(ProfileImporter.class);

	private final ProfileManager manager;
	private final ProfileStorage storage;

	public ProfileImporter(ProfileManager manager, ProfileStorage storage)
	{
		this.manager = manager;
		this.storage = storage;
	}

	/**
	 * Import a profile file into the stored profiles list.
	 *
	 * @return the imported profile
	 * @throws IOException with an actionable message when the file is invalid
	 */
	public ConfigProfile importFile(Path file) throws IOException
	{
		ConfigProfile profile = storage.read(file);
		manager.importProfile(profile);
		log.info("[coach] profile '{}' imported from {}", profile.name, file.getFileName());
		return profile;
	}
}
