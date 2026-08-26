package com.coach.plugin.audio;

import java.util.ArrayList;
import java.util.List;

/**
 * Decides what happens when an audio request arrives while something is
 * playing (roadmap Sprint 19):
 * - higher priority interrupts the playing clip
 * - same or lower priority queues
 * - when playback finishes, the highest-priority queued request starts
 *   (FIFO within a category)
 */
public class AudioInterruptManager
{
	/**
	 * Starts actual playback of a queued/submitted request. Supplied by the
	 * AudioEngine; implementations must stop any current clip first.
	 */
	public interface Starter
	{
		void start();
	}

	private static final class Queued
	{
		final AudioCategory category;
		final int order;
		final Starter starter;

		Queued(AudioCategory category, int order, Starter starter)
		{
			this.category = category;
			this.order = order;
			this.starter = starter;
		}
	}

	private AudioCategory playing;
	private final List<Queued> queue = new ArrayList<>();
	private int nextOrder;

	/**
	 * Submit a request.
	 *
	 * @return true if it started immediately (idle or interrupting);
	 *         false if it was queued behind current playback
	 */
	public synchronized boolean submit(AudioCategory incoming, Starter starter)
	{
		if (playing == null || incoming.rank > playing.rank)
		{
			start(incoming, starter);
			return true;
		}
		queue.add(new Queued(incoming, nextOrder++, starter));
		return false;
	}

	/**
	 * Called by the engine when the active clip finishes (or fails to start).
	 */
	public synchronized void onPlaybackFinished()
	{
		playing = null;
		if (queue.isEmpty())
		{
			return;
		}
		Queued best = queue.get(0);
		for (Queued candidate : queue)
		{
			if (candidate.category.rank > best.category.rank
				|| candidate.category.rank == best.category.rank
					&& candidate.order < best.order)
			{
				best = candidate;
			}
		}
		queue.remove(best);
		start(best.category, best.starter);
	}

	public synchronized int queuedCount()
	{
		return queue.size();
	}

	public synchronized void reset()
	{
		queue.clear();
		playing = null;
	}

	private void start(AudioCategory category, Starter starter)
	{
		playing = category;
		starter.start();
	}
}
