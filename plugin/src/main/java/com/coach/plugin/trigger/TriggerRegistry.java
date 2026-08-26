package com.coach.plugin.trigger;

import com.coach.plugin.encounter.model.TriggerDefinition;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maps trigger type strings from encounter JSON to evaluator factories.
 * Unknown types produce a warning and no evaluator — never a crash (§8.4).
 */
public class TriggerRegistry
{
	private static final Logger log = LoggerFactory.getLogger(TriggerRegistry.class);

	private final Map<String, Function<TriggerDefinition, TriggerEvaluator>> builders = new HashMap<>();

	public TriggerRegistry()
	{
		builders.put("animation", def -> new AnimationTriggerEvaluator(def.npcId, requireInt(def.animationId, "animationId")));
		builders.put("projectile", def -> new ProjectileTriggerEvaluator(requireInt(def.projectId, "projectId")));
		builders.put("graphic", def -> new GraphicTriggerEvaluator(def.npcId, requireInt(def.graphicId, "graphicId")));
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
			log.warn("[coach] trigger type '{}' not supported yet by TriggerEngine", definition.type);
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

	private static int requireInt(Integer value, String field)
	{
		if (value == null)
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
