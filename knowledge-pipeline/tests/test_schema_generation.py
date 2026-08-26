"""Sprint 24: draft generation + structural validation tests."""
import json
from pathlib import Path

import pytest

import json_generator
import llm_prompter
import wiki_parser

FIXTURES = Path(__file__).resolve().parents[1] / "fixtures"
PAGES = FIXTURES / "sample_wiki_pages"
DRAFT = FIXTURES / "drafts" / "nex_draft.json"


@pytest.fixture(scope="module")
def nex_boss() -> "wiki_parser.Boss":
    main = (PAGES / "nex.html").read_text(encoding="utf-8")
    strategies = (PAGES / "nex_strategies.html").read_text(encoding="utf-8")
    return wiki_parser.parse_boss(main, strategies)


@pytest.fixture(scope="module")
def generated(nex_boss) -> dict:
    return llm_prompter.generate_draft(nex_boss)


def test_prompt_artifacts_exist_and_mention_rules():
    system = llm_prompter.build_system_prompt()
    assert "PENDING_REVIEW" in system
    assert "audioOffset" in system
    assert "1 tick = 600ms" in system
    mechanic = llm_prompter.build_mechanic_prompt()
    assert "shout" in mechanic


def test_generated_draft_passes_validation(generated):
    assert json_generator.validate_draft(generated) == []


def test_draft_structure_matches_wiki_extraction(generated, nex_boss):
    boss = generated["bosses"][0]
    assert boss["npcId"] == 11278
    assert [p["phaseId"] for p in boss["phases"]] == [
        "smoke_phase", "shadow_phase", "blood_phase", "ice_phase", "zaros_phase",
    ]
    assert len(boss["phases"]) == len(nex_boss.phases)


def test_shouted_mechanics_get_shout_triggers(generated):
    smoke = generated["bosses"][0]["phases"][0]
    by_id = {m["mechanicId"]: m for m in smoke["mechanics"]}
    choke = by_id["choke"]
    trigger = choke["triggers"][0]
    assert trigger["type"] == "shout"
    assert "virus flow through you" in trigger["containsText"]
    # critical with pre-warning audio offset per system rules
    callout = choke["callouts"][0]
    assert callout["category"] == "critical"
    assert callout["audioOffset"] == -2


def test_untelegraphed_mechanics_flagged_pending_review(generated):
    smoke = generated["bosses"][0]["phases"][0]
    drag = next(m for m in smoke["mechanics"] if m["mechanicId"] == "drag")
    assert drag["triggers"][0]["triggerId"].startswith("PENDING_REVIEW_")

    zaros = generated["bosses"][0]["phases"][4]
    turmoil = next(m for m in zaros["mechanics"] if m["mechanicId"] == "turmoil")
    assert turmoil["triggers"][0]["triggerId"].startswith("PENDING_REVIEW_")

    draft = json.loads(DRAFT.read_text(encoding="utf-8"))
    assert json_generator.pending_review_count(draft) >= 3


def test_phase_exits_chain_by_hp_thresholds(generated):
    phases = generated["bosses"][0]["phases"]
    expected = [80, 60, 40, 20]
    for index, threshold in enumerate(expected):
        exit_trigger = phases[index]["exitTriggers"][0]
        assert exit_trigger["type"] == "hp"
        assert exit_trigger["hpThreshold"] == threshold
    assert phases[4]["exitTriggers"] == [], "Zaros terminal"


def test_validator_rejects_unknown_trigger_type():
    bad = {
        "schemaVersion": "1.0",
        "metadata": {"packId": "x", "name": "X", "version": "0.1.0", "gameVersion": "230"},
        "bosses": [{"bossId": "x", "name": "X", "npcId": 1,
                    "phases": [{"phaseId": "p", "name": "P",
                                "entryTrigger": {"type": "npc_spawn", "npcIds": [1]},
                                "exitTriggers": [],
                                "mechanics": [{"mechanicId": "m", "name": "M",
                                               "triggers": [{"type": "teleport"}]}]}]}],
    }
    issues = json_generator.validate_draft(bad)
    assertTrue = True  # keep linters calm about bare asserts in helpers
    assert any("unknown trigger type 'teleport'" in issue for issue in issues), issues


def test_deterministic_generation(nex_boss):
    first = json.dumps(llm_prompter.generate_draft(nex_boss), sort_keys=True)
    second = json.dumps(llm_prompter.generate_draft(nex_boss), sort_keys=True)
    assert first == second, "local generator must be deterministic"


def test_committed_fixture_matches_generator_output(nex_boss):
    committed = json.loads(DRAFT.read_text(encoding="utf-8"))
    fresh = llm_prompter.generate_draft(nex_boss)
    assert committed == fresh, (
        "committed nex_draft.json is stale — regenerate it")
