"""Shared issue/report model for the validation tools (roadmap Sprint 25).

Severities: critical (pack would misbehave or fail to load), warning
(suspicious, likely a mistake), info (needs human attention but is legal).
"""
from __future__ import annotations

import json
from dataclasses import dataclass, field, asdict
from typing import Any, Dict, List, Optional

SEV_CRITICAL = "critical"
SEV_WARNING = "warning"
SEV_INFO = "info"

STATUS_OPEN = "open"
STATUS_FIXED = "fixed"


@dataclass
class Issue:
    severity: str
    code: str
    message: str
    path: str = ""
    status: str = STATUS_OPEN

    def to_dict(self) -> Dict[str, Any]:
        return asdict(self)


@dataclass
class Report:
    issues: List[Issue] = field(default_factory=list)
    review: Optional[Dict[str, Any]] = None

    def add(self, issue: Issue) -> None:
        self.issues.append(issue)

    @property
    def critical_count(self) -> int:
        return sum(1 for i in self.issues if i.severity == SEV_CRITICAL)

    @property
    def warning_count(self) -> int:
        return sum(1 for i in self.issues if i.severity == SEV_WARNING)

    @property
    def info_count(self) -> int:
        return sum(1 for i in self.issues if i.severity == SEV_INFO)

    @property
    def passed(self) -> bool:
        """A pack passes review when nothing critical remains open."""
        return all(i.severity != SEV_CRITICAL or i.status == STATUS_FIXED
                   for i in self.issues)

    def open_by_code(self, code: str) -> List[Issue]:
        return [i for i in self.issues if i.code == code and i.status == STATUS_OPEN]

    def mark_fixed(self, code: str) -> int:
        fixed = 0
        for issue in self.issues:
            if issue.code == code and issue.status == STATUS_OPEN:
                issue.status = STATUS_FIXED
                fixed += 1
        return fixed

    def summary(self) -> Dict[str, Any]:
        by_severity = {SEV_CRITICAL: self.critical_count,
                       SEV_WARNING: self.warning_count,
                       SEV_INFO: self.info_count}
        by_status = {}
        for issue in self.issues:
            by_status[issue.status] = by_status.get(issue.status, 0) + 1
        return {
            "passed": self.passed,
            "counts": by_severity,
            "byStatus": by_status,
            "total": len(self.issues),
        }

    def to_json(self) -> str:
        payload: Dict[str, Any] = {
            "summary": self.summary(),
            "issues": [i.to_dict() for i in self.issues],
        }
        if self.review is not None:
            payload["review"] = self.review
        return json.dumps(payload, indent=1)

    def lines(self) -> List[str]:
        lines = []
        for issue in self.issues:
            prefix = {"critical": "!", "warning": "~", "info": "-"}[issue.severity]
            suffix = "" if issue.status == STATUS_OPEN else f" [{issue.status}]"
            path = f" @ {issue.path}" if issue.path else ""
            lines.append(f"{prefix} {issue.code}{path}: {issue.message}{suffix}")
        lines.append(f"-- passed={self.passed} "
                     f"(critical={self.critical_count} warning={self.warning_count} "
                     f"info={self.info_count})")
        return lines
