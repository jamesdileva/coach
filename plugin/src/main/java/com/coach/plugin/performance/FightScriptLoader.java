package com.coach.plugin.performance;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads FightScript JSON documents.
 * Lives in main scope so the Sprint 30b replay CLI can reuse it without
 * depending on test classes.
 */
public final class FightScriptLoader
{
	private FightScriptLoader()
	{
	}

	public static FightScript fromJson(String json) throws IOException
	{
		try
		{
			return FightScript.fromJson(json);
		}
		catch (RuntimeException e)
		{
			throw new IOException("invalid fight script: " + e.getMessage(), e);
		}
	}

	public static FightScript fromFile(Path file) throws IOException
	{
		return fromJson(Files.readString(file));
	}

	/**
	 * Load every fight script *.json directly under {@code directory} (non-recursive).
	 */
	public static List<FightScript> fromDirectory(Path directory) throws IOException
	{
		List<FightScript> scripts = new ArrayList<>();
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*.json"))
		{
			for (Path file : stream)
			{
				scripts.add(fromFile(file));
			}
		}
		return scripts;
	}
}
