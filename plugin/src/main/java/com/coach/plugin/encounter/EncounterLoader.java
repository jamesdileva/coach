package com.coach.plugin.encounter;

import com.coach.plugin.encounter.model.CalloutDefinition;
import com.coach.plugin.encounter.model.EncounterPack;
import com.coach.plugin.encounter.model.MechanicDefinition;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Loads and validates encounter packs (.zip files containing encounter.json)
 * from the user's pack directory (rule 9: packs are never bundled in the JAR).
 *
 * Load pipeline (§10): raw JSON → schema migration to current version →
 * Gson parse → rule validation → audio file existence check.
 */
public class EncounterLoader
{
	public static final String PACK_ENTRY = "encounter.json";
	public static final String AUDIO_PREFIX = "audio/";

	private final Gson gson = new Gson();
	private final SchemaValidator validator = new SchemaValidator();

	/**
	 * Parse + validate an encounter JSON string (no zip context:
	 * audio file references are not checked here).
	 */
	public EncounterPack parseJson(String content, String sourceName) throws PackLoadException
	{
		return parse(content, sourceName, null);
	}

	private EncounterPack parse(String content, String sourceName, Set<String> availableAudio)
		throws PackLoadException
	{
		if (content == null || content.trim().isEmpty())
		{
			throw new PackLoadException(sourceName + ": encounter.json is empty");
		}

		com.google.gson.JsonElement element;
		try
		{
			element = new JsonParser().parse(content);
		}
		catch (JsonSyntaxException | IllegalStateException e)
		{
			throw new PackLoadException(sourceName + ": invalid JSON — " + e.getMessage());
		}
		if (!element.isJsonObject())
		{
			throw new PackLoadException(sourceName + ": encounter.json must be a JSON object");
		}
		JsonObject root = SchemaMigrations.bringToCurrent(element.getAsJsonObject(), sourceName);

		EncounterPack pack;
		try
		{
			pack = gson.fromJson(root, EncounterPack.class);
		}
		catch (JsonSyntaxException e)
		{
			throw new PackLoadException(sourceName + ": invalid JSON structure — " + e.getMessage());
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

		if (availableAudio != null)
		{
			checkAudioReferences(pack, availableAudio, sourceName);
		}
		return pack;
	}

	/**
	 * Load a .zip pack: extract encounter.json, migrate, parse, validate,
	 * verify referenced audio files exist in the pack (rule 8).
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
			Set<String> audioFiles = new HashSet<>();
			java.util.Enumeration<? extends ZipEntry> entries = zip.entries();
			while (entries.hasMoreElements())
			{
				ZipEntry e = entries.nextElement();
				String name = e.getName();
				if (!e.isDirectory() && name.startsWith(AUDIO_PREFIX))
				{
					audioFiles.add(name.substring(AUDIO_PREFIX.length()));
				}
			}
			try (InputStream in = zip.getInputStream(entry))
			{
				return parse(readAll(in), sourceName, audioFiles);
			}
		}
		catch (IOException e)
		{
			throw new PackLoadException(sourceName + ": could not open zip — " + e.getMessage());
		}
	}

	private static void checkAudioReferences(EncounterPack pack, Set<String> available,
		String sourceName) throws PackLoadException
	{
		Set<String> missing = new java.util.LinkedHashSet<>();
		for (com.coach.plugin.encounter.model.BossDefinition boss : pack.bosses)
		{
			if (boss.mechanics != null)
			{
				collectMissingAudio(boss.mechanics, available, missing);
			}
			for (com.coach.plugin.encounter.model.PhaseDefinition phase : boss.phases)
			{
				if (phase.mechanics != null)
				{
					collectMissingAudio(phase.mechanics, available, missing);
				}
			}
		}
		if (!missing.isEmpty())
		{
			throw new PackLoadException(sourceName
				+ ": audio file(s) referenced by callouts are missing from the pack: "
				+ String.join(", ", missing));
		}
	}

	private static void collectMissingAudio(List<MechanicDefinition> mechanics,
		Set<String> available, Set<String> missing)
	{
		for (MechanicDefinition mechanic : mechanics)
		{
			if (mechanic.callouts == null)
			{
				continue;
			}
			for (CalloutDefinition callout : mechanic.callouts)
			{
				String audioFile = callout.audioFile;
				if (audioFile != null && !available.contains(audioFile))
				{
					missing.add(audioFile);
				}
			}
		}
	}

	private static String readAll(InputStream in) throws IOException
	{
		StringBuilder sb = new StringBuilder();
		char[] buf = new char[4096];
		java.io.InputStreamReader reader = new java.io.InputStreamReader(in, StandardCharsets.UTF_8);
		int n;
		while ((n = reader.read(buf)) != -1)
		{
			sb.append(buf, 0, n);
		}
		return sb.toString();
	}
}
