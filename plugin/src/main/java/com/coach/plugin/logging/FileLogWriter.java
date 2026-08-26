package com.coach.plugin.logging;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Append-only text file writer with a single line of responsibility:
 * persist formatted log lines to disk. Created lazily; never throws.
 */
public class FileLogWriter implements AutoCloseable
{
	private final Path file;
	private PrintWriter writer;

	public FileLogWriter(Path file)
	{
		this.file = file;
	}

	public synchronized void write(String line)
	{
		try
		{
			if (writer == null)
			{
				Files.createDirectories(file.getParent());
				writer = new PrintWriter(Files.newBufferedWriter(file, java.nio.charset.StandardCharsets.UTF_8));
			}
			writer.println(line);
			writer.flush();
		}
		catch (IOException e)
		{
			// Logging must never break the plugin; drop the line.
		}
	}

	@Override
	public synchronized void close()
	{
		if (writer != null)
		{
			writer.close();
			writer = null;
		}
	}
}
