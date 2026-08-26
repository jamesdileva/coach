package com.coach.plugin.trigger;

import com.coach.plugin.events.EventType;
import com.coach.plugin.events.GameEvent;
import com.coach.plugin.events.GameStateBridge;
import java.util.Set;
import net.runelite.api.Client;
import net.runelite.api.NPC;

/**
 * Fires when an NPC's health percentage crosses the configured threshold.
 *
 * Evaluated once per tick against live NPC state (RuneLite has no NPC HP
 * event); uses edge detection so it fires once at the crossing, not every
 * tick beyond it. Requires a client reference for the NPC lookup.
 */
public class HpTriggerEvaluator implements TriggerEvaluator
{
	private final Client client;
	private final java.util.Set<Integer> npcIds;
	private final boolean below;      // true = fire when hp drops to/below threshold
	private final int thresholdPercent;
	private final EdgeDetector edge = new EdgeDetector();

	public HpTriggerEvaluator(Client client, int npcId, boolean below, int thresholdPercent)
	{
		this(client, java.util.Set.of(npcId), below, thresholdPercent);
	}

	public HpTriggerEvaluator(Client client, java.util.Set<Integer> npcIds, boolean below,
		int thresholdPercent)
	{
		this.client = client;
		this.npcIds = npcIds;
		this.below = below;
		this.thresholdPercent = thresholdPercent;
	}

	@Override
	public Set<EventType> interestedIn()
	{
		return Set.of(EventType.TICK);
	}

	@Override
	public boolean matches(GameEvent event)
	{
		NPC npc = findNpc();
		if (npc == null)
		{
			edge.reset(); // boss gone: re-arm so a respawn re-triggers cleanly
			return false;
		}
		boolean satisfied = below
			? healthPercent(npc) <= thresholdPercent
			: healthPercent(npc) >= thresholdPercent;
		return edge.onNext(satisfied);
	}

	static int healthPercent(NPC npc)
	{
		int scale = npc.getHealthScale();
		int ratio = npc.getHealthRatio();
		if (scale <= 0)
		{
			return ratio;
		}
		return (int) Math.round(ratio * 100.0 / scale);
	}

	private NPC findNpc()
	{
		if (client == null || npcIds.isEmpty())
		{
			return null;
		}
		for (NPC candidate : client.getTopLevelWorldView().npcs())
		{
			if (npcIds.contains(candidate.getId()))
			{
				return candidate;
			}
		}
		return null;
	}

	@Override
	public String describe()
	{
		return "npc " + npcIds + " hp " + (below ? "<=" : ">=") + thresholdPercent + "%";
	}
}
