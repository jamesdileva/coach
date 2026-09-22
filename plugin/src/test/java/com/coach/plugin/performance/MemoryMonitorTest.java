package com.coach.plugin.performance;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemoryMonitorTest
{
	@Test
	void resetZeroesGrowthAndSamples()
	{
		MemoryMonitor monitor = new MemoryMonitor();
		monitor.sample();
		monitor.sample();
		assertTrue(monitor.getSamples() > 0);

		monitor.reset();
		assertEquals(0, monitor.getSamples());
		assertEquals(0L, monitor.getGrowthBytes());
		assertTrue(monitor.getBaselineBytes() > 0L);
	}

	@Test
	void sampleUpdatesLastUsedAndCount()
	{
		MemoryMonitor monitor = new MemoryMonitor();
		long before = monitor.getSamples();
		monitor.sample();
		assertEquals(before + 1, monitor.getSamples());
		assertTrue(monitor.getLastUsedBytes() > 0L);
	}

	@Test
	void formatLinesIncludesMemAndSampleCount()
	{
		MemoryMonitor monitor = new MemoryMonitor();
		monitor.sample();
		String line = monitor.formatLines();
		assertTrue(line.contains("mem "));
		assertTrue(line.contains("MB"));
		assertTrue(line.contains("sample"));
	}

	@Test
	void formatHelpers()
	{
		assertTrue(MemoryMonitor.formatMb(1024L * 1024L).contains("1.0 MB"));
		assertTrue(MemoryMonitor.formatSignedMb(-1024L * 1024L).startsWith("-"));
		assertTrue(MemoryMonitor.formatSignedMb(1024L * 1024L).startsWith("+"));
	}
}
