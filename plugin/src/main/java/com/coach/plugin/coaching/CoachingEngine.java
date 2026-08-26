package com.coach.plugin.coaching;

import com.coach.plugin.encounter.MechanicActivation;
import com.coach.plugin.encounter.model.CalloutDefinition;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The decision-making core: consumes mechanic activations, schedules callouts
 * at tick-precise offsets, suppresses duplicates via cooldowns, and delivers
 * due callouts to listeners (visual overlay + audio arrive in Sprint 9).
 */
public class CoachingEngine
{
	private static final Logger log = LoggerFactory.getLogger(CoachingEngine.class);

	/**
	 * Receives delivered (due) callouts.
	 */
	public interface Listener
	{
		void onCalloutDelivered(DeliveredCallout delivery);
	}

	/**
	 * One delivered callout, with the context it came from.
	 */
	public static final class DeliveredCallout
	{
		private final int tick;
		private final String bossId;
		private final String mechanicId;
		private final CalloutDefinition callout;

		DeliveredCallout(int tick, String bossId, String mechanicId, CalloutDefinition callout)
		{
			this.tick = tick;
			this.bossId = bossId;
			this.mechanicId = mechanicId;
			this.callout = callout;
		}

		public int getTick()
		{
			return tick;
		}

		public String getBossId()
		{
			return bossId;
		}

		public String getMechanicId()
		{
			return mechanicId;
		}

		public CalloutDefinition getCallout()
		{
			return callout;
		}

		@Override
		public String toString()
		{
			return "t" + tick + " [" + bossId + "/" + mechanicId + "] "
				+ callout.calloutId + ": \"" + callout.text + "\"";
		}
	}

	private final CalloutQueue queue = new CalloutQueue();
	private final PriorityResolver priorityResolver = new PriorityResolver();
	private final CooldownManager cooldownManager = new CooldownManager();
	private final CalloutScheduler scheduler = new CalloutScheduler(queue, priorityResolver);
	private final List<Listener> listeners = new ArrayList<>();
	private volatile Predicate<CalloutDefinition> enabledFilter = c -> true;

	public void addListener(Listener listener)
	{
		listeners.add(listener);
	}

	public void removeListener(Listener listener)
	{
		listeners.remove(listener);
	}

	/**
	 * Gate for per-callout enable/disable (full config toggles land in Sprint 17).
	 */
	public void setEnabledFilter(Predicate<CalloutDefinition> filter)
	{
		this.enabledFilter = filter != null ? filter : c -> true;
	}

	/**
	 * Called by the EncounterEngine when a mechanic activates.
	 */
	public void onActivation(MechanicActivation activation)
	{
		for (CalloutDefinition callout : activation.getCallouts())
		{
			if (!enabledFilter.test(callout))
			{
				continue;
			}
			if (cooldownManager.isOnCooldown(callout.calloutId, activation.getTick()))
			{
				log.debug("[coach] callout {} on cooldown — suppressed", callout.calloutId);
				continue;
			}
			cooldownManager.apply(callout.calloutId, 0, activation.getTick());
			scheduler.schedule(activation.getBossId(), activation.getMechanic().mechanicId,
				callout, activation.getTick());
		}
	}

	/**
	 * Deliver every queued callout that is due at this tick.
	 * Called once per game tick, after trigger/encounter processing.
	 *
	 * @return the number of callouts delivered
	 */
	public int onTick(int tick)
	{
		List<CalloutRequest> due = queue.drainDue(tick);
		for (CalloutRequest request : due)
		{
			DeliveredCallout delivery = new DeliveredCallout(tick,
				request.getBossId(), request.getMechanicId(), request.getCallout());
			log.info("[coach] CALLOUT: {}", delivery);
			for (Listener listener : listeners.toArray(new Listener[0]))
			{
				listener.onCalloutDelivered(delivery);
			}
		}
		return due.size();
	}

	// exposed for tests/debug
	CalloutQueue getQueue()
	{
		return queue;
	}
}
