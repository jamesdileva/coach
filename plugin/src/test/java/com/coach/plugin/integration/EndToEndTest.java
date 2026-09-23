package com.coach.plugin.integration;

import com.coach.plugin.coaching.CoachingEngine;
import com.coach.plugin.encounter.model.EncounterPack;
import com.coach.plugin.performance.TickReplayHarness;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sprint 30 integration: event → trigger → encounter → callout → audio + visual.
 * Uses a minimal pack (same shape as TickReplayHarnessTest) driven through
 * the synthetic schedule in TickReplayHarness.replay().
 */
class EndToEndTest
{
	private static final String PACK_JSON = "{"
		+ "\"schemaVersion\": \"1.0\","
		+ "\"metadata\": {\"packId\": \"e2e\", \"name\": \"E2E\", \"version\": \"1.0.0\", \"gameVersion\": \"x\"},"
		+ "\"bosses\": [{"
		+ "\"bossId\": \"nex\", \"name\": \"Nex\", \"npcId\": 11278,"
		+ "\"phases\": [{"
		+ "  \"phaseId\": \"p1\", \"name\": \"P1\","
		+ "  \"entryTrigger\": {\"type\": \"npc_spawn\", \"npcId\": 11278},"
		+ "  \"mechanics\": [{"
		+ "    \"mechanicId\": \"shadow_smash\", \"name\": \"Shadow Smash\","
		+ "    \"triggers\": ["
		+ "      {\"triggerId\": \"a1\", \"type\": \"animation\", \"npcId\": 11278, \"animationId\": 8960},"
		+ "      {\"triggerId\": \"p1\", \"type\": \"projectile\", \"projectId\": 2955}"
		+ "    ],"
		+ "    \"callouts\": [{"
		+ "      \"calloutId\": \"smash_callout\","
		+ "      \"text\": \"Smash!\","
		+ "      \"audioFile\": \"smash.ogg\","
		+ "      \"category\": \"critical\","
		+ "      \"priority\": 90,"
		+ "      \"audioOffset\": 0,"
		+ "      \"visualOffset\": 0,"
		+ "      \"visual\": {\"type\": \"text\", \"durationTicks\": 4}"
		+ "    }]"
		+ "  }]"
		+ "}]}"
		+ "]}";

	@Test
	void fullPipelineDeliversCalloutWithAudioAndVisual() throws Exception
	{
		List<EncounterPack> packs = TickReplayHarness.fromJson(PACK_JSON);
		TickReplayHarness harness = new TickReplayHarness(packs);
		List<String> audio = new ArrayList<>();
		harness.setAudioSink((packId, file) -> audio.add(packId + "/" + file));

		TickReplayHarness.Result result = harness.replay(60, 0);

		assertTrue(result.totalFires > 0, "triggers must fire");
		assertTrue(result.totalActivations > 0, "mechanic must activate");
		assertTrue(result.totalCallouts > 0, "callout must deliver");
		assertFalse(harness.getDelivered().isEmpty());
		assertEquals("nex", harness.getDelivered().get(0).getBossId());

		CoachingEngine.DeliveredCallout delivery = harness.getDelivered().get(0);
		assertEquals("smash_callout", delivery.getCallout().calloutId);
		assertEquals("Smash!", delivery.getCallout().text);
		assertTrue(audio.contains("e2e/smash.ogg"), "audio sink must fire: " + audio);

		assertFalse(harness.getVisuals().isEmpty(), "visual overlay must show");
		assertEquals("Smash!", harness.getVisuals().get(0).text);
	}

	@Test
	void calloutsCarryAudioFileAndCategoryForRuleChecks() throws Exception
	{
		List<EncounterPack> packs = TickReplayHarness.fromJson(PACK_JSON);
		TickReplayHarness harness = new TickReplayHarness(packs);
		List<String> audio = new ArrayList<>();
		harness.setAudioSink((p, f) -> audio.add(f));
		harness.replay(60, 0);

		for (CoachingEngine.DeliveredCallout d : harness.getDelivered())
		{
			assertTrue(d.getCallout().audioFile != null, "every delivered callout has audio (rule 5)");
			assertEquals("critical", d.getCallout().category);
		}
		assertFalse(audio.isEmpty());
	}
}
