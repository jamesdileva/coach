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
import java.util.ArrayList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Sprint 30: replay the scripted Nex fight through the production pipeline
 * against the REAL nex_1.0.0.zip pack. Asserts shout-driven callouts fire
 * on expected ticks with audio + visuals.
 *
 * Note: HP-based phase exits need a live Client; headless replay therefore
 * stays in smoke-phase mechanics for shout callouts (documented deviation).
 */
class NexSimulationTest
{
	private static final String NEX_ZIP = "../encounter-packs/nex_1.0.0.zip";
	private static final String SCRIPT = "src/test/resources/simulations/nex_full_fight.json";

	@TempDir
	static Path tempDir;

	@BeforeAll
	static void requirePack() throws Exception
	{
		assumeTrue(Files.exists(Path.of(NEX_ZIP)), "nex_1.0.0.zip not built yet");
	}

	private static EncounterPack loadPack() throws Exception
	{
		Path zip = tempDir.resolve("nex_1.0.0.zip");
		try (InputStream in = Files.newInputStream(Path.of(NEX_ZIP)))
		{
			Files.copy(in, zip, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
		}
		return new EncounterLoader().loadZip(zip);
	}

	@Test
	void fixtureLoadsAndReplays() throws Exception
	{
		EncounterPack pack = loadPack();
		FightScript script = FightScriptLoader.fromFile(Path.of(SCRIPT));
		assertTrue(script.getEvents().size() >= 10, "script has events");
		assertEquals(120, script.getEndTick());

		TickReplayHarness harness = new TickReplayHarness(List.of(pack));
		List<String> audio = new java.util.ArrayList<>();
		harness.setAudioSink((packId, file) -> audio.add(packId + "/" + file));

		TickReplayHarness.Result result = harness.replayScript(script);

		assertEquals(120, result.ticks);
		assertTrue(result.totalFires > 0, "spawn + shouts must fire");
		assertTrue(result.totalActivations > 0, "smoke mechanics must activate");
		assertTrue(result.totalCallouts > 0, "callouts must deliver");

		Set<String> calloutIds = new HashSet<>();
		for (CoachingEngine.DeliveredCallout d : harness.getDelivered())
		{
			calloutIds.add(d.getCallout().calloutId);
			assertEquals("nex", d.getBossId());
			assertTrue(d.getCallout().audioFile != null, "audio present");
		}
		assertTrue(calloutIds.contains("smoke_start_pray"), "spawn callout: " + calloutIds);
		assertTrue(calloutIds.contains("choke_callout"), "choke: " + calloutIds);
		assertTrue(calloutIds.contains("dash_callout"), "dash: " + calloutIds);
		assertFalse(audio.isEmpty(), "audio sink invoked: " + audio);
		assertTrue(audio.stream().allMatch(a -> a.startsWith("nex/")));
	}

	@Test
	void allShoutTriggersInSmokePhaseDeliverTheirCallouts() throws Exception
	{
		EncounterPack pack = loadPack();
		FightScript script = FightScriptLoader.fromFile(Path.of(SCRIPT));
		TickReplayHarness harness = new TickReplayHarness(List.of(pack));
		harness.replayScript(script);

		Set<String> ids = new HashSet<>();
		harness.getDelivered().forEach(d -> ids.add(d.getCallout().calloutId));
		// Smoke-phase shouts in the fixture (HP exits can't advance headless)
		assertTrue(ids.contains("choke_callout"));
		assertTrue(ids.contains("dash_callout"));
		assertTrue(ids.contains("smoke_start_pray"));
	}
}
