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
from dataclasses import dataclass, field
from typing import Any, Dict, List

import logic_validator
import schema_validator
from validation_report import Report, SEV_INFO


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

        # pass 3: re-validate; issues that disappeared are marked fixed
        final_report = self._collect(working)
        for code in {issue.code for issue in report.issues}:
            remaining = sum(1 for issue in final_report.issues
                            if issue.code == code and issue.status == "open")
            originally = sum(1 for issue in report.issues
                             if issue.code == code)
            still_open = originally - (
                sum(1 for issue in final_report.issues if issue.code == code))
            if still_open > 0:
                continue
            fixed_now = final_report.mark_fixed(code)
            if fixed_now:
                changes.append(f"resolved {code} x{fixed_now}")

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
