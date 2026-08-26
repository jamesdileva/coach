package com.coach.plugin.encounter;

import com.coach.plugin.encounter.model.EncounterPack;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EncounterEngineTest
{
	private static final String VALID_PACK = "{"
		+ "\"schemaVersion\": \"1.0\","
		+ "\"metadata\": {\"packId\": \"golem\", \"name\": \"Golem\", \"version\": \"1.0.0\", \"gameVersion\": \"2026-08\"},"
		+ "\"bosses\": [{\"bossId\": \"golem\", \"name\": \"Grotesque Guardian? no — test golem\", \"npcId\": 7878,"
		+ "\"phases\": [{\"phaseId\": \"p1\", \"name\": \"P1\", \"entryTrigger\": {\"type\": \"npc_spawn\", \"npcId\": 7878}}]}]}";

	@TempDir
	Path tempDir;

	private EncounterEngine engine;

	@BeforeEach
	void setUp()
	{
		engine = new EncounterEngine();
	}

	private Path writeZip(String name, String encounterJson) throws Exception
	{
		Path zip = tempDir.resolve(name);
		try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip.toFile())))
		{
			if (encounterJson != null)
			{
				out.putNextEntry(new ZipEntry(EncounterLoader.PACK_ENTRY));
				out.write(encounterJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
				out.closeEntry();
			}
		}
		return zip;
	}

	@Test
	void loadsValidZipFromDirectory() throws Exception
	{
		writeZip("golem.pack.zip", VALID_PACK);

		int count = engine.loadPacks(tempDir);

		assertEquals(1, count);
		Optional<com.coach.plugin.encounter.model.BossDefinition> boss =
			engine.getBossForNpcId(7878);
		assertTrue(boss.isPresent());
		assertEquals("golem", boss.get().bossId);
	}

	@Test
	void invalidZipsAreSkippedNotFatal() throws Exception
	{
		writeZip("broken.zip", "{not json");
		writeZip("nomanifest.zip", null);
		writeZip("good.zip", VALID_PACK);

		int count = engine.loadPacks(tempDir);

		assertEquals(1, count);
	}

	@Test
	void reloadReplacesPreviousPacks() throws Exception
	{
		writeZip("golem.pack.zip", VALID_PACK);
		assertEquals(1, engine.loadPacks(tempDir));

		Files.delete(tempDir.resolve("golem.pack.zip"));

		assertEquals(0, engine.loadPacks(tempDir));
		assertTrue(engine.getPacks().isEmpty());
	}

	@Test
	void missingDirectoryLoadsNothing()
	{
		assertEquals(0, engine.loadPacks(tempDir.resolve("does-not-exist")));
		assertTrue(engine.getPacks().isEmpty());
	}

	@Test
	void unknownNpcIdReturnsEmpty() throws Exception
	{
		writeZip("golem.pack.zip", VALID_PACK);
		engine.loadPacks(tempDir);

		assertTrue(engine.getBossForNpcId(9999).isEmpty());
	}
}
