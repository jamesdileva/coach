package com.coach.plugin.debug;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.coach.plugin.logging.LogBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Exports a debug bundle (state, trigger history, timeline, log entries) as a
 * single valid JSON file under coach/debug_logs/.
 */
public class LogExporter
{
	private static final Logger log = LoggerFactory.getLogger(LogExporter.class);
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private final Path baseDirectory;

	public LogExporter(Path baseDirectory)
	{
		this.baseDirectory = baseDirectory;
	}

	/**
	 * Write the bundle. Never throws — export failure is logged, not fatal.
	 *
	 * @return the written path, or null on failure
	 */
	public Path export(StateInspector stateInspector, TriggerHistory triggerHistory,
		EventTimeline eventTimeline, LogBuffer logBuffer)
	{
		try
		{
			LinkedHashMap<String, Object> bundle = new LinkedHashMap<>();
			bundle.put("exportedAt", Instant.now().toString());

			LinkedHashMap<String, Object> state = new LinkedHashMap<>();
			state.putAll(stateInspector.toExportData());
			bundle.put("state", state);

			var triggers = new ArrayList<LinkedHashMap<String, Object>>();
			for (TriggerHistory.Entry entry : triggerHistory.filter(null, TriggerHistory.MAX_ENTRIES))
			{
				LinkedHashMap<String, Object> t = new LinkedHashMap<>();
				t.put("tick", entry.tick);
				t.put("bossId", entry.bossId);
				t.put("contextId", entry.contextId);
				t.put("description", entry.description);
				triggers.add(t);
			}
			bundle.put("triggerHistory", triggers);

			var timeline = new ArrayList<LinkedHashMap<String, Object>>();
			for (EventTimeline.TickEntry tickEntry : eventTimeline.recent(EventTimeline.MAX_TICKS))
			{
				LinkedHashMap<String, Object> t = new LinkedHashMap<>();
				t.put("tick", tickEntry.tick);
				t.put("events", tickEntry.eventCounts);
				t.put("triggers", tickEntry.triggers);
				t.put("callouts", tickEntry.callouts);
				timeline.add(t);
			}
			bundle.put("eventTimeline", timeline);

			bundle.put("logLines", logBuffer.snapshot());

			Path file = baseDirectory.resolve(
				"coach-debug-export-" + System.currentTimeMillis() + ".json");
			Files.createDirectories(file.getParent());
			String json = GSON.toJson(bundle);
			Files.writeString(file, json, StandardCharsets.UTF_8);

			log.info("[coach] debug bundle exported: {}", file);
			return file;
		}
		catch (Exception e)
		{
			log.warn("[coach] debug export failed: {}", e.getMessage());
			return null;
		}
	}
}
