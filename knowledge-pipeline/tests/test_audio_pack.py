"""Sprint 27: audio generation + pack builder tests (no network)."""
import copy
import json
import sys
import zipfile
from pathlib import Path

import pytest

SRC = Path(__file__).resolve().parents[1] / "src"
if str(SRC) not in sys.path:
    sys.path.insert(0, str(SRC))

import audio_generator  # noqa: E402
import pack_builder  # noqa: E402
from ai_validator_agent import apply_review  # noqa: E402

FIXTURES = Path(__file__).resolve().parents[1] / "fixtures"
DRAFT = FIXTURES / "drafts" / "nex_draft.json"
DIST = FIXTURES / "dist" / "nex_1.0.0.zip"


@pytest.fixture(scope="module")
def draft() -> dict:
    return json.loads(DRAFT.read_text(encoding="utf-8"))


@pytest.fixture(scope="module")
def approved_draft(draft) -> dict:
    stamped = copy.deepcopy(draft)
    return apply_review(stamped, "approved", "fixture review")


# ---- config ----

def test_audio_config_loads_with_category_voices():
    config = audio_generator.load_audio_config()
    assert config["engine"] in ("edge", "kokoro")
    assert audio_generator.voice_for_category(config, "critical")
    assert audio_generator.voice_for_category(config, "bogus")  # falls back


def test_collect_callout_texts(draft):
    work = audio_generator.collect_callout_texts(draft)
    assert len(work) >= 10
    assert all(item["text"].strip() for item in work.values())
    assert all(item["file"].endswith(".ogg") for item in work.values())


# ---- review gate ----

def test_unreviewed_draft_refused(draft):
    assert draft["metadata"].get("review_status") != "approved"
    with pytest.raises(audio_generator.ReviewGateError):
        audio_generator.generate_audio(draft, Path("/nonexistent"),
                                       allow_unreviewed=False)


def test_explicit_override_bypasses_gate_for_fixtures(draft, tmp_path):
    async def fake_synth(text, voice, rate, mp3_path):
        mp3_path.write_bytes(b"FAKEAUDIO")

    real_convert = audio_generator.mp3_to_ogg
    audio_generator.mp3_to_ogg = lambda mp3, ogg, config: ogg.write_bytes(
        mp3.read_bytes())
    try:
        result = audio_generator.generate_audio(
            draft, tmp_path, synthesize=fake_synth, allow_unreviewed=True)
    finally:
        audio_generator.mp3_to_ogg = real_convert
    assert len(result.entries) == len(audio_generator.collect_callout_texts(draft))
    assert all((tmp_path / e.file).exists() for e in result.entries)


# ---- generation with fake synthesiser ----

@pytest.fixture()
def fake_audio_dir(tmp_path, approved_draft):
    async def fake_synth(text, voice, rate, mp3_path):
        mp3_path.write_bytes(b"FAKEAUDIO:" + text.encode("utf-8")[:16])

    real_convert = audio_generator.mp3_to_ogg
    audio_generator.mp3_to_ogg = lambda mp3, ogg, config: ogg.write_bytes(
        mp3.read_bytes())
    try:
        out = tmp_path / "audio"
        result = audio_generator.generate_audio(
            approved_draft, out, synthesize=fake_synth)
    finally:
        audio_generator.mp3_to_ogg = real_convert
    return out, result


def test_generation_writes_manifested_files(fake_audio_dir):
    out, result = fake_audio_dir
    assert result.total_bytes() > 0
    for entry in result.entries:
        assert (out / entry.file).stat().st_size == entry.bytes


def test_empty_callout_text_rejected(approved_draft, tmp_path):
    broken = copy.deepcopy(approved_draft)
    boss = broken["bosses"][0]
    boss["phases"][0]["mechanics"][0]["callouts"][0]["text"] = "   "

    async def fake_synth(text, voice, rate, mp3_path):
        mp3_path.write_bytes(b"x")

    real_convert = audio_generator.mp3_to_ogg
    audio_generator.mp3_to_ogg = lambda mp3, ogg, config: ogg.write_bytes(b"x")
    try:
        with pytest.raises(ValueError, match="empty text"):
            audio_generator.generate_audio(
                broken, tmp_path, synthesize=fake_synth)
    finally:
        audio_generator.mp3_to_ogg = real_convert


def test_oversize_audio_rejected(approved_draft, tmp_path):
    async def fat_synth(text, voice, rate, mp3_path):
        mp3_path.write_bytes(b"x" * 999999)

    real_convert = audio_generator.mp3_to_ogg
    audio_generator.mp3_to_ogg = lambda mp3, ogg, config: ogg.write_bytes(
        mp3.read_bytes())
    config = dict(audio_generator.load_audio_config())
    config["ogg"] = dict(config.get("ogg", {}), max_bytes=10)
    try:
        with pytest.raises(ValueError, match="limit"):
            audio_generator.generate_audio(
                approved_draft, tmp_path, config=config,
                synthesize=fat_synth)
    finally:
        audio_generator.mp3_to_ogg = real_convert


# ---- pack builder ----

def test_build_pack_assembles_zip_with_manifest(approved_draft, tmp_path):
    audio_dir = tmp_path / "audio"
    audio_dir.mkdir()
    referenced = pack_builder.collect_referenced_audio(approved_draft)
    assert referenced
    for audio_name in referenced:
        (audio_dir / audio_name).write_bytes(b"FAKEOGG")

    out_zip = tmp_path / "pack.zip"
    manifest = pack_builder.build_pack(
        _write_tmp_draft(approved_draft, tmp_path), audio_dir, out_zip,
        version="1.0.0")

    assert manifest["packId"] == "nex"
    assert manifest["version"] == "1.0.0"
    assert manifest["reviewStatus"] == "approved"
    with zipfile.ZipFile(out_zip) as archive:
        names = set(archive.namelist())
    assert "encounter.json" in names and "manifest.json" in names
    assert {f"audio/{name}" for name in referenced} <= names


def test_build_pack_refuses_missing_audio(approved_draft, tmp_path):
    audio_dir = tmp_path / "audio"
    audio_dir.mkdir()  # empty: every reference missing
    with pytest.raises(pack_builder.PackBuildError, match="missing"):
        pack_builder.build_pack(
            _write_tmp_draft(approved_draft, tmp_path), audio_dir,
            tmp_path / "pack.zip")


def test_build_pack_refuses_invalid_draft(tmp_path):
    bad = {"schemaVersion": "9.9", "metadata": {}, "bosses": []}
    draft_file = tmp_path / "bad.json"
    draft_file.write_text(json.dumps(bad), encoding="utf-8")
    audio_dir = tmp_path / "audio"
    audio_dir.mkdir()
    with pytest.raises(pack_builder.PackBuildError, match="schema"):
        pack_builder.build_pack(draft_file, audio_dir, tmp_path / "pack.zip")


def _write_tmp_draft(draft, tmp_path: Path) -> Path:
    draft_file = tmp_path / "draft.json"
    draft_file.write_text(json.dumps(draft), encoding="utf-8")
    return draft_file


# ---- committed distributable ----

def test_dist_zip_exists_and_validates_clean():
    assert DIST.exists(), "run the Sprint 27 build to produce fixtures/dist"
    with zipfile.ZipFile(DIST) as archive:
        names = archive.namelist()
        assert "encounter.json" in names
        assert "manifest.json" in names
        draft = json.loads(archive.read("encounter.json"))
        manifest = json.loads(archive.read("manifest.json"))
        audio_names = {n.split("/", 1)[1] for n in names
                       if n.startswith("audio/")}

    import logic_validator
    import schema_validator

    assert schema_validator.validate(draft).passed
    assert not [i for i in logic_validator.check(
        draft, available_audio_files=audio_names).issues
        if i.code == "MISSING_AUDIO"]

    import hashlib
    with zipfile.ZipFile(DIST) as archive:
        for entry in manifest["files"]:
            data = archive.read(entry["name"])
            assert len(data) == entry["bytes"]
            assert hashlib.sha256(data).hexdigest() == entry["sha256"]
            assert entry["bytes"] < 512000, f"{entry['name']} over size limit"
