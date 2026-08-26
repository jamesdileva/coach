package com.coach.plugin.logging;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogBufferTest
{
	@TempDir
	Path tempDir;

	@Test
	void ringBufferCapsAtMaxEntries()
	{
		LogBuffer buffer = new LogBuffer();
		for (int i = 0; i < LogBuffer.MAX_ENTRIES + 10; i++)
		{
			buffer.log("line-" + i);
		}

		assertEquals(LogBuffer.MAX_ENTRIES, buffer.size());
		List<String> entries = buffer.snapshot();
		assertEquals("line-10", entries.get(0));
		assertEquals("line-" + (LogBuffer.MAX_ENTRIES + 9), entries.get(entries.size() - 1));
	}

	@Test
	void writesToFileWhenWriterAttached() throws IOException
	{
		Path logFile = tempDir.resolve("logs").resolve("coach-debug.log");
		FileLogWriter writer = new FileLogWriter(logFile);

		LogBuffer buffer = new LogBuffer();
		buffer.setFileWriter(writer);
		buffer.log("hello tick");
		buffer.log("second line");

		List<String> lines = Files.readAllLines(logFile);
		assertEquals(2, lines.size());
		assertEquals("hello tick", lines.get(0));
		assertEquals("second line", lines.get(1));
	}

	@Test
	void stopsWritingAfterWriterDetached() throws IOException
	{
		Path logFile = tempDir.resolve("coach-debug.log");
		FileLogWriter writer = new FileLogWriter(logFile);

		LogBuffer buffer = new LogBuffer();
		buffer.setFileWriter(writer);
		buffer.log("kept");
		buffer.setFileWriter(null);
		buffer.log("dropped");

		List<String> lines = Files.readAllLines(logFile);
		assertEquals(1, lines.size());
		assertTrue(buffer.snapshot().contains("dropped"));
	}

	@Test
	void clearEmptiesBuffer()
	{
		LogBuffer buffer = new LogBuffer();
		buffer.log("x");
		buffer.clear();
		assertEquals(0, buffer.size());
	}
}
