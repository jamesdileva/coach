package com.coach.plugin.debug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.coach.plugin.encounter.ActiveEncounter;
import com.coach.plugin.encounter.model.BossDefinition;
import com.coach.plugin.logging.LogBuffer;
import com.coach.plugin.model.PlayerState;
import com.coach.plugin.trigger.TriggerFire;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DebugHistoryTest
{
	@Test
	void eventTimelineRecordsMergesFormatsAndEvicts()
	{
		EventTimeline timeline = new EventTimeline();
		assertEquals(List.of("(timeline empty)"), timeline.format(10));

		Map<String, Integer> counts = new LinkedHashMap<>();
		counts.put("ANIMATION_CHANGED", 2);
		counts.put("TICK", 1);
		timeline.recordTick(1, counts, 3, 1);
		timeline.recordTick(1, Map.of("ANIMATION_CHANGED", 1), 1, 0);

		List<EventTimeline.TickEntry> recent = timeline.recent(10);
		assertEquals(1, recent.size());
		assertEquals(3, recent.get(0).eventCounts.get("ANIMATION_CHANGED"));
		assertEquals(4, recent.get(0).triggers);
		assertEquals(1, recent.get(0).callouts);

		List<String> lines = timeline.format(10);
		assertEquals(1, lines.size());
		assertTrue(lines.get(0).contains("t1: 4ev"));
		assertTrue(lines.get(0).contains("ANIMATION_CHANGED=3"));
		assertTrue(lines.get(0).contains("trig=4"));
		assertTrue(lines.get(0).contains("call=1"));

		timeline.recordTick(2, null, 0, 0);
		timeline.recordTick(3, Map.of(), 0, 0);
		timeline.recent(1);
		timeline.format(2);

		for (int i = 0; i < EventTimeline.MAX_TICKS + 5; i++)
		{
			timeline.recordTick(100 + i, Map.of("TICK", 1), 0, 0);
		}
		assertEquals(EventTimeline.MAX_TICKS, timeline.recent(EventTimeline.MAX_TICKS).size());
		timeline.clear();
		assertEquals(List.of("(timeline empty)"), timeline.format(5));
	}

	@Test
	void triggerHistoryRecordsFiltersFormatsAndEvicts()
	{
		TriggerHistory history = new TriggerHistory();
		assertEquals(0, history.size());
		assertTrue(history.format(null, 10).get(0).contains("(no trigger fires"));

		history.record(new TriggerFire(5, "nex", "smoke", "shout smoke"));
		history.record(new TriggerFire(6, "inferno", "blob", "animation blob"));
		assertEquals(2, history.size());

		List<TriggerHistory.Entry> all = history.filter(null, 10);
		assertEquals(2, all.size());
		assertEquals(6, all.get(0).tick);
		assertTrue(all.get(0).matches("BLOB"));
		assertFalse(all.get(0).matches("nomatch"));

		List<String> lines = history.format("nex", 10);
		assertEquals(1, lines.size());
		assertTrue(lines.get(0).contains("[nex/smoke]"));
		assertTrue(history.format("nomatch", 5).get(0).contains("matching 'nomatch'"));
		assertTrue(history.format(null, 0).get(0).contains("(no trigger fires"));

		for (int i = 0; i < TriggerHistory.MAX_ENTRIES + 3; i++)
		{
			history.record(new TriggerFire(i, "boss", "ctx", "desc"));
		}
		assertEquals(TriggerHistory.MAX_ENTRIES, history.size());
		history.clear();
		assertEquals(0, history.size());
	}

	@Test
	void stateInspectorFormatsEmptyAndSnapshot()
	{
		StateInspector inspector = new StateInspector();
		assertNull(inspector.getPlayer());
		assertEquals(List.of(), inspector.getSessions());
		assertTrue(inspector.format().get(0).contains("(no snapshot)"));
		assertTrue(inspector.format().get(1).contains("none active"));
		assertEquals(Map.of(), inspector.toExportData());

		ActiveEncounter encounter = mock(ActiveEncounter.class);
		BossDefinition boss = new BossDefinition();
		boss.bossId = "nex";
		when(encounter.getBoss()).thenReturn(boss);
		when(encounter.getCurrentPhaseId()).thenReturn("smoke");
		when(encounter.getPhaseTick()).thenReturn(4);

		PlayerState player = new PlayerState(50, 99, 10, 20, 0, 8960);
		inspector.update(player, List.of(encounter));
		assertEquals(player, inspector.getPlayer());
		assertEquals(1, inspector.getSessions().size());
		assertEquals("nex", inspector.getSessions().get(0).bossId);

		List<String> lines = inspector.format();
		assertTrue(lines.get(0).contains("hp 50/99"));
		assertTrue(lines.get(0).contains("anim=8960"));
		assertTrue(lines.get(1).contains("encounters: 1"));
		assertTrue(lines.get(2).contains("boss=nex"));
		assertTrue(lines.get(2).contains("phase=smoke"));

		Map<String, String> export = inspector.toExportData();
		assertEquals("50", export.get("playerHp"));
		assertEquals("99", export.get("playerMaxHp"));
		assertEquals("10", export.get("playerX"));
		assertEquals("20", export.get("playerY"));
		assertEquals("0", export.get("playerPlane"));
		assertEquals("8960", export.get("playerAnimation"));
		assertEquals("nex:smoke@tick4", export.get("session0"));
	}

	@Test
	void stateInspectorUpdatesToEmpty()
	{
		StateInspector inspector = new StateInspector();
		ActiveEncounter encounter = mock(ActiveEncounter.class);
		BossDefinition boss = new BossDefinition();
		boss.bossId = "zulrah";
		when(encounter.getBoss()).thenReturn(boss);
		when(encounter.getCurrentPhaseId()).thenReturn("phase1");
		when(encounter.getPhaseTick()).thenReturn(0);
		inspector.update(null, List.of(encounter));
		assertEquals(1, inspector.getSessions().size());
		inspector.update(null, List.of());
		assertTrue(inspector.getSessions().isEmpty());
	}

	@Test
	void logExporterWritesValidJsonBundle(@TempDir Path dir) throws Exception
	{
		StateInspector state = new StateInspector();
		state.update(new PlayerState(10, 99, 0, 0, 0, -1), List.of());
		TriggerHistory history = new TriggerHistory();
		history.record(new TriggerFire(1, "nex", "ctx", "fire"));
		EventTimeline timeline = new EventTimeline();
		timeline.recordTick(1, Map.of("TICK", 1), 1, 0);
		LogBuffer buffer = new LogBuffer();
		buffer.log("line");

		LogExporter exporter = new LogExporter(dir);
		Path written = exporter.export(state, history, timeline, buffer);
		assertNotNull(written);
		assertTrue(Files.exists(written));
		String json = Files.readString(written);
		assertTrue(json.contains("\"triggerHistory\""));
		assertTrue(json.contains("\"eventTimeline\""));
		assertTrue(json.contains("\"logLines\""));
		assertTrue(json.contains("nex"));

		exporter.export(state, history, timeline, buffer);
		assertTrue(Files.list(dir).count() >= 2);
	}

	@Test
	void logExporterReturnsNullOnFailure(@TempDir Path dir) throws Exception
	{
		Path blocker = dir.resolve("blocked");
		Files.writeString(blocker, "not a dir");
		LogExporter exporter = new LogExporter(blocker.resolve("sub"));
		Path result = exporter.export(new StateInspector(), new TriggerHistory(),
			new EventTimeline(), new LogBuffer());
		assertNull(result);
	}
}
