package com.coach.plugin.encounter;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PackManagerTest
{
	@TempDir
	Path tempDir;

	private final EncounterLoader loader = new EncounterLoader();
	private PackManager manager;

	@BeforeEach
	void setUp()
	{
		manager = new PackManager(loader);
	}

	private static String pack(String packId, String bossId, int npcId, String... dependencies)
	{
		StringBuilder deps = new StringBuilder();
		if (dependencies.length > 0)
		{
			deps.append(",\"dependencies\":[");
			for (int i = 0; i < dependencies.length; i++)
			{
				if (i > 0)
				{
					deps.append(',');
				}
				deps.append('"').append(dependencies[i]).append('"');
			}
			deps.append(']');
		}
		return "{\"schemaVersion\":\"1.0\","
			+ "\"metadata\":{\"packId\":\"" + packId + "\",\"name\":\"" + packId
			+ "\",\"version\":\"1.0.0\",\"gameVersion\":\"x\"" + deps + "},"
			+ "\"bosses\":[{\"bossId\":\"" + bossId + "\",\"name\":\"" + bossId
			+ "\",\"npcId\":" + npcId + ","
			+ "\"phases\":[{\"phaseId\":\"p\",\"name\":\"P\",\"entryTrigger\":{\"type\":\"npc_spawn\",\"npcId\":" + npcId + "}}]}]}";
	}

	private void writeZip(String fileName, String json) throws Exception
	{
		try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(tempDir.resolve(fileName).toFile())))
		{
			out.putNextEntry(new ZipEntry("encounter.json"));
			out.write(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
			out.closeEntry();
		}
	}

	@Test
	void distinctBossesBothLoadCleanly() throws Exception
	{
		writeZip("a_golem.pack.zip", pack("golempack", "golem", 7878));
		writeZip("b_nex.pack.zip", pack("nexpack", "nex", 11278));

		PackManager.LoadResult result = manager.loadDirectory(tempDir);

		assertEquals(2, result.packs.size());
		assertEquals(2, result.statuses.size());
		assertTrue(result.statuses.stream().allMatch(s -> s.getState() == PackStatus.State.LOADED));
		assertTrue(result.warnings.isEmpty());
	}

	@Test
	void duplicateBossIsConflictFirstPackWins() throws Exception
	{
		writeZip("a_first.pack.zip", pack("first", "shared_boss", 100));
		writeZip("b_second.pack.zip", pack("second", "shared_boss", 100));

		PackManager.LoadResult result = manager.loadDirectory(tempDir);

		assertEquals(1, result.packs.size(), "conflicting pack skipped");
		assertEquals("first", result.packs.get(0).metadata.packId, "alphabetically first wins");
		PackStatus conflict = result.statuses.stream()
			.filter(s -> s.getState() == PackStatus.State.CONFLICT)
			.findFirst().orElseThrow();
		assertEquals("second", conflict.getPackId());
		assertTrue(conflict.getMessage().contains("shared_boss"));
	}

	@Test
	void duplicatePackIdsAreConflicts() throws Exception
	{
		writeZip("a_one.pack.zip", pack("same_id", "boss_a", 1));
		writeZip("b_two.pack.zip", pack("same_id", "boss_b", 2));

		PackManager.LoadResult result = manager.loadDirectory(tempDir);

		assertEquals(1, result.packs.size());
		long conflicts = result.statuses.stream().filter(s -> s.getState() == PackStatus.State.CONFLICT).count();
		assertEquals(1, conflicts);
	}

	@Test
	void satisfiedDependencyProducesNoWarning() throws Exception
	{
		writeZip("base.pack.zip", pack("core_pack", "core_boss", 10));
		writeZip("ext.pack.zip", pack("extension", "extra_boss", 20, "core_pack"));

		PackManager.LoadResult result = manager.loadDirectory(tempDir);

		assertEquals(2, result.packs.size());
		assertTrue(result.warnings.isEmpty());
	}

	@Test
	void missingDependencyReportedAsWarning() throws Exception
	{
		writeZip("ext.pack.zip", pack("extension", "extra_boss", 20, "not_installed"));

		PackManager.LoadResult result = manager.loadDirectory(tempDir);

		assertEquals(1, result.packs.size(), "pack still loads");
		assertEquals(1, result.warnings.size());
		assertTrue(result.warnings.get(0).contains("missing dependency: not_installed"));
	}

	@Test
	void rejectedPacksGetStatusesNotExceptions() throws Exception
	{
		writeZip("broken.zip", "{nope");
		writeZip("good.zip", pack("good", "boss", 5));

		PackManager.LoadResult result = manager.loadDirectory(tempDir);

		assertEquals(1, result.packs.size());
		PackStatus rejected = result.statuses.stream()
			.filter(s -> s.getState() == PackStatus.State.REJECTED)
			.findFirst().orElseThrow();
		assertEquals("broken.zip", rejected.getFileName());
	}

	@Test
	void engineExposesSummaryLinesForOverlay() throws Exception
	{
		writeZip("a.pack.zip", pack("alpha", "bossA", 11));
		EncounterEngine engine = new EncounterEngine(null);
		engine.loadPacks(tempDir);

		List<String> lines = engine.getPackSummaryLines();

		assertEquals(1, lines.size());
		assertTrue(lines.get(0).contains("alpha@1.0.0 [LOADED]"), lines.get(0));
	}
}
