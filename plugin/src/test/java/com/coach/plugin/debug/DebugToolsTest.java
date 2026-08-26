package com.coach.plugin.debug;

import com.coach.plugin.model.PlayerState;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.coach.plugin.encounter.ActiveEncounter;
import com.coach.plugin.encounter.Bosses;
import com.coach.plugin.logging.LogBuffer;
import com.coach.plugin.model.PlayerState;
import com.coach.plugin.trigger.TriggerFire;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DebugToolsTest
{
	@TempDir
	Path tempDir;

	// ---- TriggerHistory ----

	@Test
	void triggerHistoryCapsAndOrdersNewestFirst()
	{
		TriggerHistory history = new TriggerHistory();
		for (int i = 1; i <= TriggerHistory.MAX_ENTRIES + 5; i++)
		{
			history.record(new TriggerFire(i, "boss", "ctx" + i, "desc" + i));
		}

		assertEquals(TriggerHistory.MAX_ENTRIES, history.size());
		List<TriggerHistory.Entry> newest = history.filter(null, 3);
		assertEquals("ctx" + (TriggerHistory.MAX_ENTRIES + 5), newest.get(0).contextId);
	}

	@Test
	void triggerHistoryFiltersBySubstring()
	{
		TriggerHistory history = new TriggerHistory();
		history.record(new TriggerFire(1, "nex", "smash", "animation"));
		history.record(new TriggerFire(2, "inferno", "ranger_attack", "animation"));

		assertEquals(2, history.filter(null, 10).size());
		assertEquals(1, history.filter("nex", 10).size());
		assertEquals(1, history.filter("ranger", 10).size());
		assertTrue(history.format("nomatch", 10).get(0).startsWith("(no trigger fires matching"));
	}

	// ---- EventTimeline ----

	@Test
	void timelineAggregatesPerTickWithMeta()
	{
		EventTimeline timeline = new EventTimeline();
		timeline.recordTick(100, java.util.Map.of("ANIMATION_CHANGED", 2), 1, 0);
		timeline.recordTick(101, java.util.Map.of("TICK", 1), 0, 2);
		timeline.recordTick(102, null, 0, 0); // meta-only update creates the entry

		var lines = timeline.format(10);
		assertEquals(3, lines.size());
		assertTrue(lines.get(0).contains("t100: 2ev ANIMATION_CHANGED=2 | trig=1 call=0"), lines.get(0));
		assertTrue(lines.get(1).contains("call=2"), lines.get(1));
	}

	@Test
	void timelineMergesMetaIntoExistingTick()
	{
		EventTimeline timeline = new EventTimeline();
		timeline.recordTick(50, java.util.Map.of("TICK", 1), 0, 0);
		timeline.recordTick(50, null, 3, 1); // same tick: triggers/callouts arrive later

		var lines = timeline.format(5);
		assertTrue(lines.get(0).contains("trig=3 call=1"), lines.get(0));
		assertEquals(1, timeline.recent(10).size(), "same tick merges, not duplicates");
	}

	@Test
	void timelineRingsAtMaxTicks()
	{
		EventTimeline timeline = new EventTimeline();
		for (int i = 1; i <= EventTimeline.MAX_TICKS + 10; i++)
		{
			timeline.recordTick(i, java.util.Map.of("TICK", 1), 0, 0);
		}
		var recent = timeline.recent(EventTimeline.MAX_TICKS);
		assertEquals(EventTimeline.MAX_TICKS, recent.size());
		assertEquals(EventTimeline.MAX_TICKS + 10, recent.get(recent.size() - 1).tick,
			"oldest entries dropped");
	}

	// ---- StateInspector ----

	@Test
	void stateInspectorFormatsPlayerAndSessions()
	{
		StateInspector inspector = new StateInspector();
		inspector.update(new PlayerState(72, 99, 3222, 3218, 0, -1),
			List.of(new ActiveEncounter(
				Bosses.threePhase(), 11278, "p2", 500)));

		var lines = inspector.format();
		assertTrue(lines.get(0).contains("hp 72/99"), lines.get(0));
		assertTrue(lines.get(0).contains("pos(3222,3218,0)"), lines.get(0));
		assertTrue(lines.stream().anyMatch(l -> l.contains("boss=b phase=p2 phaseTick=")), lines.toString());
	}

	// ---- LogExporter ----

	@Test
	void exporterWritesValidJsonBundle() throws Exception
	{
		LogBuffer logBuffer = new LogBuffer();
		logBuffer.log("t1 ANIMATION_CHANGED test");
		TriggerHistory history = new TriggerHistory();
		history.record(new TriggerFire(7, "nex", "smash", "anim 8960"));
		EventTimeline timeline = new EventTimeline();
		timeline.recordTick(7, java.util.Map.of("TICK", 1), 1, 0);
		StateInspector inspector = new StateInspector();
		inspector.update(new PlayerState(50, 99, 1, 2, 0, -1), List.of());

		Path dir = tempDir.resolve("debug_logs");
		Path written = new LogExporter(dir).export(inspector, history, timeline, logBuffer);

		assertNotNull(written);
		assertTrue(written.startsWith(dir));
		String json = Files.readString(written);
		JsonObject root = new Gson().fromJson(json, JsonObject.class);
		assertEquals("50", root.getAsJsonObject("state").get("playerHp").getAsString());
		assertEquals(1, root.getAsJsonArray("triggerHistory").size());
		assertEquals(1, root.getAsJsonArray("eventTimeline").size());
		assertTrue(root.getAsJsonArray("logLines").size() >= 1);
	}
}
