package com.coach.plugin.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * File I/O for profile JSON files (roadmap Sprint 22 export/import).
 * Read failures carry actionable messages; write creates parent dirs.
 */
public class ProfileStorage
{
	private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

	public void write(Path file, ConfigProfile profile) throws IOException
	{
		if (profile == null || profile.name == null || profile.name.trim().isEmpty())
		{
			throw new IOException("profile has no name — cannot write");
		}
		Files.createDirectories(file.getParent());
		Files.writeString(file, gson.toJson(profile), StandardCharsets.UTF_8);
	}

	/**
	 * Read + structurally validate a profile file.
	 *
	 * @throws IOException unreadable, unparsable, or invalid structure
	 */
	public ConfigProfile read(Path file) throws IOException
	{
		String content;
		try
		{
			content = Files.readString(file, StandardCharsets.UTF_8);
		}
		catch (IOException e)
		{
			throw new IOException("could not read profile file: " + e.getMessage());
		}

		ConfigProfile profile;
		try
		{
			profile = gson.fromJson(content, ConfigProfile.class);
		}
		catch (JsonSyntaxException e)
		{
			throw new IOException("invalid profile JSON: " + e.getMessage());
		}
		if (profile == null)
		{
			throw new IOException("profile file is empty");
		}
		if (profile.name == null || profile.name.trim().isEmpty())
		{
			throw new IOException("invalid profile: missing required field 'name'");
		}
		if (profile.masterVolume < 0 || profile.masterVolume > 100)
		{
			profile.masterVolume = Math.max(0, Math.min(100, profile.masterVolume));
		}
		return profile;
	}
}
