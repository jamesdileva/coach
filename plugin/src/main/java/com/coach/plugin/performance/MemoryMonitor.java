package com.coach.plugin.performance;

/**
 * Samples JVM heap usage to track plugin memory growth across a fight.
 * Best-effort: the JVM may GC at any time, so growth is only meaningful
 * over long runs (master-architecture §12 target: &lt; 1 MB / 10 min).
 */
public class MemoryMonitor
{
	private long baselineBytes;
	private long lastUsedBytes;
	private long growthBytes;
	private int samples;

	public MemoryMonitor()
	{
		reset();
	}

	/** Establish the baseline used-memory reading. */
	public final void reset()
	{
		lastUsedBytes = usedBytes();
		baselineBytes = lastUsedBytes;
		growthBytes = 0L;
		samples = 0;
	}

	/** Current used heap in bytes. */
	public long usedBytes()
	{
		Runtime runtime = Runtime.getRuntime();
		return runtime.totalMemory() - runtime.freeMemory();
	}

	/** Take a sample and update growth relative to the baseline. */
	public void sample()
	{
		long used = usedBytes();
		growthBytes = used - baselineBytes;
		lastUsedBytes = used;
		samples++;
	}

	public long getGrowthBytes()
	{
		return growthBytes;
	}

	public long getBaselineBytes()
	{
		return baselineBytes;
	}

	public long getLastUsedBytes()
	{
		return lastUsedBytes;
	}

	public int getSamples()
	{
		return samples;
	}

	public String formatLines()
	{
		return "mem " + formatMb(lastUsedBytes)
			+ " used, " + formatSignedMb(growthBytes)
			+ " growth, " + samples + " sample(s)";
	}

	public static String formatMb(long bytes)
	{
		return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
	}

	public static String formatSignedMb(long bytes)
	{
		return String.format("%+.1f MB", bytes / (1024.0 * 1024.0));
	}
}
