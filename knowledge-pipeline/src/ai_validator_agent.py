"""AI Validator Agent (roadmap Sprint 25).

Runs structural + logic validation over a draft, applies only the SAFE
auto-fixes, and produces a validation report where every issue is either
fixed (with a change-log entry) or left open for human review.

Auto-fixable codes (mechanical, no game knowledge needed):
- OFFSET_RANGE        clamp to -5..10
- PRIORITY_RANGE      clamp to 1..100
- BAD_CATEGORY        fall back to "info"
- COOLDOWN_NEGATIVE   set 0
- MISSING_TEXT        fall back to calloutId

Everything else (unknown trigger types, duplicate ids, missing entry triggers,
unreachable phases, placeholder/impossible triggers, PENDING_REVIEW markers,
missing audio) is reported for humans — guessing ids or restructuring phases
is exactly how bad packs get published.
"""
from __future__ import annotations

import copy
import time
from dataclasses import dataclass, field
from typing import Any, Dict, List, Optional

REVIEW_STATUSES = ("approved", "needs_work", "rejected")


def apply_review(draft: Dict[str, Any], status: str, notes: str = "") -> Dict[str, Any]:
    """Stamp a human review decision into pack metadata (mutates and returns
    the draft). Raises ValueError on unknown statuses — the approval workflow
    is enforced, never free-form."""
    if status not in REVIEW_STATUSES:
        raise ValueError(
            f"review status must be one of {list(REVIEW_STATUSES)}, got {status!r}")
    metadata = draft.setdefault("metadata", {})
    metadata["review_status"] = status
    metadata["review_notes"] = notes or ""
    metadata["reviewed_at"] = time.strftime("%Y-%m-%dT%H:%M:%S")
    return draft

import logic_validator
import schema_validator
from validation_report import Issue, Report


@dataclass
class AgentResult:
    report: Report
    fixed_draft: Dict[str, Any]
    changes: List[str] = field(default_factory=list)

    @property
    def fixed_count(self) -> int:
        return len(self.changes)

    def summary_lines(self) -> List[str]:
        lines = self.report.lines()
        lines.append(f"-- auto-fixes applied: {self.fixed_count}")
        for change in self.changes:
            lines.append(f"   * {change}")
        return lines


class AiValidatorAgent:
    def __init__(self, available_audio_files: Optional[set] = None):
        self.available_audio_files = available_audio_files

    def run(self, draft: Dict[str, Any]) -> AgentResult:
        working = copy.deepcopy(draft)
        changes: List[str] = []

        # pass 1: structural + logic issues on the original
        report = self._collect(working)
        if report.passed and not report.issues:
            return AgentResult(report, working, changes)

        # pass 2: apply safe fixes, recording what changed
        self._auto_fix(working, changes)

        # pass 3: re-validate; issues from pass 1 whose (code, path,
        # message) no longer appear were resolved by an auto-fix and are
        # re-added as fixed history entries
        final_report = self._collect(working)
        from collections import Counter
        from validation_report import STATUS_FIXED

        def signature(issue):
            return (issue.severity, issue.code, issue.path, issue.message)

        remaining = Counter(
            signature(i) for i in final_report.issues)
        for issue in report.issues:
            sig = signature(issue)
            if remaining.get(sig, 0) > 0:
                remaining[sig] -= 1  # still open: stays as-is
            else:
                fixed_copy = Issue(issue.severity, issue.code,
                                   issue.message, issue.path, STATUS_FIXED)
                final_report.add(fixed_copy)

        return AgentResult(final_report, working, changes)

    # ---- internals ----

    def _collect(self, draft: Dict[str, Any]) -> Report:
        combined = Report()
        for issue in schema_validator.validate(draft).issues:
            combined.add(issue)
        for issue in logic_validator.check(
                draft, self.available_audio_files).issues:
            combined.add(issue)
        return combined

    def _auto_fix(self, draft: Dict[str, Any], changes: List[str]) -> None:
        for boss in draft.get("bosses") or []:
            bid = boss.get("bossId", "?")
            for p_index, phase in enumerate(boss.get("phases") or []):
                phase_path = f"bosses[{bid}].phases[{p_index}]"
                for m_index, mechanic in enumerate(phase.get("mechanics") or []):
                    mech_path = f"{phase_path}.mechanics[{m_index}]"
                    cooldown = mechanic.get("cooldown")
                    if cooldown is not None and cooldown < 0:
                        mechanic["cooldown"] = 0
                        changes.append(f"{mech_path}.cooldown -> 0")

                    for c_index, callout in enumerate(mechanic.get("callouts") or []):
                        self._fix_callout(callout,
                                          f"{mech_path}.callouts[{c_index}]",
                                          changes)

    @staticmethod
    def _fix_callout(callout: Dict[str, Any], path: str, changes: List[str]) -> None:
        cid = callout.get("calloutId", "?")

        offset = callout.get("audioOffset")
        if offset is not None and not (-5 <= offset <= 10):
            clamped = max(-5, min(10, offset))
            callout["audioOffset"] = clamped
            changes.append(f"{path}.audioOffset {offset} -> {clamped}")

        offset = callout.get("visualOffset")
        if offset is not None and not (-5 <= offset <= 10):
            clamped = max(-5, min(10, offset))
            callout["visualOffset"] = clamped
            changes.append(f"{path}.visualOffset {offset} -> {clamped}")

        priority = callout.get("priority")
        if priority is not None and not (1 <= priority <= 100):
            clamped = max(1, min(100, priority))
            callout["priority"] = clamped
            changes.append(f"{path}.priority {priority} -> {clamped}")

        category = callout.get("category")
        if category not in ("critical", "warning", "info", "transition"):
            callout["category"] = "info"
            changes.append(f"{path}.category '{category}' -> info")

        if not callout.get("text"):
            fallback = cid if cid else "(missing text)"
            callout["text"] = fallback
            changes.append(f"{path}.text -> '{fallback}'")
