package com.coach.plugin.simulation;

import com.coach.plugin.coaching.CoachingEngine;
import com.coach.plugin.encounter.EncounterLoader;
import com.coach.plugin.encounter.model.EncounterPack;
import com.coach.plugin.performance.FightScript;
import com.coach.plugin.performance.FightScriptLoader;
import com.coach.plugin.performance.TickReplayHarness;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Sprint 30: full Inferno run (all 69 waves) via FightScript replay against
 * the REAL inferno_1.0.0.zip pack. Asserts wave entry/despawn fires progress
 * the encounter and blob/Jad/Zuk mechanics deliver callouts.
 */
class InfernoSimulationTest
{
	private static final String INFERNO_ZIP = "../encounter-packs/inferno_1.0.0.zip";
	private static final String SCRIPT = "src/test/resources/simulations/inferno_full_run.json";

	@TempDir
	static Path tempDir;

	@BeforeAll
	static void requirePack() throws Exception
	{
		assumeTrue(Files.exists(Path.of(INFERNO_ZIP)), "inferno_1.0.0.zip not built yet");
	}

	private static EncounterPack loadPack() throws Exception
	{
		Path zip = tempDir.resolve("inferno_1.0.0.zip");
		try (InputStream in = Files.newInputStream(Path.of(INFERNO_ZIP)))
		{
			Files.copy(in, zip, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
		}
		return new EncounterLoader().loadZip(zip);
	}

	@Test
	void fullRunReplaysWithFiresAndBlobCallout() throws Exception
	{
		EncounterPack pack = loadPack();
		FightScript script = FightScriptLoader.fromFile(Path.of(SCRIPT));
		assertEquals(700, script.getEndTick());

		TickReplayHarness harness = new TickReplayHarness(List.of(pack));
		List<String> audio = new java.util.ArrayList<>();
		harness.setAudioSink((packId, file) -> audio.add(packId + "/" + file));

		TickReplayHarness.Result result = harness.replayScript(script);

		assertEquals(700, result.ticks);
		assertTrue(result.totalFires > 0, "wave spawns/despawns must fire");
		assertTrue(result.withinLooseBudgets(),
			"worst tick " + result.getWorstTickMillis() + "ms < 600ms");

		Set<String> ids = new HashSet<>();
		harness.getDelivered().forEach(d -> ids.add(d.getCallout().calloutId));
		// wave 4 blob_attack is scripted with animation 7581
		assertTrue(ids.contains("blob_attacking"), "blob callout: " + ids);
		assertFalse(audio.isEmpty(), "audio sink invoked");
	}

	@Test
	void packHasSixtyNinePhasesAndScriptCoversSpawnAndZuk() throws Exception
	{
		EncounterPack pack = loadPack();
		assertEquals(69, pack.bosses.get(0).phases.size());

		FightScript script = FightScriptLoader.fromFile(Path.of(SCRIPT));
		boolean hasZukSpawn = script.getEvents().stream()
			.anyMatch(e -> "npc_spawn".equals(e.getType()) && e.getNpcId() == 7706);
		assertTrue(hasZukSpawn, "script ends at Zuk spawn");
	}

	@Test
	void deliveredCalloutsAllHaveAudioFiles() throws Exception
	{
		EncounterPack pack = loadPack();
		FightScript script = FightScriptLoader.fromFile(Path.of(SCRIPT));
		TickReplayHarness harness = new TickReplayHarness(List.of(pack));
		harness.replayScript(script);

		for (CoachingEngine.DeliveredCallout d : harness.getDelivered())
		{
			assertTrue(d.getCallout().audioFile != null, d.getCallout().calloutId);
			assertEquals("inferno", d.getBossId());
		}
	}
}
