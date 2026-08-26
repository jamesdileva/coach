package com.coach.plugin.encounter;

import com.coach.plugin.encounter.model.EncounterPack;
import com.coach.plugin.encounter.model.EncounterPack;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Guards the real Sotetseg pack: loads, validates, and checks structure.
 */
class TobSotetsegPackTest
{
	@TempDir
	static Path tempDir;

	static final String SOTE_ZIP = "../encounter-packs/tob_sotetseg_1.0.0.zip";

	@BeforeAll
	static void requirePack() throws Exception
	{
		assumeTrue(Files.exists(Path.of(SOTE_ZIP)), "tob_sotetseg_1.0.0.zip not built yet");
	}

	private static EncounterPack load() throws Exception
	{
		Path zip = tempDir.resolve("tob_sotetseg_1.0.0.zip");
		try (InputStream in = Files.newInputStream(Path.of(SOTE_ZIP)))
		{
			Files.copy(in, zip, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
		}
		return new EncounterLoader().loadZip(zip);
	}

	@Test
	void packLoadsAndValidates() throws Exception
	{
		assertEquals("tob_sotetseg", load().metadata.packId);
	}

	@Test
	void coversBothNpcVariantsAndMazeThresholds() throws Exception
	{
		EncounterPack pack = load();
		var phase = pack.bosses.get(0).phases.get(0);

		assertEquals(java.util.List.of(8337, 8388),
			phase.entryTrigger.npcIds, "both scaling variants covered");

		var mazeThresholds = phase.mechanics.stream()
			.filter(m -> m.mechanicId.startsWith("maze"))
			.map(m -> m.triggers.get(0).hpThreshold)
			.sorted()
			.collect(java.util.stream.Collectors.toList());
		assertEquals(java.util.List.of(34, 67), mazeThresholds, "maze warnings at ~33.3% and ~66.6%");
	}
}
