package com.coach.plugin.logging;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Central debug log sink: bounded in-memory ring buffer (for the in-game
 * debug overlay) with optional file persistence.
 */
public class LogBuffer
{
	public static final int MAX_ENTRIES = 100;

	private final Deque<String> entries = new ArrayDeque<>();
	private FileLogWriter fileWriter;

	/**
	 * Record one formatted line. Never throws — logging must not break the plugin.
	 */
	public synchronized void log(String line)
	{
		if (entries.size() >= MAX_ENTRIES)
		{
			entries.pollFirst();
		}
		entries.addLast(line);

		if (fileWriter != null)
		{
			fileWriter.write(line);
		}
	}

	public synchronized List<String> snapshot()
	{
		return new ArrayList<>(entries);
	}

	public synchronized int size()
	{
		return entries.size();
	}

	public synchronized void clear()
	{
		entries.clear();
	}

	public synchronized void setFileWriter(FileLogWriter writer)
	{
		if (fileWriter != null && fileWriter != writer)
		{
			fileWriter.close();
		}
		fileWriter = writer;
	}
}
