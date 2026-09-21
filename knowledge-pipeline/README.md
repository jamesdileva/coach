# Coach Knowledge Pipeline

Turns OSRS Wiki pages into validated, reviewed, voiced encounter packs:

```
wiki HTML -> Boss data -> draft JSON -> validation -> human review
    -> TTS audio -> distributable pack.zip (+ changelog)
```

## Layout

| Path | Purpose |
|------|---------|
| `src/wiki_fetcher.py` | UA-tagged fetch with verbatim HTML caching |
| `src/wiki_parser.py` | infobox facts + phases/mechanics/shouts extraction |
| `src/models.py` | Boss/Phase/Mechanic dataclasses |
| `src/prompts/` | versioned generation rules (system + mechanic prompts) |
| `src/llm_prompter.py` | prompt payloads + `generate_draft()` (injectable LLM seam) |
| `src/json_generator.py` | deterministic draft builder + structural validation |
| `src/schema_validator.py` | full v1.0 structural mirror of the plugin loader rules |
| `src/logic_validator.py` | reachability, placeholder triggers, missing audio |
| `src/ai_validator_agent.py` | collect → safe auto-fixes → re-validate; `apply_review()` |
| `src/validation_report.py` | shared Issue/Report model |
| `src/human_review_interface/` | Flask review UI (`GET /review`, `/diff`, `/api/status`) |
| `src/audio_generator.py` | approved-only TTS → ffmpeg `.ogg` |
| `src/pack_builder.py` | validate → audio check → pack zip + manifest |
| `src/pipeline.py` | end-to-end CLI (this sprint) |
| `src/changelog_generator.py` | version-to-version diff notes |
| `fixtures/` | committed wiki HTML, generated drafts, dist zips |

## Install

```bash
pip install -e ".[dev]"        # pytest
pip install -e ".[review]"     # Flask review UI
pip install -e ".[pipeline]"   # edge-tts + pyyaml (ffmpeg must be on PATH)
```

## End-to-end usage

```bash
# 1. Full run up to the review gate (stops, exit 2, artifacts kept):
python src/pipeline.py run --boss "Nex" --out-dir dist/nex

# 2. Review in the browser:
python src/human_review_interface/app.py dist/nex/draft.json --port 8080
# ... approve with notes ...

# 3. Resume through audio + pack:
python src/pipeline.py run --resume dist/nex --review approved \
    --review-notes "verified in-game" --version 1.0.0

# One-shot with pre-decided review (CI-style), incl. changelog diff:
python src/pipeline.py run --boss "Nex" --out-dir dist/nex \
    --version 1.0.0 --review approved --previous-draft old/nex.json
```

Exit codes: `0` success, `1` stage failure (message + partial artifacts kept),
`2` awaiting human review.

## Supported bosses

Anything with a wiki page + strategies page works; the roadmap's six are
Nex, Inferno, Theatre of Blood, Tombs of Amascut, Chambers of Xeric, plus
whatever comes next. `--pages` overrides the default
`<boss>` + `<boss>/Strategies` pair.

## Rules the pipeline enforces

- No LLM API keys anywhere; drafts are authored against versioned prompts.
- Untelegraphed mechanics get `PENDING_REVIEW_*` triggers, never guessed ids.
- Only `approved` drafts proceed to audio generation.
- Audio is pre-recorded TTS `.ogg`, generated offline at pack-build time.
- Every pack ships `manifest.json` (files, sizes, SHA-256, review status).
