"""Changelog generation for encounter packs (roadmap Sprint 28).

Diffs a new draft against the previous version's draft JSON: added/removed
phases and mechanics, changed callout text. Without a previous draft it
writes an "initial release" summary instead.
"""
from __future__ import annotations

from typing import Any, Dict, List, Optional


def _index_mechanics(phase: Dict[str, Any]) -> Dict[str, Dict[str, Any]]:
    return {m.get("mechanicId", "?"): m for m in phase.get("mechanics") or []}


def _index_callouts(mechanic: Dict[str, Any]) -> Dict[str, str]:
    return {c.get("calloutId", "?"): c.get("text", "")
            for c in mechanic.get("callouts") or []}


def generate_changelog(new_draft: Dict[str, Any],
                       previous_draft: Optional[Dict[str, Any]],
                       version: str) -> str:
    new_bosses = new_draft.get("bosses") or []
    boss_name = new_bosses[0].get("name", "?") if new_bosses else "?"
    pack_id = (new_draft.get("metadata") or {}).get("packId", "?")

    lines = [f"# {boss_name} — {version}", ""]
    new_phases = new_bosses[0].get("phases", []) if new_bosses else []

    if previous_draft is None:
        lines.append("Initial release.")
        lines.append("")
        lines.append(f"Phases: {len(new_phases)} "
                     f"({', '.join(p.get('phaseId', '?') for p in new_phases)})")
        mechanics = sum(len(p.get("mechanics") or []) for p in new_phases)
        lines.append(f"Mechanics: {mechanics}")
        pending = sum(
            1 for p in new_phases for m in (p.get("mechanics") or [])
            for t in (m.get("triggers") or [])
            if str(t.get("triggerId", "")).startswith("PENDING_REVIEW_"))
        if pending:
            lines.append(f"Triggers awaiting verification: {pending}")
        lines.append("")
        return "\n".join(lines)

    old_bosses = previous_draft.get("bosses") or []
    old_phases = {p.get("phaseId", "?"): p
                  for p in (old_bosses[0].get("phases", []) if old_bosses else [])}
    new_phase_map = {p.get("phaseId", "?"): p for p in new_phases}

    added_phases = [pid for pid in new_phase_map if pid not in old_phases]
    removed_phases = [pid for pid in old_phases if pid not in new_phase_map]
    if added_phases:
        lines.append(f"Added phases: {', '.join(added_phases)}")
    if removed_phases:
        lines.append(f"Removed phases: {', '.join(removed_phases)}")
    if not added_phases and not removed_phases:
        lines.append("No phase changes.")

    changed: List[str] = []
    for pid, new_phase in new_phase_map.items():
        if pid not in old_phases:
            continue
        old_mechs = _index_mechanics(old_phases[pid])
        new_mechs = _index_mechanics(new_phase)
        for mid in sorted(set(new_mechs) - set(old_mechs)):
            changed.append(f"{pid}: mechanic added ({mid})")
        for mid in sorted(set(old_mechs) - set(new_mechs)):
            changed.append(f"{pid}: mechanic removed ({mid})")
        for mid in sorted(set(new_mechs) & set(old_mechs)):
            old_calls = _index_callouts(old_mechs[mid])
            new_calls = _index_callouts(new_mechs[mid])
            for cid in sorted(set(new_calls) - set(old_calls)):
                changed.append(f"{pid}/{mid}: callout added ({cid})")
            for cid in sorted(set(old_calls) - set(new_calls)):
                changed.append(f"{pid}/{mid}: callout removed ({cid})")
            for cid in sorted(set(new_calls) & set(old_calls)):
                if new_calls[cid] != old_calls[cid]:
                    changed.append(f"{pid}/{mid}: callout text changed ({cid})")

    lines.append("")
    if changed:
        lines.append("Changed:")
        lines.extend(f"- {entry}" for entry in changed)
    else:
        lines.append("No mechanic/callout changes.")
    lines.append("")
    lines.append(f"packId: {pack_id}")
    lines.append("")
    return "\n".join(lines)
