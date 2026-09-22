package com.coach.plugin.performance;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TickReplayHarnessTest
{
	private static final String PACK_JSON = "{"
		+ "\"schemaVersion\": \"1.0\","
		+ "\"metadata\": {\"packId\": \"t\", \"name\": \"T\", \"version\": \"1.0.0\", \"gameVersion\": \"x\"},"
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
		+ "      \"category\": \"critical\","
		+ "      \"priority\": 90,"
		+ "      \"audioOffset\": 0,"
		+ "      \"visualOffset\": 0"
		+ "    }]"
		+ "  }]"
		+ "}]}"
		+ "]}";

	private static TickReplayHarness harness() throws Exception
	{
		return new TickReplayHarness(TickReplayHarness.fromJson(PACK_JSON));
	}

	@Test
	void fromJsonParsesFixturePack() throws Exception
	{
		var packs = TickReplayHarness.fromJson(PACK_JSON);
		assertEquals(1, packs.size());
		assertEquals("nex", packs.get(0).bosses.get(0).bossId);
	}

	@Test
	void replayProducesFiresActivationsAndCallouts() throws Exception
	{
		TickReplayHarness h = harness();
		TickReplayHarness.Result result = h.replay(120, 2);

		assertEquals(120, result.ticks);
		assertTrue(result.totalFires > 0, "triggers must fire");
		assertTrue(result.totalActivations > 0, "mechanic must activate");
		assertTrue(result.totalCallouts > 0, "callout must deliver");
	}

	@Test
	void syntheticTickStaysWellUnderLooseBudget() throws Exception
	{
		TickReplayHarness h = harness();
		h.replay(50, 0); // JIT warm-up
		TickReplayHarness.Result result = h.replay(500, 5);

		assertTrue(result.withinLooseBudgets(),
			"worst tick " + result.getWorstTickMillis() + "ms must be < 600ms");
	}

	@Test
	void noiseDoesNotCreateMatchesForUninterestedTypes() throws Exception
	{
		TickReplayHarness h = harness();
		TickReplayHarness.Result quiet = h.replay(60, 0);
		TickReplayHarness.Result noisy = h.replay(60, 10);
		// same synthetic boss schedule — noise must not change fire count
		assertEquals(quiet.totalFires, noisy.totalFires,
			"VARBIT noise must not produce extra trigger evaluations/fires");
	}

	@Test
	void profilerAndMemoryMonitorsAreExposed()
	{
		TickReplayHarness h = null;
		try
		{
			h = harness();
		}
		catch (Exception e)
		{
			throw new AssertionError(e);
		}
		assertNotNull(h.getProfiler());
		assertNotNull(h.getMemoryMonitor());
		h.replay(10, 0);
		assertNotNull(h.getProfiler().getLast());
	}

	@Test
	void resultTracksMemoryAndComponents()
	{
		TickReplayHarness h = null;
		TickReplayHarness.Result result;
		try
		{
			h = harness();
			result = h.replay(100, 3);
		}
		catch (Exception e)
		{
			throw new AssertionError(e);
		}
		assertNotNull(result.maxComponentNanos);
		// worst tick wall time recorded; components observed
		assertTrue(result.getWorstTickMillis() >= 0L);
		// growth may be negative after GC — only assert the sample path ran
		assertTrue(h.getMemoryMonitor().getSamples() > 0);
	}
}
