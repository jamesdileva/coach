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
 * Guards the real ToA pack: five bosses, HP-threshold mechanic coverage.
 */
class ToaPackTest
{
	@TempDir
	static Path tempDir;

	static final String TOA_ZIP = "../encounter-packs/toa_1.0.0.zip";

	@BeforeAll
	static void requirePack() throws Exception
	{
		assumeTrue(Files.exists(Path.of(TOA_ZIP)), "toa_1.0.0.zip not built yet");
	}

	private static EncounterPack load() throws Exception
	{
		Path zip = tempDir.resolve("toa_1.0.0.zip");
		try (InputStream in = Files.newInputStream(Path.of(TOA_ZIP)))
		{
			Files.copy(in, zip, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
		}
		return new EncounterLoader().loadZip(zip);
	}

	@Test
	void packLoadsAndValidates() throws Exception
	{
		assertEquals("toa", load().metadata.packId);
	}

	@Test
	void coversAllFiveBosses() throws Exception
	{
		var bossIds = load().bosses.stream()
			.map(b -> b.bossId)
			.collect(java.util.stream.Collectors.toSet());

		assertEquals(java.util.Set.of("zebak", "akkha", "baba", "kephri", "wardens"),
			bossIds);
	}

	@Test
	void zebakSpecialsAndEnrageThresholdsMatchWiki() throws Exception
	{
		var zebak = load().bosses.stream()
			.filter(b -> b.bossId.equals("zebak"))
			.findFirst().orElseThrow();

		var thresholds = zebak.phases.get(0).mechanics.stream()
			.filter(m -> m.mechanicId.startsWith("zebak_special"))
			.map(m -> m.triggers.get(0).hpThreshold)
			.sorted()
			.collect(java.util.stream.Collectors.toList());
		assertEquals(java.util.List.of(40, 55, 70, 85), thresholds);

		var enrage = zebak.phases.get(0).mechanics.stream()
			.filter(m -> m.mechanicId.equals("zebak_enrage"))
			.findFirst().orElseThrow();
		assertEquals(25, enrage.triggers.get(0).hpThreshold);
	}

	@Test
	void akkhaShadowPhasesAtWikiThresholds() throws Exception
	{
		var akkha = load().bosses.stream()
			.filter(b -> b.bossId.equals("akkha"))
			.findFirst().orElseThrow();

		var thresholds = akkha.phases.get(0).mechanics.stream()
			.filter(m -> m.mechanicId.startsWith("akkha_shadows"))
			.map(m -> m.triggers.get(0).hpThreshold)
			.sorted()
			.collect(java.util.stream.Collectors.toList());
		assertEquals(java.util.List.of(20, 40, 60, 80), thresholds);
	}
}
