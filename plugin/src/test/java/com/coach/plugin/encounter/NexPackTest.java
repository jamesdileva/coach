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
 * Loads the REAL Nex encounter pack from the repo's encounter-packs/ directory.
 * Guards the pack against regressions: must always parse, validate, and
 * contain the full five-phase structure with shout coverage.
 */
class NexPackTest
{
	@TempDir
	static Path tempDir;

	static final String NEX_ZIP = "../encounter-packs/nex_1.0.0.zip";

	private static Path extractNexZip() throws Exception
	{
		Path zip = tempDir.resolve("nex_1.0.0.zip");
		try (InputStream in = Files.newInputStream(Path.of(NEX_ZIP)))
		{
			Files.copy(in, zip, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
		}
		return zip;
	}

	@BeforeAll
	static void requireNexPack() throws Exception
	{
		assumeTrue(Files.exists(Path.of(NEX_ZIP)),
			"nex_1.0.0.zip not built yet — run the packaging step");
	}

	@Test
	void nexPackLoadsAndValidates() throws Exception
	{
		EncounterPack pack = new EncounterLoader().loadZip(extractNexZip());

		assertEquals("nex", pack.metadata.packId);
		assertEquals("1.0", pack.schemaVersion);
		assertEquals(11278, pack.bosses.get(0).npcId);
	}

	@Test
	void allFivePhasesPresentInOrder() throws Exception
	{
		EncounterPack pack = new EncounterLoader().loadZip(extractNexZip());

		var phases = pack.bosses.get(0).phases;
		assertEquals(5, phases.size());
		assertEquals("smoke", phases.get(0).phaseId);
		assertEquals("shadow", phases.get(1).phaseId);
		assertEquals("blood", phases.get(2).phaseId);
		assertEquals("ice", phases.get(3).phaseId);
		assertEquals("zaros", phases.get(4).phaseId);

		// HP-threshold exits chain the four elemental phases; Zaros is terminal
		assertEquals(80, phases.get(0).exitTriggers.get(0).hpThreshold);
		assertEquals(60, phases.get(1).exitTriggers.get(0).hpThreshold);
		assertEquals(40, phases.get(2).exitTriggers.get(0).hpThreshold);
		assertEquals(20, phases.get(3).exitTriggers.get(0).hpThreshold);
		assertTrue(phases.get(4).exitTriggers.isEmpty(), "Zaros is terminal");
	}

	@Test
	void everySpecialHasShoutCoverageAndAudio() throws Exception
	{
		EncounterPack pack = new EncounterLoader().loadZip(extractNexZip());

		var expectedShouts = new String[] {
			"Let the virus flow through you", "no escape",
			"Darken my shadow", "Fear the shadow", "Embrace darkness",
			"Flood my lungs with blood", "A siphon will solve this",
			"I demand a blood sacrifice",
			"power of ice", "Contain this", "prison of ice",
			"power of zaros"
		};

		java.util.Set<String> foundShouts = new java.util.HashSet<>();
		int calloutsWithAudio = 0;
		int totalCallouts = 0;
		for (var boss : pack.bosses)
		{
			for (var phase : boss.phases)
			{
				for (var mechanic : phase.mechanics)
				{
					for (var trigger : mechanic.triggers)
					{
						if ("shout".equals(trigger.type))
						{
							foundShouts.add(trigger.containsText);
						}
					}
					for (var callout : mechanic.callouts)
					{
						totalCallouts++;
						if (callout.audioFile != null)
						{
							calloutsWithAudio++;
						}
					}
				}
			}
		}

		for (String shout : expectedShouts)
		{
			assertTrue(foundShouts.stream().anyMatch(s -> s.contains(shout)),
				"missing shout coverage for: " + shout);
		}
		assertEquals(totalCallouts, calloutsWithAudio, "rule 5/11: every callout ships audio");
	}
}
