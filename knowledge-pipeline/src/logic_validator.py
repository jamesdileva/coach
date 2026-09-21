"""Semantic/logic validation on top of the structural pass (roadmap Sprint 25).

Detects what pure structure cannot:
- phases unreachable from the sequential chain
- impossible triggers (placeholder ids, missing required fields per type)
- audio files referenced by callouts but absent from the pack
"""
from __future__ import annotations

import re
from typing import Any, Dict, List, Optional, Set

from validation_report import Issue, Report, SEV_CRITICAL, SEV_INFO, SEV_WARNING


def check(draft: Dict[str, Any],
          available_audio_files: Optional[Set[str]] = None) -> Report:
    report = Report()
    for boss in draft.get("bosses") or []:
        bid = boss.get("bossId", "?")
        _check_reachability(boss, f"bosses[{bid}]", report)
        for phase_index, phase in enumerate(boss.get("phases") or []):
            base = f"bosses[{bid}].phases[{phase_index}]"
            for m_index, mechanic in enumerate(phase.get("mechanics") or []):
                _check_mechanic(mechanic,
                                f"{base}.mechanics[{m_index}]",
                                available_audio_files, report)
    return report


def _check_reachability(boss: Dict[str, Any], base: str, report: Report) -> None:
    """Sequential-chain model: every phase after the first must be entered via
    the previous phase's exit trigger. A non-final phase with no exitTriggers
    makes everything after it unreachable."""
    phases = boss.get("phases") or []
    for index, phase in enumerate(phases):
        if index == 0:
            continue
        previous = phases[index - 1]
        exits = previous.get("exitTriggers") or []
        if not exits:
            pid = phase.get("phaseId", "?")
            prev_pid = previous.get("phaseId", "?")
            report.add(Issue(
                SEV_CRITICAL, "PHASE_UNREACHABLE",
                f"phase '{pid}' is unreachable — phase '{prev_pid}' has no "
                f"exit triggers and is not the final phase",
                path=f"{base}.phases[{index}]"))


def _check_mechanic(mechanic: Dict[str, Any], path: str,
                    available_audio_files: Optional[Set[str]],
                    report: Report) -> None:
    mid = mechanic.get("mechanicId", "?")

    for t_index, trigger in enumerate(mechanic.get("triggers") or []):
        tpath = f"{path}.triggers[{t_index}]"
        ttype = trigger.get("type")
        trigger_id = str(trigger.get("triggerId", ""))

        if trigger_id.startswith("PENDING_REVIEW_"):
            report.add(Issue(
                SEV_INFO, "PENDING_REVIEW",
                f"trigger needs a verified id before publication "
                f"(type={ttype})", tpath))
            continue

        if ttype in ("npc_spawn", "npc_despawn"):
            ids = trigger.get("npcIds") or (
                [trigger["npcId"]] if trigger.get("npcId") else [])
            if not ids:
                report.add(Issue(SEV_CRITICAL, "IMPOSSIBLE_TRIGGER",
                                 f"{ttype} trigger has no npcId/npcIds", tpath))
            elif any(npc_id == 0 for npc_id in ids):
                report.add(Issue(SEV_WARNING, "PLACEHOLDER_NPC_ID",
                                 f"{ttype} trigger contains placeholder npc id 0",
                                 tpath))

        elif ttype == "animation":
            anim = trigger.get("animationId")
            if anim is None:
                report.add(Issue(SEV_CRITICAL, "IMPOSSIBLE_TRIGGER",
                                 "animation trigger missing animationId", tpath))
            elif anim <= 0:
                report.add(Issue(SEV_CRITICAL, "IMPOSSIBLE_TRIGGER",
                                 f"animation trigger has placeholder id {anim}",
                                 tpath))

        elif ttype == "projectile":
            if not trigger.get("projectId"):
                report.add(Issue(SEV_CRITICAL, "IMPOSSIBLE_TRIGGER",
                                 "projectile trigger missing projectId", tpath))

        elif ttype == "graphic":
            if not trigger.get("graphicId"):
                report.add(Issue(SEV_CRITICAL, "IMPOSSIBLE_TRIGGER",
                                 "graphic trigger missing graphicId", tpath))

        elif ttype == "hp":
            if trigger.get("hpThreshold") is None:
                report.add(Issue(SEV_CRITICAL, "IMPOSSIBLE_TRIGGER",
                                 "hp trigger missing hpThreshold", tpath))
            if not (trigger.get("npcIds") or trigger.get("npcId")):
                report.add(Issue(SEV_CRITICAL, "IMPOSSIBLE_TRIGGER",
                                 "hp trigger missing npc target", tpath))

        elif ttype == "shout":
            if not trigger.get("containsText"):
                report.add(Issue(SEV_CRITICAL, "IMPOSSIBLE_TRIGGER",
                                 "shout trigger missing containsText", tpath))

        elif ttype == "location":
            region_fields = ("minX", "maxX", "minY", "maxY")
            if any(trigger.get(field) is None for field in region_fields):
                report.add(Issue(SEV_CRITICAL, "IMPOSSIBLE_TRIGGER",
                                 "location trigger missing region bounds", tpath))

        elif ttype == "wave_cleared":
            if not trigger.get("npcIds"):
                report.add(Issue(SEV_CRITICAL, "IMPOSSIBLE_TRIGGER",
                                 "wave_cleared trigger missing npcIds", tpath))

    for c_index, callout in enumerate(mechanic.get("callouts") or []):
        cid = callout.get("calloutId", "?")
        cpath = f"{path}.callouts[{c_index}]"
        audio = callout.get("audioFile")
        if audio and available_audio_files is not None \
                and audio not in available_audio_files:
            report.add(Issue(SEV_CRITICAL, "MISSING_AUDIO",
                             f"audio file '{audio}' not present in pack audio/",
                             cpath))


def referenced_npc_ids(draft: Dict[str, Any]) -> Set[int]:
    """All npc ids referenced anywhere in the draft (useful for the agent's
    cross-reference reports)."""
    ids: Set[int] = set()

    def collect(trigger: Dict[str, Any]) -> None:
        if isinstance(trigger.get("npcId"), int):
            ids.add(trigger["npcId"])
        for npc_id in trigger.get("npcIds") or []:
            if isinstance(npc_id, int):
                ids.add(npc_id)

    def walk_mechanics(mechanics: List[Dict[str, Any]]) -> None:
        for mechanic in mechanics:
            for trigger in mechanic.get("triggers") or []:
                collect(trigger)
            for condition in mechanic.get("conditions") or []:
                if isinstance(condition.get("npcId"), int):
                    ids.add(condition["npcId"])

    for boss in draft.get("bosses") or []:
        walk_mechanics(boss.get("mechanics") or [])
        for phase in boss.get("phases") or []:
            entry = phase.get("entryTrigger")
            if entry:
                collect(entry)
            for exit_trigger in phase.get("exitTriggers") or []:
                collect(exit_trigger)
            walk_mechanics(phase.get("mechanics") or [])
    return ids


def suspicious_placeholder_ids(draft: Dict[str, Any]) -> Set[int]:
    return {npc_id for npc_id in referenced_npc_ids(draft)
            if isinstance(npc_id, int) and npc_id <= 0}
