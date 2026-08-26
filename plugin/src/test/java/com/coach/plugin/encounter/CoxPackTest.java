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
 * Sprint 16: CoX pack + community template guards.
 */
class CoxPackTest
{
	@TempDir
	static Path tempDir;

	static final String COX_ZIP = "../encounter-packs/cox_1.0.0.zip";
	static final String TEMPLATE = "../encounter-packs/template.pack/encounter.json";

	@BeforeAll
	static void requireArtifacts() throws Exception
	{
		assumeTrue(Files.exists(Path.of(COX_ZIP)), "cox_1.0.0.zip not built yet");
	}

	private static EncounterPack loadZip(String name) throws Exception
	{
		Path zip = tempDir.resolve(name);
		try (InputStream in = Files.newInputStream(Path.of("../encounter-packs/" + name)))
		{
			Files.copy(in, zip, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
		}
		return new EncounterLoader().loadZip(zip);
	}

	@Test
	void coxPackLoadsAndValidates() throws Exception
	{
		assertEquals("cox", loadZip("cox_1.0.0.zip").metadata.packId);
	}

	@Test
	void coxCoversFourKeyRooms() throws Exception
	{
		var bossIds = loadZip("cox_1.0.0.zip").bosses.stream()
			.map(b -> b.bossId)
			.collect(java.util.stream.Collectors.toSet());
		assertEquals(java.util.Set.of("vanguards", "tekton", "vasa", "olm"), bossIds);
	}

	@Test
	void templatePackDemonstratesEveryTriggerTypeAndLoads() throws Exception
	{
		Path json = tempDir.resolve("template_encounter.json");
		try (InputStream in = Files.newInputStream(Path.of(TEMPLATE)))
		{
			Files.copy(in, json, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
		}
		EncounterPack pack = new EncounterLoader().parseJson(
			Files.readString(json), "template.pack/encounter.json");

		var usedTypes = new java.util.HashSet<String>();
		for (var boss : pack.bosses)
		{
			for (var phase : boss.phases)
			{
				if (phase.entryTrigger != null)
				{
					usedTypes.add(phase.entryTrigger.type);
				}
				if (phase.exitTriggers != null)
				{
					for (var exit : phase.exitTriggers)
					{
						usedTypes.add(exit.type);
					}
				}
				for (var mechanic : phase.mechanics)
				{
					for (var trigger : mechanic.triggers)
					{
						usedTypes.add(trigger.type);
					}
				}
			}
		}

		for (String type : new String[] {
			"animation", "projectile", "graphic", "npc_spawn", "npc_despawn",
			"hp", "tick_timer", "player_state", "location", "shout",
			"wave_cleared", "composite"})
		{
			assertTrue(usedTypes.contains(type), "template must demonstrate: " + type);
		}
	}
}
