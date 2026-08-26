"""Extraction data models for the knowledge pipeline (roadmap Sprint 23).

Plain dataclasses with to_dict() so parsed wiki content serialises cleanly
to JSON for downstream pipeline stages.
"""
from dataclasses import dataclass, field
from typing import Any, Dict, List


@dataclass
class Mechanic:
    """A single special attack / notable behaviour."""

    name: str
    description: str = ""
    shouts: List[str] = field(default_factory=list)

    def to_dict(self) -> Dict[str, Any]:
        return {
            "name": self.name,
            "description": self.description,
            "shouts": list(self.shouts),
        }


@dataclass
class Phase:
    """A boss phase section (e.g. Nex's 'Smoke phase')."""

    title: str
    description: str = ""
    mechanics: List[Mechanic] = field(default_factory=list)
    shouts: List[str] = field(default_factory=list)

    def to_dict(self) -> Dict[str, Any]:
        return {
            "title": self.title,
            "description": self.description,
            "shouts": list(self.shouts),
            "mechanics": [m.to_dict() for m in self.mechanics],
        }


@dataclass
class Boss:
    """Parsed boss page: infobox facts plus phase/mechanic extraction."""

    name: str
    npc_ids: List[int] = field(default_factory=list)
    combat_level: str = ""
    max_hit: str = ""
    attack_styles: List[str] = field(default_factory=list)
    infobox: Dict[str, str] = field(default_factory=dict)
    phases: List[Phase] = field(default_factory=list)
    source_pages: List[str] = field(default_factory=list)

    def to_dict(self) -> Dict[str, Any]:
        return {
            "name": self.name,
            "npcIds": self.npc_ids,
            "combatLevel": self.combat_level,
            "maxHit": self.max_hit,
            "attackStyles": list(self.attack_styles),
            "infobox": dict(self.infobox),
            "phases": [p.to_dict() for p in self.phases],
            "sourcePages": list(self.source_pages),
        }
