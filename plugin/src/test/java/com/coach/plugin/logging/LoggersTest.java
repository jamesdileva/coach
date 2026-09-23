package com.coach.plugin.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LoggersTest
{
	@Test
	void logBufferRingEvictsOldest()
	{
		LogBuffer buffer = new LogBuffer();
		for (int i = 0; i < LogBuffer.MAX_ENTRIES + 10; i++)
		{
			buffer.log("line-" + i);
		}
		assertEquals(LogBuffer.MAX_ENTRIES, buffer.size());
		List<String> snapshot = buffer.snapshot();
		assertEquals("line-10", snapshot.get(0));
		assertEquals("line-" + (LogBuffer.MAX_ENTRIES + 9), snapshot.get(snapshot.size() - 1));
		buffer.clear();
		assertEquals(0, buffer.size());
	}

	@Test
	void logBufferWritesThroughToFileWriter(@TempDir Path dir)
	{
		Path file = dir.resolve("logs").resolve("coach.log");
		LogBuffer buffer = new LogBuffer();
		buffer.setFileWriter(new FileLogWriter(file));
		buffer.log("alpha");
		buffer.log("beta");
		buffer.clear();
		assertEquals(0, buffer.size());

		FileLogWriter replacement = new FileLogWriter(dir.resolve("other.log"));
		buffer.setFileWriter(replacement);
		replacement.close();
	}

	@Test
	void triggerLoggerWritesMatchOnly(@TempDir Path dir) throws Exception
	{
		LogBuffer buffer = new LogBuffer();
		TriggerLogger logger = new TriggerLogger(buffer);
		logger.triggerFired(5, "ctx-1", "detail-x");
		logger.triggerEvaluated(6, "trig-1", false);
		logger.triggerEvaluated(7, "trig-2", true);

		List<String> lines = buffer.snapshot();
		assertEquals(2, lines.size());
		assertTrue(lines.get(0).contains("TRIGGER_FIRED ctx=ctx-1"));
		assertTrue(lines.get(1).contains("TRIGGER_MATCH trigger=trig-2"));
	}

	@Test
	void calloutLoggerFormatsScheduledAndDelivered()
	{
		LogBuffer buffer = new LogBuffer();
		CalloutLogger logger = new CalloutLogger(buffer);
		logger.calloutScheduled(3, "cal-1", 4, 5);
		logger.calloutDelivered(4, "cal-1", "visual");

		List<String> lines = buffer.snapshot();
		assertEquals("t3 CALLOUT_SCHEDULED id=cal-1 visual=t4 audio=t5", lines.get(0));
		assertEquals("t4 CALLOUT_DELIVERED id=cal-1 visual", lines.get(1));
	}

	@Test
	void fileLogWriterAppendsAndCloses(@TempDir Path dir) throws Exception
	{
		Path file = dir.resolve("nested").resolve("coach.log");
		try (FileLogWriter writer = new FileLogWriter(file))
		{
			writer.write("one");
			writer.write("two");
		}
		List<String> lines = Files.readAllLines(file);
		assertEquals(List.of("one", "two"), lines);

		FileLogWriter closed = new FileLogWriter(file);
		closed.write("after-close-attempt");
		closed.close();
		closed.close();
	}

	@Test
	void fileLogWriterDropsWhenPathUnwritable(@TempDir Path dir)
	{
		Path notADir = dir.resolve("file-not-dir");
		try
		{
			Files.writeString(notADir, "x");
		}
		catch (Exception e)
		{
			throw new AssertionError(e);
		}
		FileLogWriter writer = new FileLogWriter(notADir.resolve("child.log"));
		writer.write("will-not-throw");
		writer.close();
		assertFalse(Files.exists(notADir.resolve("child.log")));
	}
}
