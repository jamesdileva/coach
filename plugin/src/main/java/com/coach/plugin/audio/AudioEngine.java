package com.coach.plugin.audio;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Plays pre-recorded callout audio from encounter packs (rule 11: never
 * generated at runtime).
 *
 * Audio bytes are pre-loaded into memory at pack load so playback latency is
 * near zero. Playback is asynchronous and never blocks the tick thread.
 *
 * Format note: Java supports WAV/PCM out of the box but has no Ogg/Vorbis
 * decoder. The TTS pipeline (Sprint 27) ships .ogg; a decoder integration is
 * added there where real files can be tested. Until then non-WAV entries are
 * cached but fail gracefully with a log line.
 */
public class AudioEngine
{
	private static final Logger log = LoggerFactory.getLogger(AudioEngine.class);

	private final Map<String, byte[]> cache = new ConcurrentHashMap<>();
	private final ExecutorService playbackPool = Executors.newSingleThreadExecutor(r -> {
		Thread thread = new Thread(r, "coach-audio");
		thread.setDaemon(true);
		return thread;
	});

	private volatile boolean muted;
	private volatile int masterVolume = 70;

	public void setMuted(boolean muted)
	{
		this.muted = muted;
	}

	public void setMasterVolume(int volume)
	{
		this.masterVolume = Math.max(0, Math.min(100, volume));
	}

	public boolean isMuted()
	{
		return muted;
	}

	/**
	 * Pre-load every audio/* entry of a pack zip, keyed "packId/file.ogg".
	 */
	public void loadFromZip(Path zipPath, String packId)
	{
		try (ZipFile zip = new ZipFile(zipPath.toFile()))
		{
			java.util.Enumeration<? extends ZipEntry> entries = zip.entries();
			while (entries.hasMoreElements())
			{
				ZipEntry entry = entries.nextElement();
				String name = entry.getName();
				if (entry.isDirectory() || !name.startsWith("audio/")
					|| !name.endsWith(".wav") && !name.endsWith(".ogg"))
				{
					continue;
				}
				try (InputStream in = zip.getInputStream(entry))
				{
					cache.put(packId + "/" + name.substring("audio/".length()), readAll(in));
				}
			}
		}
		catch (IOException e)
		{
			log.warn("[coach] could not load audio from {}: {}", zipPath, e.getMessage());
		}
	}

	public void clear()
	{
		cache.clear();
	}

	public int getLoadedCount()
	{
		return cache.size();
	}

	/**
	 * Attempt playback; returns false when muted or the file is unavailable.
	 * Never throws — audio failure must not break coaching.
	 */
	public boolean play(String packId, String audioFile)
	{
		if (muted || audioFile == null)
		{
			return false;
		}
		byte[] data = cache.get(packId + "/" + audioFile);
		if (data == null)
		{
			log.debug("[coach] audio file not in pack: {}/{}", packId, audioFile);
			return false;
		}
		int volume = masterVolume;
		playbackPool.execute(() -> playBytes(data, volume));
		return true;
	}

	private void playBytes(byte[] data, int volume)
	{
		try
		{
			try (Clip clip = AudioSystem.getClip())
			{
				clip.open(AudioSystem.getAudioInputStream(new ByteArrayInputStream(data)));
				applyVolume(clip, volume);
				clip.start();
				// Clip plays on its own line; keep reference until done.
				Thread.sleep(Math.max(1, clip.getMicrosecondLength() / 1000) + 50);
			}
		}
		catch (Exception e)
		{
			log.debug("[coach] audio playback failed (headless or unsupported format): {}", e.getMessage());
		}
	}

	private void applyVolume(Clip clip, int volumePercent)
	{
		try
		{
			javax.sound.sampled.FloatControl gain =
				(javax.sound.sampled.FloatControl) clip.getControl(javax.sound.sampled.FloatControl.Type.MASTER_GAIN);
			float min = gain.getMinimum();
			float max = gain.getMaximum(); // usually 6 dB
			float db = min + (max - min) * (Math.max(0, Math.min(100, volumePercent)) / 100f);
			gain.setValue(db);
		}
		catch (Exception ignored)
		{
			// volume control unsupported: play at default
		}
	}

	private static byte[] readAll(InputStream in) throws IOException
	{
		java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
		byte[] buf = new byte[4096];
		int n;
		while ((n = in.read(buf)) != -1)
		{
			out.write(buf, 0, n);
		}
		return out.toByteArray();
	}
}
