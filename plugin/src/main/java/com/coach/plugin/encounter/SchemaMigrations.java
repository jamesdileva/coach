package com.coach.plugin.encounter;

import com.coach.plugin.encounter.model.EncounterPack;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Brings older encounter pack versions up to the current schema by applying
 * declarative migration steps from /schemas/migration_v0_to_v1.json.
 *
 * Unknown versions (newer than the plugin, or with no migration path) are
 * rejected with an actionable message — never silently loaded (§10).
 */
public final class SchemaMigrations
{
	private static final String MIGRATIONS_RESOURCE = "/schemas/migration_v0_to_v1.json";

	private static final Gson GSON = new Gson();
	private static volatile Map<String, Migration> migrations;

	private SchemaMigrations()
	{
	}

	public static JsonObject bringToCurrent(JsonObject root, String sourceName) throws PackLoadException
	{
		JsonElement versionElement = root.get("schemaVersion");
		String version = versionElement != null && !versionElement.isJsonNull()
			? versionElement.getAsString() : null;

		if (version == null)
		{
			// legacy layouts may omit schemaVersion entirely — try detectors
			Migration detected = detectLegacyLayout(root);
			if (detected == null)
			{
				throw new PackLoadException(sourceName + ": missing required field: schemaVersion");
			}
			version = detected.from;
		}
		if (EncounterPack.SUPPORTED_SCHEMA_VERSION.equals(version))
		{
			return root; // already current
		}

		int guard = 0;
		while (!EncounterPack.SUPPORTED_SCHEMA_VERSION.equals(version))
		{
			if (++guard > 10)
			{
				throw new PackLoadException(sourceName + ": migration loop detected at version " + version);
			}
			Migration migration = loadMigrations().get(version);
			if (migration == null)
			{
				throw new PackLoadException(sourceName + ": unsupported schemaVersion '" + version
					+ "' and no migration path to " + EncounterPack.SUPPORTED_SCHEMA_VERSION);
			}
			for (Step step : migration.steps)
			{
				step.apply(root);
			}
			// reaching the target version is implicit in the chain
			root.addProperty("schemaVersion", migration.to);
			version = migration.to;
		}
		return root;
	}

	private static Migration detectLegacyLayout(JsonObject root)
	{
		for (Migration migration : loadMigrations().values())
		{
			if (migration.detect != null && root.has(migration.detect.path))
			{
				return migration;
			}
		}
		return null;
	}

	private static synchronized Map<String, Migration> loadMigrations()
	{
		if (migrations != null)
		{
			return migrations;
		}
		try (InputStream in = SchemaMigrations.class.getResourceAsStream(MIGRATIONS_RESOURCE))
		{
			if (in == null)
			{
				throw new IllegalStateException("missing resource " + MIGRATIONS_RESOURCE);
			}
			MigrationFile file = GSON.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), MigrationFile.class);
			com.google.common.collect.ImmutableMap.Builder<String, Migration> builder =
				com.google.common.collect.ImmutableMap.builder();
			for (Migration migration : file.migrations)
			{
				builder.put(migration.from, migration);
			}
			migrations = builder.build();
		}
		catch (Exception e)
		{
			throw new IllegalStateException("could not load schema migrations", e);
		}
		return migrations;
	}

	static final class MigrationFile
	{
		List<Migration> migrations;
	}

	static final class Migration
	{
		String from;
		String to;
		Detect detect;
		List<Step> steps;
	}

	static final class Detect
	{
		String path;
	}

	static final class Step
	{
		String op;
		String from;
		String to;
		String path;
		String value;

		void apply(JsonObject root)
		{
			switch (op)
			{
				case "rename":
					if (root.has(from))
					{
						root.add(to, root.remove(from));
					}
					break;
				case "copy":
					if (root.has(from))
					{
						root.add(to, root.get(from).deepCopy());
					}
					break;
				case "set":
					root.addProperty(path, value);
					break;
				case "remove":
					root.remove(path);
					break;
				default:
					throw new IllegalArgumentException("unknown migration op: " + op);
			}
		}
	}
}
