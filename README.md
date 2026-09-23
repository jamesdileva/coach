# Project Coach

Real-time, tick-accurate **boss coaching** for Old School RuneScape — a
[RuneLite](https://runelite.net/) plugin plus a Python knowledge pipeline.

Visual overlays and audio callouts are driven by data-only JSON **encounter
packs**. The plugin **never simulates input**; every callout is advisory.

## Features

- Tick-accurate callouts (1 tick = 600 ms) with optional audio/visual offsets  
- Phase machines, mechanic triggers, cooldowns, predictions  
- Overlay toolkit: prayer indicator, countdown, timeline, mini HUD, status  
- Accessibility: audio-only / visual-only, high contrast, text scale  
- User-writable pack directory (`<RuneLite>/coach/encounters/`)  
- Wiki → draft → review → TTS → pack **knowledge pipeline** (offline audio)  
- Headless test harness + fight-script replay (JaCoCo **≥80%** line gate)

## Repository layout

| Path | Purpose |
|------|---------|
| [`plugin/`](plugin/) | RuneLite plugin (Java 11, Gradle) |
| [`knowledge-pipeline/`](knowledge-pipeline/) | Wiki → validated pack pipeline |
| [`encounter-packs/`](encounter-packs/) | Pack sources (`template`, `nex`, `inferno`, …) |
| [`docs/`](docs/) | Architecture, roadmap, guides, worklog |
| [`AGENTS.md`](AGENTS.md) | Working agreement for humans and AI agents |

## Documentation

| Doc | Audience |
|-----|----------|
| [`CHANGELOG.md`](CHANGELOG.md) | Release notes (v1.0.0 + beta) |
| [`docs/BETA_GUIDE.md`](docs/BETA_GUIDE.md) | Beta testers — install, checklists, issue filing |
| [`docs/USER_GUIDE.md`](docs/USER_GUIDE.md) | Players — install, settings, troubleshooting |
| [`docs/examples/ENCODING.md`](docs/examples/ENCODING.md) | Pack authors — JSON schema, triggers, audio |
| [`docs/DEVELOPER_SETUP.md`](docs/DEVELOPER_SETUP.md) | Developers — build, test, conventions |
| [`docs/API_REFERENCE.md`](docs/API_REFERENCE.md) | Integrators — packages and key classes |
| [`docs/master-architecture.md`](docs/master-architecture.md) | Source of truth — vision, rules, components |
| [`docs/sprint-roadmap.md`](docs/sprint-roadmap.md) | 34-sprint build order |
| [`docs/implementation-guide.md`](docs/implementation-guide.md) | Technical reference |
| [`docs/worklog.md`](docs/worklog.md) | Sprint history, decisions, deviations |
| [`knowledge-pipeline/README.md`](knowledge-pipeline/README.md) | Pipeline CLI usage |
| [`release/hub/README.md`](release/hub/README.md) | Maintainer — plugin-hub PR steps |

## Quick start (players)

1. Enable **Coach** in RuneLite (hub when live, else local JAR — see user guide).  
2. Put pack zips in `<RuneLite dir>/coach/encounters/`.  
3. Configure volumes/categories under **Settings → Plugins → Coach**.  
4. Fight the boss — overlays and callouts fire on tick.

**Beta testers:** start with [`docs/BETA_GUIDE.md`](docs/BETA_GUIDE.md)
(install, pack verification checklist, GitHub issue templates).

Full steps: [`docs/USER_GUIDE.md`](docs/USER_GUIDE.md).

## Quick start (developers)

```powershell
# From plugin/ — requires JDK 11
$env:JAVA_HOME = "$env:USERPROFILE\tools\jdk\jdk-11.0.32+9"
.\gradlew.bat --no-daemon check
```

JAR output: `plugin/build/libs/coach-<version>.jar`.

Release package (ProGuard + size gate):

```powershell
# From plugin/ — or: powershell -File release\run.ps1
.\gradlew.bat --no-daemon buildRelease
# → plugin/build/release/coach-1.0.0.jar (+ hub stubs)
```

Details: [`docs/DEVELOPER_SETUP.md`](docs/DEVELOPER_SETUP.md).

## Project rules (constitution)

1. Never simulate input — advisory only  
2. All callouts have a **visual and** audio option  
3. Packs are pure data and must pass schema validation  
4. Tick timing is authoritative — wrong-tick callouts are bugs  
5. Every feature ships with tests  
6. No network calls in the plugin  
7. TTS audio is pre-recorded offline at pack-build time  

Full list: [`AGENTS.md`](AGENTS.md).

## License / status

**Beta / v1.0.0** — see [`CHANGELOG.md`](CHANGELOG.md) and git tag `v1.0.0`.
Hub submission steps: [`release/hub/README.md`](release/hub/README.md)
(manual PR to [runelite/plugin-hub](https://github.com/runelite/plugin-hub)).
Contributions: follow the sprint workflow in `AGENTS.md` and log work in
`docs/worklog.md`.
