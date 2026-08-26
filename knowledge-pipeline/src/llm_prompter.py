"""Prompt construction + generation entry point (roadmap Sprint 24).

Project Coach has no LLM API keys by design (plugin rule 10: no network in the
plugin; pipeline rule: no paid API dependency). Draft encounter JSON is instead
authored by the coding assistant during development, using the prompt artifacts
in src/prompts/ and the deterministic scaffolding in json_generator.py.

This module therefore provides:
- build_system_prompt() / build_user_prompt(): exact payloads used for
  generation, kept under version control so drafts are reproducible.
- generate_draft(): an injectable-complete_fn seam. The default complete_fn is
  the local heuristic generator; a future remote LLM can be slotted in without
  touching callers.
"""
from __future__ import annotations

from pathlib import Path
from typing import Callable, Dict, List

from models import Boss

PROMPTS_DIR = Path(__file__).parent / "prompts"


def build_system_prompt() -> str:
    return (PROMPTS_DIR / "system_prompt.txt").read_text(encoding="utf-8")


def build_mechanic_prompt() -> str:
    return (PROMPTS_DIR / "mechanic_extraction_prompt.txt").read_text(encoding="utf-8")


def build_user_prompt(boss: Boss) -> str:
    """Render extracted wiki data as the user-side prompt payload."""
    lines = [f"Boss: {boss.name}",
             f"NPC ids: {', '.join(str(i) for i in boss.npc_ids)}",
             ""]
    for phase in boss.phases:
        lines.append(f"## {phase.title}")
        if phase.description:
            lines.append(phase.description)
        for shout in phase.shouts:
            lines.append(f"(phase telegraph: {shout})")
        for mechanic in phase.mechanics:
            shouts = " | shouts: " + "; ".join(mechanic.shouts) if mechanic.shouts else ""
            lines.append(f"- {mechanic.name}: {mechanic.description}{shouts}")
        lines.append("")
    return "\n".join(lines)


CompleteFn = Callable[[str, str], Dict]  # (system_prompt, user_prompt) -> draft dict


def _local_heuristic_complete(system_prompt: str, user_prompt: str) -> Dict:
    """Deterministic offline stand-in for a hosted LLM.

    Delegates to json_generator.build_draft_from_boss via the payload's boss
    block. Kept here so callers always go through one entry point.
    """
    import json
    from json_generator import build_draft_from_boss_payload

    payload = json.loads(user_prompt.split("PAYLOAD:", 1)[1]) \
        if "PAYLOAD:" in user_prompt else None
    if payload is None:
        raise ValueError("local generator requires 'PAYLOAD:' json section")
    return build_draft_from_boss_payload(payload.get("boss") or payload)


def generate_draft(boss: Boss,
                   complete_fn: CompleteFn | None = None) -> Dict:
    """Produce a draft encounter JSON dict for the boss.

    complete_fn defaults to the local deterministic generator; tests and a
    future hosted-LLM path can inject alternatives.
    """
    from json_generator import boss_to_payload

    system_prompt = build_system_prompt()
    user_prompt = build_user_prompt(boss) + "\nPAYLOAD:\n" + __import__("json").dumps(
        {"boss": boss.to_dict()}, indent=1)

    fn = complete_fn or _local_heuristic_complete
    return fn(system_prompt, user_prompt)


def list_prompt_files() -> List[str]:
    return sorted(p.name for p in PROMPTS_DIR.glob("*.txt"))
