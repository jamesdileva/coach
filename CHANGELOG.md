# Changelog

All notable changes to **Project Coach** (RuneLite plugin).  
Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).  
Versions follow [Semantic Versioning](https://semver.org/).

## [1.0.0] — 2026-09-22

First stable release: tick-accurate boss coaching, six encounter packs,
knowledge pipeline, docs, and release tooling.

### Added — plugin

- **Encounter engine** — load user-writable `.zip` packs (`schemaVersion`
  `1.0`), schema validation, v0.9→1.0 migration, pack statuses
  (`LOADED` / `REJECTED` / `CONFLICT`).
- **Trigger engine** — animation, projectile, graphic, NPC spawn/despawn,
  HP edges, tick timer, player state, location, shout, wave-cleared,
  composite `AND`/`OR`.
- **Phase machine** — entry/exit triggers, per-phase mechanics, cooldowns,
  recovery on death/despawn.
- **Coaching engine** — priority queue, audio/visual dispatch with
  `audioOffset` / `visualOffset` (−5…+10 ticks), cooldowns, prediction
  countdown (≤5 ticks).
- **Overlays** — prayer indicator, countdown, phase timeline, mini HUD,
  status (HP %), safe-tile helper; debug overlay tabs
  (`EVENTS` / `TRIGGERS` / `STATE` / `TIMELINE` / `PROFILING`).
- **Audio** — category volumes, interrupt priority, mute, never-blocks
  visual path; offline TTS pipeline (`.wav` plays; `.ogg` validates).
- **Accessibility** — `BOTH` / `AUDIO_ONLY` / `VISUAL_ONLY`, essential-only,
  high-contrast palette, text scale 50–200%.
- **Profiles** — Learning / Practice / Performance defaults; import/export
  JSON.
- **Settings** — full `CoachConfig` surface (see `docs/USER_GUIDE.md`).
- **Logging & debug** — ring-buffer events, optional file log
  (`coach/logs/coach-debug.log`), auto debug-bundle export on disable
  (`coach/debug_logs/`).
- **Performance** — per-tick profiler, tick budgets, headless
  `TickReplayHarness` + `FightScript` JSON replay.
- **Testing** — 269 unit/integration/simulation tests; JaCoCo LINE ≥ 80%
  on `check`; Nex + Inferno sim fixtures.

### Added — packs (source in `encounter-packs/`)

| Pack | Boss |
|------|------|
| `nex.pack` | Nex |
| `inferno.pack` | Inferno |
| `toa.pack` | Tombs of Amascut |
| `cox.pack` | Chambers of Xeric |
| `sotetseg.pack` | Sotetseg (ToB) |
| `template.pack` | Community authoring template |

Pack zips are **not** bundled in the JAR (constitution rule 9) — copy
user-writable zips into `<RuneLite>/coach/encounters/`.

### Added — knowledge pipeline (`knowledge-pipeline/`)

- Wiki fetch → parse → draft → structural + logic validation → AI
  validator agent → human review UI (Flask) → TTS `.ogg` → pack zip +
  changelog. End-to-end CLI; 53 Python tests.

### Added — documentation (Sprint 31)

- `README.md`, `docs/USER_GUIDE.md`, `docs/DEVELOPER_SETUP.md`,
  `docs/API_REFERENCE.md`, `docs/examples/ENCODING.md`.

### Added — release (Sprint 32)

- Version **1.0.0**; `gradlew buildRelease`
  (check → ProGuard → optional jarsign → package → **&lt;5 MB** gate).
- `release/run.sh` / `release/run.ps1`.
- Hub-oriented `runelite-plugin.properties`
  (`displayName`, `author`, `support`, `build=gradle`).
- Hub submission stub generated at
  `plugin/build/release/hub-plugin.txt` (`repository=` + `commit=`).

### Security / constitution

- No network calls in the plugin.
- No input simulation — advisory callouts only.
- Packs are pure data; must pass schema validation.
- Every callout has a visual **and** audio option.
- Tick timing authoritative (600 ms); wrong-tick callouts are bugs.

### Known issues / limitations

- **`.ogg` callouts do not play in-game** (no Ogg decoder on the Java
  classpath) — visual callouts still fire; prefer `.wav` packs until a
  decoder ships.
- Several condition types validate but evaluate **false with a warning**:
  `prayer_active` / `prayer_inactive` / `inventory_contains` /
  `player_in_region` / `custom` (ConditionEvaluator default branch);
  `custom` triggers are not registered yet.
- Live NPC/animation ids in packs need human verification before
  publication (constitution rule 8); ids flagged in pack READMEs may drift
  with game updates.
- ProGuard `-dontobfuscate` keeps names for hub code review; shrink/optimize
  only. RuneLite hub PR is **not** part of this repo’s automated pipeline —
  submit `plugins/coach` manually (see Sprint 33).
- JAR signing is **optional** (`-Pcoach.keystore=...`); hub builds from
  source and does not require a signed jar.
- Manual walkthroughs (clean-client install, 6-boss smoke) remain for a
  human (roadmap Sprint 32 manual testing).

[1.0.0]: https://github.com/jamesdileva/coach/releases/tag/v1.0.0
