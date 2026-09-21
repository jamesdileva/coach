"""Schema v1.0 structural validation — Python mirror of the plugin's
SchemaValidator rules (master-architecture §10), so drafts reach human review
already loadable by the plugin.

Every violation becomes a critical Issue with a stable code the AI validator
agent can auto-fix where safe.
"""
from __future__ import annotations

from typing import Any, Dict, List, Set

from validation_report import Issue, Report, SEV_CRITICAL

SCHEMA_VERSION = "1.0"
KNOWN_TRIGGER_TYPES = {
    "animation", "projectile", "graphic", "npc_spawn", "npc_despawn",
    "hp", "tick_timer", "player_state", "location", "shout", "wave_cleared",
    "custom", "composite",
}
CATEGORIES = {"critical", "warning", "info", "transition"}
MIN_OFFSET, MAX_OFFSET = -5, 10


def validate(draft: Dict[str, Any]) -> Report:
    report = Report()
    if draft.get("schemaVersion") != SCHEMA_VERSION:
        report.add(Issue(SEV_CRITICAL, "SCHEMA_VERSION",
                         f"schemaVersion must be '{SCHEMA_VERSION}'"))
        return report

    metadata = draft.get("metadata") or {}
    for field in ("packId", "name", "version", "gameVersion"):
        if not metadata.get(field):
            report.add(Issue(SEV_CRITICAL, "MISSING_FIELD",
                             f"metadata.{field} is required", path=f"metadata.{field}"))

    bosses = draft.get("bosses") or []
    if not bosses:
        report.add(Issue(SEV_CRITICAL, "NO_BOSSES", "at least one boss required"))
        return report

    seen_pack_bosses: Set[str] = set()
    for boss in bosses:
        _validate_boss(boss, seen_pack_bosses, report)
    return report


def _validate_boss(boss: Dict[str, Any], seen_pack_bosses: Set[str], report: Report) -> None:
    bid = boss.get("bossId")
    base = f"bosses[{bid if bid else '?'}]"
    if not bid:
        report.add(Issue(SEV_CRITICAL, "MISSING_FIELD", "bossId required", base))
        return
    if bid in seen_pack_bosses:
        report.add(Issue(SEV_CRITICAL, "DUPLICATE_ID",
                         f"duplicate bossId '{bid}'", base))
    seen_pack_bosses.add(bid)
    if not boss.get("name"):
        report.add(Issue(SEV_CRITICAL, "MISSING_FIELD", "boss name required", base))

    phases = boss.get("phases") or []
    if not phases:
        report.add(Issue(SEV_CRITICAL, "NO_PHASES", "at least one phase required",
                         base))
        return

    seen_phase_ids: Set[str] = set()
    for index, phase in enumerate(phases):
        path = f"{base}.phases[{index}]"
        pid = phase.get("phaseId")
        if not pid:
            report.add(Issue(SEV_CRITICAL, "MISSING_FIELD", "phaseId required", path))
            continue
        if pid in seen_phase_ids:
            report.add(Issue(SEV_CRITICAL, "DUPLICATE_ID",
                             f"duplicate phaseId '{pid}'", path))
        seen_phase_ids.add(pid)
        if not phase.get("entryTrigger"):
            report.add(Issue(SEV_CRITICAL, "MISSING_ENTRY_TRIGGER",
                             "phase entryTrigger required", path))

        seen_mechanics: Set[str] = set()
        seen_callout_ids_in_phase: Set[str] = set()
        for m_index, mechanic in enumerate(phase.get("mechanics") or []):
            _validate_mechanic(mechanic, f"{path}.mechanics[{m_index}]",
                               seen_mechanics, seen_callout_ids_in_phase, report)

    # shared boss-level mechanics have their own id scope
    seen_shared: Set[str] = set()
    seen_callouts_shared: Set[str] = set()
    for m_index, mechanic in enumerate(boss.get("mechanics") or []):
        _validate_mechanic(mechanic, f"{base}.shared.mechanics[{m_index}]",
                           seen_shared, seen_callouts_shared, report)


def _validate_mechanic(mechanic: Dict[str, Any], path: str,
                       seen_mechanics: Set[str],
                       seen_callout_ids: Set[str],
                       report: Report) -> None:
    mid = mechanic.get("mechanicId")
    mech_path = f"{path}[{mid if mid else '?'}]"
    if not mid:
        report.add(Issue(SEV_CRITICAL, "MISSING_FIELD",
                         "mechanicId required", mech_path))
        return
    if mid in seen_mechanics:
        report.add(Issue(SEV_CRITICAL, "DUPLICATE_ID",
                         f"duplicate mechanicId '{mid}'", mech_path))
    seen_mechanics.add(mid)
    if not mechanic.get("triggers"):
        report.add(Issue(SEV_CRITICAL, "NO_TRIGGERS",
                         "at least one trigger required", mech_path))

    cooldown = mechanic.get("cooldown")
    if cooldown is not None and cooldown < 0:
        report.add(Issue(SEV_CRITICAL, "COOLDOWN_NEGATIVE",
                         f"cooldown {cooldown} must be >= 0", mech_path))

    for t_index, trigger in enumerate(mechanic.get("triggers") or []):
        _validate_trigger(trigger, f"{mech_path}.triggers[{t_index}]", report)

    callouts = mechanic.get("callouts") or []
    if not callouts:
        report.add(Issue(SEV_WARNING, "NO_CALLOUTS",
                         "mechanic has no callouts (informational only)", mech_path))

    for c_index, callout in enumerate(callouts):
        _validate_callout(callout, f"{mech_path}.callouts[{c_index}]",
                          seen_callout_ids, report)


def _validate_trigger(trigger: Dict[str, Any], path: str, report: Report) -> None:
    ttype = trigger.get("type")
    if ttype not in KNOWN_TRIGGER_TYPES:
        report.add(Issue(SEV_CRITICAL, "UNKNOWN_TRIGGER_TYPE",
                         f"unknown trigger type '{ttype}'", path))


def _validate_callout(callout: Dict[str, Any], path: str,
                      seen_callout_ids: Set[str], report: Report) -> None:
    cid = callout.get("calloutId")
    if not cid:
        report.add(Issue(SEV_CRITICAL, "MISSING_FIELD",
                         "calloutId required", path))
        return
    if cid in seen_callout_ids:
        report.add(Issue(SEV_CRITICAL, "DUPLICATE_ID",
                         f"duplicate calloutId '{cid}'", path))
    seen_callout_ids.add(cid)
    if not callout.get("text"):
        report.add(Issue(SEV_CRITICAL, "MISSING_TEXT",
                         "callout text required (also TTS source)", path))

    category = callout.get("category")
    if category not in CATEGORIES:
        report.add(Issue(SEV_CRITICAL, "BAD_CATEGORY",
                         f"category must be one of {sorted(CATEGORIES)}, "
                         f"got '{category}'", path))

    priority = callout.get("priority")
    if priority is not None and not (1 <= priority <= 100):
        report.add(Issue(SEV_CRITICAL, "PRIORITY_RANGE",
                         f"priority {priority} must be 1-100", path))

    for offset_field in ("audioOffset", "visualOffset"):
        offset = callout.get(offset_field)
        if offset is not None and not (MIN_OFFSET <= offset <= MAX_OFFSET):
            report.add(Issue(SEV_CRITICAL, "OFFSET_RANGE",
                             f"{offset_field} {offset} must be "
                             f"{MIN_OFFSET}..{MAX_OFFSET}", path))
