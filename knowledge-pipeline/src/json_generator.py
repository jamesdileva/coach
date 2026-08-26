"""Draft encounter-JSON generation + structural validation (roadmap Sprint 24).

build_draft_from_boss_payload() deterministically converts extracted wiki data
(models.Boss.to_dict shape) into a schema v1.0 draft:

- one phase per "… phase" wiki section, chained by HP-threshold exits at
  80/60/40/20%
- mechanics with shouts -> shout triggers; without -> PENDING_REVIEW
  animation placeholders (never silent id guessing)
- phase-start prayer guidance callout for every phase

validate_draft() mirrors the plugin's load-time rules so drafts reach human
review already conformant (unique ids, known trigger types, tick offsets,
required fields).
"""
from __future__ import annotations

import re
from typing import Any, Dict, List, Optional, Tuple

SCHEMA_VERSION = "1.0"
GAME_VERSION = "230"

KNOWN_TRIGGER_TYPES = {
    "animation", "projectile", "graphic", "npc_spawn", "npc_despawn",
    "hp", "tick_timer", "player_state", "location", "shout", "wave_cleared",
    "custom", "composite",
}
CATEGORIES = ("critical", "warning", "info", "transition")

EXIT_THRESHOLDS = [80, 60, 40, 20]  # per elemental-phase convention


def slugify(text: str) -> str:
    return re.sub(r"[^a-z0-9]+", "_", text.lower()).strip("_")


def boss_to_payload(boss_dict: Dict[str, Any]) -> Dict[str, Any]:
    """Normalise a models.Boss.to_dict payload for the generator."""
    return {
        "name": boss_dict.get("name", ""),
        "npcIds": list(boss_dict.get("npcIds") or []),
        "phases": [
            {
                "title": phase.get("title", ""),
                "description": phase.get("description", ""),
                "shouts": list(phase.get("shouts") or []),
                "mechanics": [
                    {
                        "name": m.get("name", ""),
                        "description": m.get("description", ""),
                        "shouts": list(m.get("shouts") or []),
                    }
                    for m in (phase.get("mechanics") or [])
                ],
            }
            for phase in (boss_dict.get("phases") or [])
        ],
    }


def build_draft_from_boss_payload(payload: Dict[str, Any],
                                  pack_id: Optional[str] = None) -> Dict[str, Any]:
    name = payload["name"]
    npc_ids = payload["npcIds"]
    pack = pack_id or slugify(name) or "draft"
    phases_in = payload["phases"]

    phases_out: List[Dict[str, Any]] = []
    for index, phase in enumerate(phases_in):
        phase_slug = slugify(phase["title"]) or f"phase_{index + 1}"
        exit_triggers: List[Dict[str, Any]] = []
        if index < len(EXIT_THRESHOLDS) and index < len(phases_in) - 1:
            exit_triggers.append({
                "triggerId": f"{phase_slug}_exit",
                "type": "hp",
                "npcId": npc_ids[0] if npc_ids else 0,
                "hpThreshold": EXIT_THRESHOLDS[index],
                "hpDirection": "below",
            })

        mechanics: List[Dict[str, Any]] = [_phase_start_mechanic(phase, npc_ids)]

        for mechanic in phase["mechanics"]:
            mech_slug = slugify(mechanic["name"])
            if not mech_slug:
                continue
            shouts = mechanic["shouts"]
            if shouts:
                trigger = {
                    "triggerId": f"shout_{mech_slug}",
                    "type": "shout",
                    "containsText": shouts[0],
                    "senderName": name,
                }
            else:
                trigger = {
                    "triggerId": f"PENDING_REVIEW_anim_{mech_slug}",
                    "type": "animation",
                    "animationId": -1,
                }
            is_action_required = bool(shouts)
            mechanics.append({
                "mechanicId": mech_slug,
                "name": mechanic["name"],
                "triggers": [trigger],
                "callouts": [{
                    "calloutId": f"call_{mech_slug}",
                    "text": _imperative(mechanic),
                    "category": "critical" if is_action_required else "warning",
                    "priority": 90 if is_action_required else 65,
                    "audioOffset": -2 if is_action_required else 0,
                    "visualOffset": 0,
                    "visual": {"type": "text", "color": "#FF0000" if is_action_required else "#FF9800",
                               "durationTicks": 4},
                }],
                "cooldown": 12,
            })

        phases_out.append({
            "phaseId": phase_slug,
            "name": phase["title"],
            "entryTrigger": {
                "triggerId": f"{phase_slug}_entry",
                "type": "npc_spawn",
                "npcIds": npc_ids,
            },
            "exitTriggers": exit_triggers,
            "mechanics": mechanics,
        })

    return {
        "schemaVersion": SCHEMA_VERSION,
        "metadata": {
            "packId": pack,
            "name": name,
            "description": (
                "AI-generated draft from OSRS Wiki extraction. Human review "
                "required (rule 8). PENDING_REVIEW triggers need verified ids."
            ),
            "author": "Project Coach Knowledge Pipeline",
            "version": "0.1.0",
            "gameVersion": GAME_VERSION,
        },
        "bosses": [{
            "bossId": pack,
            "name": name,
            "npcId": npc_ids[0] if npc_ids else 0,
            "phases": phases_out,
        }],
    }


def _phase_start_mechanic(phase: Dict[str, Any], npc_ids: List[int]) -> Dict[str, Any]:
    phase_slug = slugify(phase["title"]) or "phase"
    start_shout = next((s for s in phase["shouts"] if s), None)

    if start_shout:
        trigger = {"triggerId": f"{phase_slug}_start_shout",
                   "type": "shout", "containsText": start_shout, "senderName": None}
    else:
        trigger = {"triggerId": f"PENDING_REVIEW_start_{phase_slug}",
                   "type": "animation", "animationId": -1}

    return {
        "mechanicId": f"{phase_slug}_start",
        "name": f"{phase['title']} guidance",
        "triggers": [trigger],
        "callouts": [{
            "calloutId": f"call_{phase_slug}_start",
            "text": f"{phase['title']} starting!",
            "category": "transition",
            "priority": 55,
            "audioOffset": -2,
            "visualOffset": 0,
            "visual": {"type": "text", "color": "#00E5FF", "durationTicks": 4},
        }],
        "cooldown": 100,
    }


def _imperative(mechanic: Dict[str, Any]) -> str:
    """Short overlay text derived from the mechanic name/description."""
    name = mechanic["name"].strip()
    desc = mechanic["description"].strip()
    # prefer the first sentence fragment of the description when informative
    first_sentence = re.split(r"(?<=[.!])\s+", desc)[0] if desc else ""
    if 0 < len(first_sentence) <= 90:
        return f"{name}! {first_sentence}"
    return f"{name}!"


# ---- structural validation mirroring plugin rules ----

def validate_draft(draft: Dict[str, Any]) -> List[str]:
    errors: List[str] = []

    if draft.get("schemaVersion") != SCHEMA_VERSION:
        errors.append(f"schemaVersion must be {SCHEMA_VERSION}")

    metadata = draft.get("metadata") or {}
    for field in ("packId", "name", "version", "gameVersion"):
        if not metadata.get(field):
            errors.append(f"metadata.{field} missing")

    bosses = draft.get("bosses") or []
    if not bosses:
        errors.append("at least one boss required")
        return errors

    for boss in bosses:
        bid = boss.get("bossId", "?")
        if not boss.get("phases"):
            errors.append(f"boss[{bid}] needs phases")
            continue
        seen_phase_ids = set()
        for phase in boss["phases"]:
            pid = phase.get("phaseId")
            if not pid:
                errors.append(f"boss[{bid}] phase missing phaseId")
                continue
            if pid in seen_phase_ids:
                errors.append(f"duplicate phaseId '{pid}' in boss {bid}")
            seen_phase_ids.add(pid)
            if not phase.get("entryTrigger"):
                errors.append(f"phase[{pid}] missing entryTrigger")

            seen_mechanics = set()
            for mechanic in phase.get("mechanics") or []:
                mid = mechanic.get("mechanicId", "?")
                if mid in seen_mechanics:
                    errors.append(f"duplicate mechanicId '{mid}' in boss {bid}")
                seen_mechanics.add(mid)
                if not mechanic.get("triggers"):
                    errors.append(f"mechanic[{mid}] needs triggers")
                for trigger in mechanic.get("triggers") or []:
                    ttype = trigger.get("type")
                    if ttype not in KNOWN_TRIGGER_TYPES:
                        errors.append(
                            f"mechanic[{mid}] unknown trigger type '{ttype}'")
                    if str(trigger.get("triggerId", "")).startswith("PENDING_REVIEW_"):
                        continue  # allowed in drafts; flagged for review
                for callout in mechanic.get("callouts") or []:
                    category = callout.get("category")
                    if category not in CATEGORIES:
                        errors.append(
                            f"callout[{callout.get('calloutId')}] bad category '{category}'")
                    for offset_field in ("audioOffset", "visualOffset"):
                        offset = callout.get(offset_field)
                        if offset is not None and not (-5 <= offset <= 10):
                            errors.append(
                                f"callout[{callout.get('calloutId')}] {offset_field} out of range")
    return errors


def pending_review_count(draft: Dict[str, Any]) -> int:
    count = 0
    for boss in draft.get("bosses", []):
        for phase in boss.get("phases", []):
            for mechanic in phase.get("mechanics", []):
                for trigger in mechanic.get("triggers", []):
                    if str(trigger.get("triggerId", "")).startswith("PENDING_REVIEW_"):
                        count += 1
    return count
