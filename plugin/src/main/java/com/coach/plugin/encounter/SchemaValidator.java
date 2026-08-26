package com.coach.plugin.encounter;

import com.coach.plugin.encounter.model.BossDefinition;
import com.coach.plugin.encounter.model.CalloutDefinition;
import com.coach.plugin.encounter.model.EncounterPack;
import com.coach.plugin.encounter.model.MechanicDefinition;
import com.coach.plugin.encounter.model.PhaseDefinition;
import com.coach.plugin.encounter.model.TriggerDefinition;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Validates a parsed encounter pack against the schema v1 rules
 * (master-architecture §10). Returns all violations found — empty list = valid.
 *
 * Full JSON-Schema checking lives in the AI pipeline; this is the plugin's
 * load-time gate.
 */
public final class SchemaValidator
{
	private static final Set<String> TRIGGER_TYPES = Set.of(
		"animation", "projectile", "graphic", "npc_spawn", "npc_despawn",
		"hp", "tick_timer", "player_state", "location", "shout", "wave_cleared",
		"custom", "composite");

	private static final Set<String> CALLOUT_CATEGORIES = Set.of(
		"critical", "warning", "info", "transition");

	private static final Set<String> CONDITION_TYPES = Set.of(
		"npc_hp_below", "npc_hp_above", "player_hp_below", "player_hp_above",
		"tick_mod", "player_in_region", "prayer_active", "prayer_inactive",
		"inventory_contains", "custom");

	private static final int MIN_TICK_OFFSET = -5;
	private static final int MAX_TICK_OFFSET = 10;

	private List<String> errors = new ArrayList<>();

	/**
	 * Validate a pack. Stateless across calls: each invocation starts fresh.
	 */
	public List<String> validate(EncounterPack pack)
	{
		errors = new ArrayList<>();
		if (pack.schemaVersion == null)
		{
			errors.add("missing required field: schemaVersion");
			return errors;
		}
		if (!EncounterPack.SUPPORTED_SCHEMA_VERSION.equals(pack.schemaVersion))
		{
			errors.add("unsupported schemaVersion: '" + pack.schemaVersion
				+ "' (supported: " + EncounterPack.SUPPORTED_SCHEMA_VERSION + ")");
			return errors;
		}

		if (pack.metadata == null)
		{
			errors.add("missing required section: metadata");
		}
		else
		{
			requireText(pack.metadata.packId, "metadata.packId");
			requireText(pack.metadata.name, "metadata.name");
			requireText(pack.metadata.version, "metadata.version");
			requireText(pack.metadata.gameVersion, "metadata.gameVersion");
			if (pack.metadata.dependencies != null)
			{
				int i = 0;
				for (String dependency : pack.metadata.dependencies)
				{
					if (dependency == null || dependency.trim().isEmpty())
					{
						errors.add("metadata.dependencies[" + i + "] must be a non-empty packId");
					}
					i++;
				}
			}
		}

		if (pack.bosses == null || pack.bosses.isEmpty())
		{
			errors.add("pack must contain at least one boss");
			return errors;
		}

		Set<String> bossIds = new HashSet<>();
		for (BossDefinition boss : pack.bosses)
		{
			validateBoss(boss, bossIds);
		}
		return errors;
	}

	private void validateBoss(BossDefinition boss, Set<String> bossIds)
	{
		String label = bossLabel(boss);

		if (!requireText(boss.bossId, label + ".bossId")) return;
		if (!bossIds.add(boss.bossId))
		{
			errors.add("duplicate bossId: " + boss.bossId);
		}
		label = "boss[" + boss.bossId + "]";

		requireText(boss.name, label + ".name");
		requireNonNull(boss.npcId, label + ".npcId");

		if (boss.phases == null || boss.phases.isEmpty())
		{
			errors.add(label + ": at least one phase is required");
			return;
		}

		Set<String> phaseIds = new HashSet<>();
		for (PhaseDefinition phase : boss.phases)
		{
			validatePhase(boss.bossId, phase, phaseIds);
		}

		// shared boss-level mechanics have their own id scope
		validateMechanics(boss.bossId, boss.mechanics, label + ".mechanics");
	}

	private void validatePhase(String bossId, PhaseDefinition phase, Set<String> phaseIds)
	{
		String label = "boss[" + bossId + "] phase[" + (phase.phaseId != null ? phase.phaseId : "?") + "]";
		phase.bossId = bossId;

		if (!requireText(phase.phaseId, label + ".phaseId")) return;
		if (!phaseIds.add(phase.phaseId))
		{
			errors.add("duplicate phaseId '" + phase.phaseId + "' in boss " + bossId);
		}
		requireText(phase.name, label + ".name");

		if (phase.entryTrigger == null)
		{
			errors.add(label + ": entryTrigger is required");
		}
		else
		{
			validateTrigger(phase.entryTrigger, label + ".entryTrigger");
		}

		if (phase.exitTriggers != null)
		{
			int i = 0;
			for (TriggerDefinition trigger : phase.exitTriggers)
			{
				validateTrigger(trigger, label + ".exitTriggers[" + i++ + "]");
			}
		}

		validateMechanics(bossId, phase.mechanics, label + ".mechanics");
	}

	private void validateMechanics(String bossId, List<MechanicDefinition> mechanics, String label)
	{
		if (mechanics == null)
		{
			return;
		}
		// id uniqueness is scoped per mechanic list (one phase or the shared
		// boss-level list) — generated wave packs legitimately reuse attack
		// mechanics across phases, and runtime only evaluates the active phase.
		Set<String> scopeIds = new HashSet<>();
		for (MechanicDefinition mechanic : mechanics)
		{
			mechanic.bossId = bossId;
			String mLabel = label + "[" + (mechanic.mechanicId != null ? mechanic.mechanicId : "?") + "]";

			if (!requireText(mechanic.mechanicId, mLabel + ".mechanicId")) continue;
			if (!scopeIds.add(mechanic.mechanicId))
			{
				errors.add("duplicate mechanicId '" + mechanic.mechanicId + "' in boss " + bossId);
			}
			mLabel = label + "[" + mechanic.mechanicId + "]";
			requireText(mechanic.name, mLabel + ".name");

			if (mechanic.triggers == null || mechanic.triggers.isEmpty())
			{
				errors.add(mLabel + ": at least one trigger is required");
			}
			else
			{
				int i = 0;
				for (TriggerDefinition trigger : mechanic.triggers)
				{
					trigger.contextId = mechanic.mechanicId;
					validateTrigger(trigger, mLabel + ".triggers[" + i++ + "]");
				}
			}

			if (mechanic.callouts != null)
			{
				Set<String> calloutIds = new HashSet<>();
				int i = 0;
				for (CalloutDefinition callout : mechanic.callouts)
				{
					callout.bossId = bossId;
					validateCallout(callout, mLabel + ".callouts[" + i++ + "]", calloutIds);
				}
			}

			if (mechanic.cooldown != null && mechanic.cooldown < 0)
			{
				errors.add(mLabel + ".cooldown must be >= 0");
			}

			if (mechanic.conditions != null)
			{
				int i = 0;
				for (com.coach.plugin.encounter.model.ConditionDefinition condition : mechanic.conditions)
				{
					validateCondition(condition, mLabel + ".conditions[" + i++ + "]");
				}
			}
		}
	}

	private void validateCondition(com.coach.plugin.encounter.model.ConditionDefinition condition, String label)
	{
		if (!requireText(condition.type, label + ".type")) return;
		if (!CONDITION_TYPES.contains(condition.type))
		{
			errors.add(label + ": unknown condition type '" + condition.type + "'");
			return;
		}
		if ((condition.type.endsWith("_hp_below") || condition.type.endsWith("_hp_above"))
			&& condition.threshold == null)
		{
			errors.add(label + ": hp conditions require a threshold");
		}
		if ("tick_mod".equals(condition.type) && (condition.mod == null || condition.mod < 1))
		{
			errors.add(label + ": tick_mod requires mod >= 1");
		}
	}

	private void validateCallout(CalloutDefinition callout, String label, Set<String> calloutIds)
	{
		if (!requireText(callout.calloutId, label + ".calloutId")) return;
		if (!calloutIds.add(callout.calloutId))
		{
			errors.add("duplicate calloutId '" + callout.calloutId + "'");
			return;
		}
		label = label.replace("[?]", "[" + callout.calloutId + "]");
		requireText(callout.text, label + ".text");

		if (!requireText(callout.category, label + ".category")) return;
		if (!CALLOUT_CATEGORIES.contains(callout.category))
		{
			errors.add(label + ".category must be one of " + CALLOUT_CATEGORIES
				+ " but was '" + callout.category + "'");
		}

		if (callout.priority != null && (callout.priority < 1 || callout.priority > 100))
		{
			errors.add(label + ".priority must be 1-100");
		}

		checkTickOffset(callout.audioOffset, label + ".audioOffset");
		checkTickOffset(callout.visualOffset, label + ".visualOffset");

		if (callout.visual != null && callout.visual.type != null)
		{
			// visual type whitelist is enforced when rendering lands (Sprint 9+)
		}
	}

	private void validateTrigger(TriggerDefinition trigger, String label)
	{
		if (!requireText(trigger.type, label + ".type")) return;
		if (!TRIGGER_TYPES.contains(trigger.type))
		{
			errors.add(label + ": unknown trigger type '" + trigger.type + "'");
		}
		if ("composite".equals(trigger.type))
		{
			if (trigger.children == null || trigger.children.isEmpty())
			{
				errors.add(label + ": composite trigger requires children");
			}
			else if (!"AND".equalsIgnoreCase(trigger.logic) && !"OR".equalsIgnoreCase(trigger.logic))
			{
				errors.add(label + ": composite logic must be AND or OR");
			}
			else
			{
				int i = 0;
				for (TriggerDefinition child : trigger.children)
				{
					validateTrigger(child, label + ".children[" + i++ + "]");
				}
			}
		}
	}

	private void checkTickOffset(Integer offset, String label)
	{
		if (offset != null && (offset < MIN_TICK_OFFSET || offset > MAX_TICK_OFFSET))
		{
			errors.add(label + " must be between " + MIN_TICK_OFFSET + " and "
				+ MAX_TICK_OFFSET + " but was " + offset);
		}
	}

	private boolean requireText(String value, String field)
	{
		if (value == null || value.trim().isEmpty())
		{
			errors.add("missing required field: " + field);
			return false;
		}
		return true;
	}

	private boolean requireNonNull(Object value, String field)
	{
		if (value == null)
		{
			errors.add("missing required field: " + field);
			return false;
		}
		return true;
	}

	private static String bossLabel(BossDefinition boss)
	{
		return "boss[" + (boss.bossId != null ? boss.bossId : "?") + "]";
	}
}

