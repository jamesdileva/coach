package com.coach.plugin.encounter;

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
 * Loads the REAL Inferno encounter pack and guards structure:
 * 69 phases (68 waves + Zuk), per-wave attack mechanics, wave_cleared exits.
 */
class InfernoPackTest
{
	@TempDir
	static Path tempDir;

	static final String INFERNO_ZIP = "../encounter-packs/inferno_1.0.0.zip";

	@BeforeAll
	static void requirePack() throws Exception
	{
		assumeTrue(Files.exists(Path.of(INFERNO_ZIP)), "inferno_1.0.0.zip not built yet");
	}

	private static EncounterPack load() throws Exception
	{
		Path zip = tempDir.resolve("inferno_1.0.0.zip");
		try (InputStream in = Files.newInputStream(Path.of(INFERNO_ZIP)))
		{
			Files.copy(in, zip, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
		}
		return new EncounterLoader().loadZip(zip);
	}

	@Test
	void packLoadsAndValidates() throws Exception
	{
		assertEquals("inferno", load().metadata.packId);
	}

	@Test
	void allSixtyNinePhasesWithTerminalZuk() throws Exception
	{
		var phases = load().bosses.get(0).phases;
		assertEquals(69, phases.size());
		assertEquals("wave_1", phases.get(0).phaseId);
		assertEquals("wave_68", phases.get(67).phaseId);
		assertEquals("zuk", phases.get(68).phaseId);
		assertTrue(phases.get(68).exitTriggers.isEmpty(), "Zuk is terminal");
		for (int i = 0; i < 68; i++)
		{
			assertEquals("wave_cleared", phases.get(i).exitTriggers.get(0).type,
				"wave " + (i + 1) + " must exit via wave_cleared");
		}
	}

	@Test
	void blobWavesIncludeSplitIdsInClearSets() throws Exception
	{
		var phases = load().bosses.get(0).phases;
		var wave5ClearIds = phases.get(4).exitTriggers.get(0).npcIds;
		assertTrue(wave5ClearIds.contains(7703), "blob split melee id tracked");
		assertTrue(wave5ClearIds.contains(7704), "blob split ranged id tracked");

		var wave9ClearIds = phases.get(8).exitTriggers.get(0).npcIds;
		assertTrue(!wave9ClearIds.contains(7703), "no splits expected without blobs");
	}

	@Test
	void jadWavesCarryJadPrayerMechanicsAndHealers() throws Exception
	{
		var phases = load().bosses.get(0).phases;
		var wave67 = phases.get(66);

		boolean hasJadMagicTrigger = false;
		boolean hasHealerMechanic = false;
		for (var mechanic : wave67.mechanics)
		{
			if ("jad_attack".equals(mechanic.mechanicId))
			{
				hasJadMagicTrigger = true;
				assertEquals(3, mechanic.triggers.size(), "jad magic/ranged/melee triggers");
			}
			if ("jad_healers".equals(mechanic.mechanicId))
			{
				hasHealerMechanic = true;
			}
		}
		assertTrue(hasJadMagicTrigger, "wave 67 needs jad prayer callouts");
		assertTrue(hasHealerMechanic, "wave 67 needs healer spawn alert");
	}
}
