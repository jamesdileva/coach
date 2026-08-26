package com.coach.plugin.trigger;

import com.coach.plugin.events.EventType;
import com.coach.plugin.events.GameEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.HashSet;

/**
 * Combines child triggers with AND/OR logic evaluated against the same event.
 *
 * Note: children are expected to observe the same event type — cross-event
 * combinations ("NPC spawned AND hp < 50%") belong to mechanic conditions,
 * which arrive with the ConditionEvaluator in Sprint 7.
 */
public class CompositeTriggerEvaluator implements TriggerEvaluator
{
	private final boolean and;
	private final List<TriggerEvaluator> children;
	private final Set<EventType> interested = new HashSet<>();

	public CompositeTriggerEvaluator(boolean and, List<TriggerEvaluator> children)
	{
		this.and = and;
		this.children = List.copyOf(children);
		for (TriggerEvaluator child : this.children)
		{
			interested.addAll(child.interestedIn());
		}
	}

	@Override
	public Set<EventType> interestedIn()
	{
		return interested;
	}

	@Override
	public boolean matches(GameEvent event)
	{
		List<Boolean> results = new ArrayList<>(children.size());
		for (TriggerEvaluator child : children)
		{
			results.add(child.matches(event));
		}
		if (and)
		{
			for (boolean result : results)
			{
				if (!result)
				{
					return false;
				}
			}
			return true;
		}
		for (boolean result : results)
		{
			if (result)
			{
				return true;
			}
		}
		return false;
	}

	@Override
	public String describe()
	{
		StringBuilder sb = new StringBuilder(and ? "AND(" : "OR(");
		for (int i = 0; i < children.size(); i++)
		{
			if (i > 0)
			{
				sb.append(", ");
			}
			sb.append(children.get(i).describe());
		}
		return sb.append(')').toString();
	}

	static boolean parseLogic(String logic)
	{
		return "AND".equalsIgnoreCase(String.valueOf(logic).toUpperCase(Locale.ROOT));
	}
}
