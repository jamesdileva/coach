package com.coach.plugin.encounter;

import com.coach.plugin.encounter.model.EncounterPack;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Loads and validates encounter packs (.zip files containing encounter.json)
 * from the user's pack directory (rule 9: packs are never bundled in the JAR).
 */
public class EncounterLoader
{
	public static final String PACK_ENTRY = "encounter.json";

	private final Gson gson = new Gson();
	private final SchemaValidator validator = new SchemaValidator();

	/**
	 * Parse + validate an encounter JSON string. Useful for tests and loose files.
	 */
	public EncounterPack parseJson(String content, String sourceName) throws PackLoadException
	{
		if (content == null || content.trim().isEmpty())
		{
			throw new PackLoadException(sourceName + ": encounter.json is empty");
		}

		EncounterPack pack;
		try
		{
			pack = gson.fromJson(content, EncounterPack.class);
		}
		catch (JsonSyntaxException e)
		{
			throw new PackLoadException(sourceName + ": invalid JSON — " + e.getMessage());
		}
		if (pack == null)
		{
			throw new PackLoadException(sourceName + ": encounter.json parsed to nothing");
		}
		pack.sourceName = sourceName;

		java.util.List<String> errors = validator.validate(pack);
		if (!errors.isEmpty())
		{
			throw new PackLoadException(sourceName + " failed validation: "
				+ String.join("; ", errors));
		}
		return pack;
	}

	/**
	 * Load a .zip pack: extract encounter.json, parse, validate.
	 */
	public EncounterPack loadZip(Path zipPath) throws PackLoadException
	{
		String sourceName = zipPath.getFileName() != null ? zipPath.getFileName().toString() : zipPath.toString();
		try (ZipFile zip = new ZipFile(zipPath.toFile()))
		{
			ZipEntry entry = zip.getEntry(PACK_ENTRY);
			if (entry == null)
			{
				throw new PackLoadException(sourceName + ": missing " + PACK_ENTRY
					+ " — is this a Coach encounter pack?");
			}
			try (InputStream in = zip.getInputStream(entry))
			{
				String content = readAll(in);
				return parseJson(content, sourceName);
			}
		}
		catch (IOException e)
		{
			throw new PackLoadException(sourceName + ": could not open zip — " + e.getMessage());
		}
	}

	private static String readAll(InputStream in) throws IOException
	{
		StringBuilder sb = new StringBuilder();
		char[] buf = new char[4096];
		InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
		int n;
		while ((n = reader.read(buf)) != -1)
		{
			sb.append(buf, 0, n);
		}
		return sb.toString();
	}
}
