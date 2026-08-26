"""Offline tests against the committed real wiki fixtures (Nex)."""
import json
from pathlib import Path

import pytest

import wiki_fetcher
import wiki_parser
from models import Boss

FIXTURES = Path(__file__).resolve().parents[1] / "fixtures" / "sample_wiki_pages"


@pytest.fixture(scope="module")
def boss() -> Boss:
    main = (FIXTURES / "nex.html").read_text(encoding="utf-8")
    strategies = (FIXTURES / "nex_strategies.html").read_text(encoding="utf-8")
    return wiki_parser.parse_boss(
        main, strategies, source_pages=["Nex", "Nex/Strategies"])


def test_infobox_extraction(boss: Boss):
    assert boss.name == "Nex"
    assert 11278 in boss.npc_ids
    assert boss.npc_ids == [11278, 11279, 11280, 11281, 11282]
    assert boss.combat_level == "1001"
    assert "Magic" in boss.attack_styles
    assert "Ranged" in boss.attack_styles


def test_all_five_phases_in_order(boss: Boss):
    titles = [phase.title for phase in boss.phases]
    assert titles == [
        "Smoke phase", "Shadow phase", "Blood phase", "Ice phase", "Zaros phase",
    ]


def test_special_attacks_extracted_with_shouts(boss: Boss):
    smoke = next(p for p in boss.phases if p.title == "Smoke phase")
    names = {m.name for m in smoke.mechanics}
    assert {"Choke", "Drag", "Smoke Dash"} <= names

    dash = next(m for m in smoke.mechanics if m.name == "Smoke Dash")
    assert any("NO ESCAPE" in shout for shout in dash.shouts)
    assert "50 damage" in dash.description

    blood = next(p for p in boss.phases if p.title == "Blood phase")
    sacrifice = next(m for m in blood.mechanics if m.name == "Blood Sacrifice")
    assert any("I demand a blood sacrifice" in s for s in sacrifice.shouts)


def test_zaros_wrath_shout_captured(boss: Boss):
    zaros = next(p for p in boss.phases if p.title == "Zaros phase")
    wrath = next(m for m in zaros.mechanics if m.name == "Wrath")
    assert any("Taste my wrath" in s for s in wrath.shouts)


def test_serialisation_round_trips_to_json(boss: Boss):
    data = json.loads(json.dumps(boss.to_dict()))
    assert data["name"] == "Nex"
    assert len(data["phases"]) == 5
    smoke = data["phases"][0]
    assert smoke["title"] == "Smoke phase"
    assert any(m["name"] == "Choke" for m in smoke["mechanics"])


# ---- fetcher ----

def test_fetcher_serves_from_cache(tmp_path: Path):
    fixture = (FIXTURES / "nex.html").read_text(encoding="utf-8")
    fetcher = wiki_fetcher.WikiFetcher(tmp_path / "cache")
    assert not fetcher.is_cached("Nex")

    # prime the cache manually — no network in tests
    cache_file = tmp_path / "cache" / "Nex.html"
    cache_file.write_text(fixture, encoding="utf-8")

    assert fetcher.is_cached("Nex")
    assert fetcher.fetch("Nex") == fixture
    assert fetcher._page_slug("Nex/Strategies") == "Nex__Strategies"


def test_fetcher_slug_is_safe_for_paths():
    assert wiki_fetcher.WikiFetcher._page_slug("Nex") == "Nex"
    assert wiki_fetcher.WikiFetcher._page_slug("Nex/Strategies") == "Nex__Strategies"
