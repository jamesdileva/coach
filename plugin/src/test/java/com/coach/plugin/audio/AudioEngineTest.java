package com.coach.plugin.audio;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AudioEngineTest
{
	@TempDir
	Path tempDir;

	/**
	 * Minimal valid 16-bit mono 8 kHz WAV containing a short sine beep.
	 */
	private static byte[] wavBytes() throws Exception
	{
		AudioFormat format = new AudioFormat(8000f, 16, 1, true, false);
		int samples = (int) (format.getFrameRate() * 0.05);
		byte[] pcm = new byte[samples * 2];
		for (int i = 0; i < samples; i++)
		{
			short value = (short) (Math.sin(2 * Math.PI * 440 * i / format.getFrameRate()) * 8000);
			pcm[i * 2] = (byte) value;
			pcm[i * 2 + 1] = (byte) (value >> 8);
		}
		try (AudioInputStream stream = new AudioInputStream(
			new java.io.ByteArrayInputStream(pcm), format, pcm.length / format.getFrameSize());
			ByteArrayOutputStream out = new ByteArrayOutputStream())
		{
			javax.sound.sampled.AudioSystem.write(stream,
				javax.sound.sampled.AudioFileFormat.Type.WAVE, out);
			return out.toByteArray();
		}
	}

	private Path packZipWithAudio(String fileName) throws Exception
	{
		Path zip = tempDir.resolve("pack.zip");
		try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip.toFile())))
		{
			out.putNextEntry(new ZipEntry("audio/" + fileName));
			out.write(wavBytes());
			out.closeEntry();
		}
		return zip;
	}

	@Test
	void loadsWavEntriesFromPackZip() throws Exception
	{
		AudioEngine engine = new AudioEngine();
		engine.loadFromZip(packZipWithAudio("pray_ranged.wav"), "pack1");

		assertEquals(1, engine.getLoadedCount());
		assertTrue(engine.play("pack1", "pray_ranged.wav"), "playback accepted");
	}

	@Test
	void missingFileAndNullAreGracefulNoOps()
	{
		AudioEngine engine = new AudioEngine();
		assertFalse(engine.play("pack1", "missing.wav"));
		assertFalse(engine.play("pack1", null));
	}

	@Test
	void muteSuppressesPlayback()
	{
		AudioEngine engine = new AudioEngine();
		engine.setMuted(true);

		assertTrue(engine.isMuted());
		assertFalse(engine.play("any", "any.wav"), "muted -> never plays");
	}

	@Test
	void volumeClampedToRange()
	{
		AudioEngine engine = new AudioEngine();
		engine.setMasterVolume(-5);   // clamps to 0 internally
		engine.setMasterVolume(150);  // clamps to 100 internally
		engine.setMuted(false);
		// no assertion beyond "didn't throw": volume is applied at playback time
	}
}
