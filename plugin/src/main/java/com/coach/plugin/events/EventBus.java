package com.coach.plugin.events;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/**
 * Internal pub-sub event bus with tick batching.
 *
 * Events posted between two game ticks are buffered; when the TICK event
 * arrives, the entire buffer is flushed as one batch to all listeners.
 * This guarantees all events within the same 600ms tick are processed
 * together (master-architecture §8.4).
 */
public class EventBus
{
	/**
	 * Receives one batch per game tick, in tick order.
	 */
	public interface Listener
	{
		void onTickBatch(int tick, List<GameEvent> events);
	}

	private final Deque<GameEvent> buffer = new ArrayDeque<>();
	private final List<Listener> listeners = new ArrayList<>();

	public void subscribe(Listener listener)
	{
		listeners.add(listener);
	}

	public void unsubscribe(Listener listener)
	{
		listeners.remove(listener);
	}

	/**
	 * Buffer an event, or flush the current batch if this is the TICK event.
	 */
	public void post(GameEvent event)
	{
		if (event.getType() == EventType.TICK)
		{
			flush(event.getTick(), event);
			return;
		}
		buffer.add(event);
	}

	public int pendingCount()
	{
		return buffer.size();
	}

	private void flush(int tick, GameEvent tickEvent)
	{
		List<GameEvent> batch = new ArrayList<>(buffer.size() + 1);
		batch.addAll(buffer);
		buffer.clear();
		batch.add(tickEvent);

		List<GameEvent> immutableBatch = Collections.unmodifiableList(batch);
		for (Listener listener : listeners)
		{
			listener.onTickBatch(tick, immutableBatch);
		}
	}
}
