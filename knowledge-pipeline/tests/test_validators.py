"""Sprint 25: validation tools + AI validator agent tests."""
import json
from pathlib import Path

import pytest

import ai_validator_agent
import json_generator
import logic_validator
import schema_validator
from validation_report import Issue, Report, SEV_CRITICAL, SEV_INFO, SEV_WARNING

FIXTURES = Path(__file__).resolve().parents[1] / "fixtures"


@pytest.fixture(scope="module")
def nex_draft() -> dict:
    return json.loads((FIXTURES / "drafts" / "nex_draft.json").read_text(encoding="utf-8"))


def test_report_model_counts_and_pass():
    report = Report()
    report.add(Issue(SEV_CRITICAL, "A", "a"))
    report.add(Issue(SEV_INFO, "B", "b"))
    assert not report.passed
    assert report.critical_count == 1 and report.info_count == 1


# ---- schema validator ----

def test_schema_validator_clean_on_generated_draft(nex_draft):
    issues = schema_validator.validate(nex_draft)
    criticals = [i for i in issues.issues if i.severity == SEV_CRITICAL]
    assert criticals == [], [i.message for i in criticals]


def test_schema_validator_catches_duplicates_and_ranges():
    draft = {
        "schemaVersion": "1.0",
        "metadata": {"packId": "x", "name": "X", "version": "0.1.0", "gameVersion": "230"},
        "bosses": [{
            "bossId": "b", "name": "B", "npcId": 1,
            "phases": [
                {"phaseId": "p", "name": "P",
                 "entryTrigger": {"type": "npc_spawn", "npcIds": [1]},
                 "mechanics": [
                     {"mechanicId": "m", "name": "M",
                      "triggers": [{"triggerId": "t", "type": "shout", "containsText": "x"}],
                      "callouts": [
                          {"calloutId": "c", "text": "t", "category": "urgent",
                           "priority": 500, "audioOffset": -50},
                          {"calloutId": "c", "text": "t2", "category": "info"},
                      ],
                      "cooldown": -4},
                 ]},
                # duplicate phase id AND duplicate mechanic id in second phase
                {"phaseId": "p", "name": "P2",
                 "entryTrigger": {"type": "npc_spawn", "npcIds": [1]},
                 "mechanics": [
                     {"mechanicId": "m", "name": "M2",
                      "triggers": [{"type": "tick_timer"}]},
                 ]},
            ]}],
    }
    codes = {issue.code for issue in schema_validator.validate(draft).issues}
    assert {"DUPLICATE_ID", "BAD_CATEGORY", "PRIORITY_RANGE",
            "OFFSET_RANGE", "COOLDOWN_NEGATIVE"} <= codes, codes


# ---- logic validator ----

def test_logic_flags_unreachable_phase():
    draft = {
        "schemaVersion": "1.0",
        "metadata": {"packId": "x", "name": "X", "version": "0.1.0", "gameVersion": "230"},
        "bosses": [{
            "bossId": "b", "name": "B", "npcId": 7,
            "phases": [
                {"phaseId": "p1", "name": "P1",
                 "entryTrigger": {"type": "npc_spawn", "npcIds": [7]},
                 "exitTriggers": [],   # no exit -> p2 unreachable
                 "mechanics": []},
                {"phaseId": "p2", "name": "P2",
                 "entryTrigger": {"type": "npc_spawn", "npcIds": [7]},
                 "exitTriggers": [], "mechanics": []},
            ]}],
    }
    codes = {issue.code for issue in logic_validator.check(draft).issues}
    assert "PHASE_UNREACHABLE" in codes


def test_logic_flags_placeholder_and_impossible_triggers():
    draft = {
        "schemaVersion": "1.0",
        "metadata": {"packId": "x", "name": "X", "version": "0.1.0", "gameVersion": "230"},
        "bosses": [{
            "bossId": "b", "name": "B", "npcId": 7,
            "phases": [{
                "phaseId": "p", "name": "P",
                "entryTrigger": {"type": "npc_spawn", "npcIds": [7]},
                "exitTriggers": [],
                "mechanics": [
                    {"mechanicId": "pending", "name": "Pending",
                     "triggers": [{"triggerId": "PENDING_REVIEW_x",
                                   "type": "animation", "animationId": -1}]},
                    {"mechanicId": "bad_shout", "name": "BadShout",
                     "triggers": [{"type": "shout"}]},
                ]}]}],
    }
    issues = logic_validator.check(draft).issues
    codes = {issue.code for issue in issues}
    assert "PENDING_REVIEW" in codes
    assert "IMPOSSIBLE_TRIGGER" in codes
    pending = [i for i in issues if i.code == "PENDING_REVIEW"]
    assert pending[0].severity == SEV_INFO


def test_logic_detects_missing_audio_files(nex_draft):
    missing = logic_validator.check(nex_draft,
                                    available_audio_files=set()).issues
    missing_audio = [i for i in missing if i.code == "MISSING_AUDIO"]
    assert len(missing_audio) >= 10, "nex draft references many audio files"

    # collect the exact audio names the draft references, then verify no
    # false criticals when the pack ships every referenced file:
    referenced = set()
    for boss in nex_draft["bosses"]:
        for phase in boss["phases"]:
            for mechanic in phase["mechanics"]:
                for callout in mechanic.get("callouts") or []:
                    if callout.get("audioFile"):
                        referenced.add(callout["audioFile"])
    assert referenced, "draft should reference audio files"
    full_pack = logic_validator.check(nex_draft, available_audio_files=referenced)
    assert not [i for i in full_pack.issues if i.code == "MISSING_AUDIO"]


# ---- agent ----

def test_agent_auto_fixes_mechanical_issues_and_reports():
    draft = {
        "schemaVersion": "1.0",
        "metadata": {"packId": "x", "name": "X", "version": "0.1.0", "gameVersion": "230"},
        "bosses": [{
            "bossId": "b", "name": "B", "npcId": 7,
            "phases": [{
                "phaseId": "p", "name": "P",
                "entryTrigger": {"type": "npc_spawn", "npcIds": [7]},
                "exitTriggers": [],
                "mechanics": [{
                    "mechanicId": "m", "name": "M",
                    "triggers": [{"triggerId": "t", "type": "shout", "containsText": "roar!"}],
                    "callouts": [{
                        "calloutId": "c", "category": "urgent",
                        "priority": 900, "audioOffset": -99,
                    }],
                    "cooldown": -2,
                }]}]}],
    }
    result = ai_validator_agent.AiValidatorAgent().run(draft)

    callout = result.fixed_draft["bosses"][0]["phases"][0]["mechanics"][0]["callouts"][0]
    assert callout["priority"] == 100
    assert callout["audioOffset"] == -5
    assert callout["category"] == "info"
    assert callout["text"] == "c"
    assert result.fixed_draft["bosses"][0]["phases"][0]["mechanics"][0]["cooldown"] == 0

    assert result.fixed_count >= 5, result.changes  # cooldown + 4 callout fields
    assert result.report.passed, result.report.lines()


def test_agent_leaves_pending_review_open_for_humans(nex_draft):
    result = ai_validator_agent.AiValidatorAgent().run(nex_draft)
    open_pending = result.report.open_by_code("PENDING_REVIEW")
    assert len(open_pending) >= 3, "pending review is a human decision"

    json.dumps(result.report.to_json())  # must serialise cleanly
