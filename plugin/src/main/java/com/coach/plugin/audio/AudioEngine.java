package com.coach.plugin.audio;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Plays pre-recorded callout audio from encounter packs (rule 11: never
 * generated at runtime).
 *
 * Audio bytes are pre-loaded into memory at pack load so playback latency is
 * near zero. Playback is asynchronous and never blocks the tick thread.
 *
 * Priority model (roadmap Sprint 19): higher-category requests interrupt the
 * playing clip; same/lower categories queue; the highest-priority queued
 * request starts when the current clip finishes. Volume is
 * master × per-category, both configurable live.
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

	private final AudioInterruptManager interruptManager = new AudioInterruptManager();
	private volatile Clip currentClip;

	private volatile boolean muted;
	private volatile int masterVolume = 70;
	private final Map<AudioCategory, Integer> categoryVolumes =
		new EnumMap<>(AudioCategory.class);

	public AudioEngine()
	{
		categoryVolumes.put(AudioCategory.CRITICAL, 100);
		categoryVolumes.put(AudioCategory.WARNING, 80);
		categoryVolumes.put(AudioCategory.INFO, 60);
		categoryVolumes.put(AudioCategory.TRANSITION, 50);
	}

	public void setMuted(boolean muted)
	{
		this.muted = muted;
	}

	public void setMasterVolume(int volume)
	{
		this.masterVolume = clamp(volume);
	}

	public void setCategoryVolume(String calloutCategory, int volume)
	{
		categoryVolumes.put(
			AudioCategory.fromCalloutCategory(calloutCategory), clamp(volume));
	}

	public void resetQueue()
	{
		interruptManager.reset();
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
		resetQueue();
	}

	public int getLoadedCount()
	{
		return cache.size();
	}

	/**
	 * Submit an audio request for playback; returns false when muted or the
	 * file is unavailable (it may also return false when queued — that is not
	 * an error). Never throws — audio failure must not break coaching.
	 */
	public boolean play(String packId, String audioFile, String calloutCategory)
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

		AudioCategory category = new AudioPriorityResolver().resolve(calloutCategory);
		float gainDb = effectiveGain(category);
		byte[] clipData = data.clone();

		boolean started = interruptManager.submit(category, () ->
			playbackPool.execute(() -> startClip(clipData, gainDb)));
		log.debug("[coach] audio {}: {} ({})", audioFile,
			started ? "playing" : "queued", category);
		return true;
	}

	private void startClip(byte[] data, float gainDb)
	{
		stopCurrentClip();
		try
		{
			Clip clip = AudioSystem.getClip();
			clip.open(AudioSystem.getAudioInputStream(new ByteArrayInputStream(data)));
			applyVolume(clip, gainDb);
			clip.addLineListener(event -> {
				if (event.getType() == LineEvent.Type.STOP && event.getLine() == clip)
				{
					if (currentClip == clip)
					{
						currentClip = null;
						interruptManager.onPlaybackFinished();
					}
				}
			});
			currentClip = clip;
			clip.start();
			// keep the line alive until it finishes; STOP listener handles cleanup
			playbackPool.execute(() -> {
				try
				{
					Thread.sleep(Math.max(1, clip.getMicrosecondLength() / 1000) + 50);
				}
				catch (InterruptedException e)
				{
					Thread.currentThread().interrupt();
				}
			});
		}
		catch (Exception e)
		{
			log.debug("[coach] audio playback failed (headless or unsupported format): {}", e.getMessage());
			currentClip = null;
			interruptManager.onPlaybackFinished();
		}
	}

	private synchronized void stopCurrentClip()
	{
		if (currentClip != null)
		{
			currentClip.stop(); // stale STOP event ignored via identity check
		}
	}

	private float effectiveGain(AudioCategory category)
	{
		int master = masterVolume;
		int categoryVolume = categoryVolumes.getOrDefault(category, 70);
		return (master / 100f) * (categoryVolume / 100f);
	}

	private void applyVolume(Clip clip, float linearPercent)
	{
		try
		{
			javax.sound.sampled.FloatControl gain =
				(javax.sound.sampled.FloatControl) clip.getControl(javax.sound.sampled.FloatControl.Type.MASTER_GAIN);
			float min = gain.getMinimum();
			float max = gain.getMaximum();
			gain.setValue(min + (max - min) * Math.max(0f, Math.min(1f, linearPercent)));
		}
		catch (Exception ignored)
		{
			// volume control unsupported: play at default
		}
	}

	private static int clamp(int value)
	{
		return Math.max(0, Math.min(100, value));
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
