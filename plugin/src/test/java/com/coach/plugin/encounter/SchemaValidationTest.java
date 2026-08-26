package com.coach.plugin.encounter;

import com.coach.plugin.encounter.model.EncounterPack;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sprint 10 consolidated validation suite: runs the loader against the
 * committed zip fixtures in src/test/resources/test_packs/ plus edge cases
 * for the schema v1.0 rules (master-architecture §10).
 */
class SchemaValidationTest
{
	private static final EncounterLoader LOADER = new EncounterLoader();

	@TempDir
	static Path tempDir;

	private static Path extractFixture(String name) throws Exception
	{
		Path zip = tempDir.resolve(name);
		try (InputStream in = SchemaValidationTest.class.getResourceAsStream("/test_packs/" + name))
		{
			if (in == null)
			{
				throw new IllegalStateException("missing fixture: " + name);
			}
			Files.copy(in, zip, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
		}
		return zip;
	}

	@BeforeAll
	static void extractAll() throws Exception
	{
		extractFixture("valid_pack.zip");
		extractFixture("invalid_missing_fields.zip");
		extractFixture("invalid_bad_trigger_type.zip");
		extractFixture("legacy_v09_pack.zip");
	}

	// ---- fixtures ----

	@Test
	void validPackLoadsIncludingAudioReference()
		throws Exception
	{
		EncounterPack pack = LOADER.loadZip(tempDir.resolve("valid_pack.zip"));

		assertEquals("fixture", pack.metadata.packId);
		assertEquals(1, pack.bosses.size());
		assertEquals("pray_ranged.wav", pack.bosses.get(0)
			.phases.get(0).mechanics.get(0).callouts.get(0).audioFile);
	}

	@Test
	void invalidMissingFieldsRejectedWithClearMessage()
		throws Exception
	{
		PackLoadException e = assertThrows(PackLoadException.class,
			() -> LOADER.loadZip(tempDir.resolve("invalid_missing_fields.zip")));
		assertTrue(e.getMessage().contains("invalid_missing_fields.zip failed validation"),
			e.getMessage());
	}

	@Test
	void invalidBadTriggerTypeRejectedWithClearMessage()
		throws Exception
	{
		PackLoadException e = assertThrows(PackLoadException.class,
			() -> LOADER.loadZip(tempDir.resolve("invalid_bad_trigger_type.zip")));
		assertTrue(e.getMessage().contains("unknown trigger type 'teleport'"), e.getMessage());
	}

	@Test
	void legacyV09PackMigratesAndLoads()
		throws Exception
	{
		EncounterPack pack = LOADER.loadZip(tempDir.resolve("legacy_v09_pack.zip"));

		assertEquals("1.0", pack.schemaVersion, "migrated to current version");
		assertEquals("legacy", pack.metadata.packId);
		assertEquals(1, pack.bosses.size(), "'encounters' renamed to 'bosses'");
	}

	// ---- rule-by-rule edge cases ----

	/** Minimal valid v1.0 pack; mechanicExtras splice into the single mechanic. */
	private static String packWith(String mechanicExtras, String triggerJson)
	{
		return "{\"schemaVersion\": \"1.0\","
			+ "\"metadata\": {\"packId\": \"v\", \"name\": \"V\", \"version\": \"1.0.0\", \"gameVersion\": \"x\"},"
			+ "\"bosses\": [{"
			+ "\"bossId\": \"golem\", \"name\": \"Golem\", \"npcId\": 7878,"
			+ "\"phases\": [{"
			+ "\"phaseId\": \"p1\", \"name\": \"P1\","
			+ "\"entryTrigger\": {\"type\": \"npc_spawn\", \"npcId\": 7878},"
			+ "\"mechanics\": [{"
			+ "  \"mechanicId\": \"slam\", \"name\": \"Slam\","
			+ "  \"triggers\": [" + triggerJson + "],"
			+ "  \"callouts\": [{\"calloutId\": \"c1\", \"text\": \"Pray!\", \"category\": \"critical\", \"priority\": 90}]"
			+ (mechanicExtras.isEmpty() ? "" : "," + mechanicExtras)
			+ "}]}]}]}";
	}

	private static final String ANIM_TRIGGER =
		"{\"triggerId\": \"t1\", \"type\": \"animation\", \"npcId\": 7878, \"animationId\": 8960}";

	@Test
	void unsupportedFutureVersionRejectedWithoutMigrationPath()
	{
		String json = "{\"schemaVersion\":\"2.0\",\"metadata\":{\"packId\":\"f\",\"name\":\"F\",\"version\":\"2.0.0\",\"gameVersion\":\"x\"},\"bosses\":[]}";
		PackLoadException e = assertThrows(PackLoadException.class,
			() -> LOADER.parseJson(json, "future.zip"));
		assertTrue(e.getMessage().contains("no migration path"), e.getMessage());
	}

	@Test
	void unknownLegacyVersionRejected()
	{
		String json = "{\"schemaVersion\":\"0.5\",\"metadata\":{}}";
		PackLoadException e = assertThrows(PackLoadException.class,
			() -> LOADER.parseJson(json, "ancient.zip"));
		assertTrue(e.getMessage().contains("no migration path"), e.getMessage());
	}

	@Test
	void duplicateCalloutIdsRejected()
	{
		String json = packWith("", ANIM_TRIGGER).replace(
			"\"calloutId\": \"c1\", \"text\": \"Pray!\", \"category\": \"critical\", \"priority\": 90",
			"\"calloutId\": \"c1\", \"text\": \"A\", \"category\": \"info\"},"
				+ "{\"calloutId\": \"c1\", \"text\": \"B\", \"category\": \"info\"");
		PackLoadException e = assertThrows(PackLoadException.class,
			() -> LOADER.parseJson(json, "dupcallout.zip"));
		assertTrue(e.getMessage().contains("duplicate calloutId"), e.getMessage());
	}

	@Test
	void compositeTriggerWithoutChildrenRejected()
	{
		PackLoadException e = assertThrows(PackLoadException.class,
			() -> LOADER.parseJson(packWith("", "{\"type\":\"composite\"}"), "comp.zip"));
		assertTrue(e.getMessage().contains("composite trigger requires children"), e.getMessage());
	}

	@Test
	void compositeTriggerWithBadLogicRejected()
	{
		PackLoadException e = assertThrows(PackLoadException.class,
			() -> LOADER.parseJson(packWith("",
				"{\"type\":\"composite\",\"logic\":\"XOR\",\"children\":[" + ANIM_TRIGGER + "]}"),
			"logic.zip"));
		assertTrue(e.getMessage().contains("composite logic must be AND or OR"), e.getMessage());
	}

	@Test
	void negativeCooldownRejected()
	{
		PackLoadException e = assertThrows(PackLoadException.class,
			() -> LOADER.parseJson(packWith("\"cooldown\":-3", ANIM_TRIGGER), "cd.zip"));
		assertTrue(e.getMessage().contains("cooldown must be >= 0"), e.getMessage());
	}

	@Test
	void priorityOutOfRangeRejected()
	{
		String json = packWith("", ANIM_TRIGGER).replace("\"priority\": 90", "\"priority\":900");
		PackLoadException e = assertThrows(PackLoadException.class,
			() -> LOADER.parseJson(json, "prio.zip"));
		assertTrue(e.getMessage().contains("priority must be 1-100"), e.getMessage());
	}

	@Test
	void hpConditionMissingThresholdRejected()
	{
		PackLoadException e = assertThrows(PackLoadException.class,
			() -> LOADER.parseJson(packWith(
				"\"conditions\":[{\"type\":\"npc_hp_below\"}]", ANIM_TRIGGER), "cond.zip"));
		assertTrue(e.getMessage().contains("hp conditions require a threshold"), e.getMessage());
	}

	@Test
	void unknownConditionTypeRejectedAtLoadTime()
	{
		PackLoadException e = assertThrows(PackLoadException.class,
			() -> LOADER.parseJson(packWith(
				"\"conditions\":[{\"type\":\"moon_phase\"}]", ANIM_TRIGGER), "cond2.zip"));
		assertTrue(e.getMessage().contains("unknown condition type 'moon_phase'"), e.getMessage());
	}

	@Test
	void emptyBossesArrayRejected()
	{
		String json = "{\"schemaVersion\":\"1.0\",\"metadata\":{\"packId\":\"e\",\"name\":\"E\",\"version\":\"1.0.0\",\"gameVersion\":\"x\"},\"bosses\":[]}";
		PackLoadException e = assertThrows(PackLoadException.class,
			() -> LOADER.parseJson(json, "empty.zip"));
		assertTrue(e.getMessage().contains("at least one boss"), e.getMessage());
	}

	@Test
	void missingAudioFileInZipRejected()
		throws Exception
	{
		// strip the wav entry: rewrite the fixture without audio/
		Path stripped = tempDir.resolve("no_audio.zip");
		try (java.util.zip.ZipFile src = new java.util.zip.ZipFile(tempDir.resolve("valid_pack.zip").toFile());
			java.util.zip.ZipOutputStream out = new java.util.zip.ZipOutputStream(Files.newOutputStream(stripped)))
		{
			java.util.Enumeration<? extends java.util.zip.ZipEntry> entries = src.entries();
			while (entries.hasMoreElements())
			{
				java.util.zip.ZipEntry entry = entries.nextElement();
				if (entry.getName().startsWith("audio/"))
				{
					continue;
				}
				out.putNextEntry(new java.util.zip.ZipEntry(entry.getName()));
				out.write(src.getInputStream(entry).readAllBytes());
				out.closeEntry();
			}
		}
		PackLoadException e = assertThrows(PackLoadException.class,
			() -> LOADER.loadZip(stripped));
		assertTrue(e.getMessage().contains("pray_ranged.wav"), e.getMessage());
		assertTrue(e.getMessage().contains("missing from the pack"), e.getMessage());
	}

	// ---- helpers ----
}
