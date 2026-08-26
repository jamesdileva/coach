package com.coach.plugin.trigger;

import com.coach.plugin.encounter.model.TriggerDefinition;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import net.runelite.api.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maps trigger type strings from encounter JSON to evaluator factories.
 * Unknown or not-yet-supported types produce a warning and no evaluator —
 * never a crash (§8.4).
 */
public class TriggerRegistry
{
	private static final Logger log = LoggerFactory.getLogger(TriggerRegistry.class);

	private final Client client; // nullable in tests; needed by hp/location evaluators
	private final Map<String, Function<TriggerDefinition, TriggerEvaluator>> builders = new HashMap<>();

	public TriggerRegistry(Client client)
	{
		this.client = client;

		builders.put("animation", def -> new AnimationTriggerEvaluator(def.npcId, requireInt(def.animationId, "animationId")));
		builders.put("projectile", def -> new ProjectileTriggerEvaluator(requireInt(def.projectId, "projectId"), def.srcNpcId));
		builders.put("graphic", def -> new GraphicTriggerEvaluator(def.npcId, requireInt(def.graphicId, "graphicId")));
		builders.put("npc_spawn", def -> new NpcSpawnTriggerEvaluator(def.npcId, def.npcIds, true));
		builders.put("npc_despawn", def -> new NpcSpawnTriggerEvaluator(def.npcId, def.npcIds, false));
		builders.put("shout", this::buildShout);
		builders.put("wave_cleared", this::buildWaveCleared);
		builders.put("hp", this::buildHp);
		builders.put("tick_timer", this::buildTickTimer);
		builders.put("player_state", this::buildPlayerState);
		builders.put("location", this::buildLocation);
		builders.put("composite", this::buildComposite);
		// 'custom' deliberately unregistered until ConditionEvaluator lands (Sprint 7)
	}

	/**
	 * Build an evaluator for the definition; empty for unknown/unbuildable types.
	 */
	public Optional<TriggerEvaluator> create(TriggerDefinition definition)
	{
		if (definition == null || definition.type == null)
		{
			return Optional.empty();
		}
		Function<TriggerDefinition, TriggerEvaluator> builder = builders.get(definition.type);
		if (builder == null)
		{
			log.warn("[coach] trigger type '{}' not supported yet by TriggerEngine"
				+ ("custom".equals(definition.type) ? " (arrives with ConditionEvaluator)" : ""),
				definition.type);
			return Optional.empty();
		}
		try
		{
			return Optional.of(builder.apply(definition));
		}
		catch (MissingFieldException e)
		{
			log.warn("[coach] trigger type '{}' missing field {}: {}", definition.type, e.field, e.getMessage());
			return Optional.empty();
		}
	}

	private TriggerEvaluator buildWaveCleared(TriggerDefinition def)
	{
		if (def.npcIds == null || def.npcIds.isEmpty())
		{
			throw new MissingFieldException("npcIds");
		}
		return new WaveClearedEvaluator(def.npcIds);
	}

	private TriggerEvaluator buildShout(TriggerDefinition def)
	{
		return new ShoutTriggerEvaluator(requireText(def.containsText, "containsText"), def.senderName);
	}

	private TriggerEvaluator buildHp(TriggerDefinition def)
	{
		int threshold = requireInt(def.hpThreshold, "hpThreshold");
		boolean below = !"above".equalsIgnoreCase(def.hpDirection);
		if (def.npcIds != null && !def.npcIds.isEmpty())
		{
			return new HpTriggerEvaluator(client, new java.util.HashSet<>(def.npcIds), below, threshold);
		}
		return new HpTriggerEvaluator(client, requireInt(def.npcId, "npcId"), below, threshold);
	}

	private TriggerEvaluator buildTickTimer(TriggerDefinition def)
	{
		return new TickTimerTriggerEvaluator(
			requireInt(def.tickMod, "tickMod"),
			def.tickOffset != null ? def.tickOffset : 0);
	}

	private TriggerEvaluator buildPlayerState(TriggerDefinition def)
	{
		if (def.animationId != null)
		{
			return new PlayerStateTriggerEvaluator(def.animationId, null, true);
		}
		if (def.hpThreshold != null)
		{
			boolean below = !"above".equalsIgnoreCase(def.hpDirection);
			return new PlayerStateTriggerEvaluator(null, def.hpThreshold, below);
		}
		throw new MissingFieldException("animationId or hpThreshold");
	}

	private TriggerEvaluator buildLocation(TriggerDefinition def)
	{
		if (client == null)
		{
			log.warn("[coach] location trigger requires client access — skipping");
			return null;
		}
		return new LocationTriggerEvaluator(client,
			requireInt(def.minX, "minX"), requireInt(def.maxX, "maxX"),
			requireInt(def.minY, "minY"), requireInt(def.maxY, "maxY"));
	}

	private TriggerEvaluator buildComposite(TriggerDefinition def)
	{
		if (def.children == null || def.children.isEmpty())
		{
			throw new MissingFieldException("children");
		}
		List<TriggerEvaluator> children = new ArrayList<>(def.children.size());
		for (TriggerDefinition child : def.children)
		{
			children.add(create(child)
				.orElseThrow(() -> new MissingFieldException("valid child trigger")));
		}
		return new CompositeTriggerEvaluator(CompositeTriggerEvaluator.parseLogic(def.logic), children);
	}

	private static int requireInt(Integer value, String field)
	{
		if (value == null)
		{
			throw new MissingFieldException(field);
		}
		return value;
	}

	private static String requireText(String value, String field)
	{
		if (value == null || value.trim().isEmpty())
		{
			throw new MissingFieldException(field);
		}
		return value;
	}

	private static class MissingFieldException extends RuntimeException
	{
		final String field;

		MissingFieldException(String field)
		{
			super("missing required numeric field: " + field);
			this.field = field;
		}
	}
}
