package com.coach.plugin.trigger;

import com.coach.plugin.events.EventType;
import com.coach.plugin.events.GameEvent;
import java.util.Set;

/**
 * A stateless matcher for one trigger definition from encounter JSON.
 * Evaluators never track state — that belongs to the Encounter Engine (§8.4).
 */
public interface TriggerEvaluator
{
	/**
	 * Event types this evaluator cares about; the engine skips evaluation otherwise.
	 */
	Set<EventType> interestedIn();

	/**
	 * Does this event satisfy the trigger?
	 */
	boolean matches(GameEvent event);

	/**
	 * Human-readable description for debug logs.
	 */
	String describe();
}
