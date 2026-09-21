"""Sprint 28: end-to-end pipeline orchestration tests (offline)."""
import json
import shutil
import sys
from pathlib import Path

import pytest

SRC = Path(__file__).resolve().parents[1] / "src"
if str(SRC) not in sys.path:
    sys.path.insert(0, str(SRC))

import changelog_generator  # noqa: E402
import pipeline  # noqa: E402

FIXTURES = Path(__file__).resolve().parents[1] / "fixtures"
PAGES = FIXTURES / "sample_wiki_pages"


@pytest.fixture()
def seeded_cache(tmp_path: Path) -> Path:
    """Pre-seeded fetcher cache so no test touches the network."""
    cache = tmp_path / "cache"
    cache.mkdir()
    shutil.copy(PAGES / "nex.html", cache / "Nex.html")
    shutil.copy(PAGES / "nex_strategies.html", cache / "Nex__Strategies.html")
    return cache


@pytest.fixture()
def fake_synth():
    async def synth(text, voice, rate, mp3_path):
        mp3_path.write_bytes(b"FAKEAUDIO:" + text.encode("utf-8")[:8])
    return synth


@pytest.fixture(autouse=True)
def stub_ffmpeg(monkeypatch):
    import audio_generator
    monkeypatch.setattr(
        audio_generator, "mp3_to_ogg",
        lambda mp3, ogg, config: ogg.write_bytes(mp3.read_bytes()))


def base_config(tmp_path, seeded_cache, fake_synth, **overrides) -> pipeline.RunConfig:
    kwargs = dict(
        boss="Nex",
        cache_dir=seeded_cache,
        out_dir=tmp_path / "run",
        version="9.9.9",
        review="approved",
        review_notes="test run",
        synthesize=fake_synth,
        log_file=tmp_path / "run" / "pipeline.log",
    )
    kwargs.update(overrides)
    return pipeline.RunConfig(**kwargs)


def test_full_run_end_to_end_offline(tmp_path, seeded_cache, fake_synth):
    config = base_config(tmp_path, seeded_cache, fake_synth)
    assert pipeline.run_pipeline(config) == pipeline.EXIT_OK

    run = tmp_path / "run"
    assert (run / "draft.json").exists()
    assert (run / "validation_report.json").exists()
    assert (run / "pack.zip").exists()
    assert (run / "changelog.md").exists()
    assert (run / "pipeline.log").exists()

    draft = json.loads((run / "draft.json").read_text(encoding="utf-8"))
    assert draft["metadata"]["review_status"] == "approved"
    assert draft["metadata"]["version"] == "9.9.9"

    import zipfile
    with zipfile.ZipFile(run / "pack.zip") as archive:
        names = set(archive.namelist())
    assert "encounter.json" in names and "manifest.json" in names
    assert any(n.startswith("audio/") for n in names)

    changelog = (run / "changelog.md").read_text(encoding="utf-8")
    assert "Initial release" in changelog  # no --previous-draft given


def test_run_without_review_stops_then_resumes(tmp_path, seeded_cache, fake_synth):
    first = base_config(tmp_path, seeded_cache, fake_synth,
                        review=None, review_notes="")
    assert pipeline.run_pipeline(first) == pipeline.EXIT_NEEDS_REVIEW
    run = tmp_path / "run"
    assert (run / "draft.json").exists()
    assert not (run / "pack.zip").exists()
    assert "review_status" not in json.loads(
        (run / "draft.json").read_text(encoding="utf-8"))["metadata"]

    resume = pipeline.RunConfig(
        out_dir=run, resume_dir=run, review="approved",
        review_notes="late approval", synthesize=fake_synth,
        log_file=tmp_path / "run2.log")
    assert pipeline.run_pipeline(resume) == pipeline.EXIT_OK
    assert (run / "pack.zip").exists()
    draft = json.loads((run / "draft.json").read_text(encoding="utf-8"))
    assert draft["metadata"]["review_status"] == "approved"


def test_resume_without_review_decision_fails(tmp_path):
    resume = pipeline.RunConfig(out_dir=tmp_path / "empty",
                                resume_dir=tmp_path / "empty")
    assert pipeline.run_pipeline(resume) == pipeline.EXIT_FAILED


def test_bad_version_rejected(tmp_path, seeded_cache, fake_synth):
    config = base_config(tmp_path, seeded_cache, fake_synth, version="not-semver")
    assert pipeline.run_pipeline(config) == pipeline.EXIT_FAILED


def test_synthesis_failure_fails_gracefully_with_artifacts(
        tmp_path, seeded_cache):
    async def broken_synth(text, voice, rate, mp3_path):
        raise RuntimeError("tts exploded")

    config = base_config(tmp_path, seeded_cache, broken_synth)
    assert pipeline.run_pipeline(config) == pipeline.EXIT_FAILED
    run = tmp_path / "run"
    assert (run / "draft.json").exists(), "partial artifacts retained"
    assert (run / "validation_report.json").exists()
    assert not (run / "pack.zip").exists()


def test_main_entrypoint_runs_offline(tmp_path, seeded_cache, monkeypatch):
    async def fake_synth(text, voice, rate, mp3_path):
        mp3_path.write_bytes(b"x")

    import audio_generator
    monkeypatch.setattr(audio_generator, "mp3_to_ogg",
                        lambda mp3, ogg, config: ogg.write_bytes(b"x"))

    import pipeline as pipeline_module
    real_generate = audio_generator.generate_audio

    def patched_generate(draft, output_dir, **kwargs):
        kwargs["synthesize"] = fake_synth
        return real_generate(draft, output_dir, **kwargs)

    monkeypatch.setattr(pipeline_module, "generate_audio", patched_generate)

    argv = ["run", "--boss", "Nex",
            "--cache-dir", str(seeded_cache),
            "--out-dir", str(tmp_path / "cli"),
            "--version", "1.2.3",
            "--review", "approved",
            "--log-file", str(tmp_path / "cli.log")]
    assert pipeline_module.main(argv) == pipeline.EXIT_OK
    assert (tmp_path / "cli" / "pack.zip").exists()


# ---- changelog unit tests ----

def _mini_draft(callout_text: str, extra_mechanic: bool = False) -> dict:
    mechanics = [{
        "mechanicId": "m1", "name": "M1",
        "triggers": [{"triggerId": "t", "type": "shout", "containsText": "roar!"}],
        "callouts": [{"calloutId": "c1", "text": callout_text,
                      "category": "critical"}],
    }]
    if extra_mechanic:
        mechanics.append({
            "mechanicId": "m2", "name": "M2",
            "triggers": [{"triggerId": "t2", "type": "tick_timer", "tickMod": 4}],
            "callouts": [],
        })
    return {
        "schemaVersion": "1.0",
        "metadata": {"packId": "x", "name": "X", "version": "1.0.0",
                     "gameVersion": "230"},
        "bosses": [{
            "bossId": "x", "name": "X", "npcId": 1,
            "phases": [{
                "phaseId": "p", "name": "P",
                "entryTrigger": {"type": "npc_spawn", "npcIds": [1]},
                "exitTriggers": [],
                "mechanics": mechanics,
            }],
        }],
    }


def test_changelog_initial_release():
    text = changelog_generator.generate_changelog(
        _mini_draft("Run!"), None, "1.0.0")
    assert "Initial release" in text
    assert "Phases: 1" in text


def test_changelog_diffs_versions():
    old = _mini_draft("Run!")
    new = _mini_draft("Run faster!", extra_mechanic=True)
    text = changelog_generator.generate_changelog(new, old, "1.1.0")
    assert "mechanic added (m2)" in text
    assert "callout text changed (c1)" in text
    assert "packId: x" in text
