# AGENTS.md — Project Coach

Guidance for AI coding agents (and humans) working in this repository.

## What this is

**Project Coach** is a RuneLite plugin for Old School RuneScape that provides
real-time, tick-accurate coaching for boss fights — visual overlays and audio
callouts driven by data-only JSON "encounter packs". It never simulates input;
all callouts are advisory.

## Read first

| Doc | Purpose |
|-----|---------|
| `docs/master-architecture.md` | Source of truth: vision, rules, components, schema |
| `docs/sprint-roadmap.md` | Build order: 34 sprints across 6 phases |
| `docs/implementation-guide.md` | Technical reference: class specs, JSON schema |
| `docs/integration.md` | Sentinel integration playbook (reference; see Backlog) |
| `docs/worklog.md` | Sprint history, decisions, deviations |

## Project rules (constitution — never violate)

1. The plugin never simulates input (no key presses, mouse, camera).
2. All callouts are advisory.
3. Encounter definitions are pure data (JSON) — no game logic in packs.
4. Packs must pass schema validation before loading.
5. Every callout has a visual AND audio option.
6. Tick timing is authoritative (1 tick = 600ms). Wrong-tick callouts are bugs.
7. Every feature ships with tests.
8. AI-generated encounter content requires human verification before publication.
9. Packs load from a user-writable directory, not bundled in the JAR.
10. No network calls in the plugin.
11. Audio = pre-recorded TTS `.ogg` files, generated offline during pack creation.

## Repo layout

```
coach/
├── docs/          # Planning + reference docs
├── plugin/        # RuneLite plugin (Gradle project) — see master-architecture §11
│   ├── build.gradle
│   └── src/main/java/com/coach/plugin/...
└── (future) knowledge-pipeline/, encounter-packs/
```

## Build & environment (Windows)

- **Java 11 required** (RuneLite standard). Installed at:
  `%USERPROFILE%\tools\jdk\jdk-11.0.32+9`
  (Chocolatey needs admin on this machine; JDK was installed as a user-level
  zip from Adoptium instead.)
- **Gradle**: use the wrapper (`plugin\gradlew.bat`, Gradle 7.6.4). A local
  Gradle install also exists at `%USERPROFILE%\tools\gradle\gradle-7.6.4`.
- Build command:

  ```powershell
  $env:JAVA_HOME = "$env:USERPROFILE\tools\jdk\jdk-11.0.32+9"
  .\gradlew.bat --no-daemon build   # from plugin/
  ```

- Output JAR: `plugin/build/libs/coach-<version>.jar`

## Code conventions

- Java 11, PascalCase classes, camelCase methods/variables, UPPER_SNAKE_CASE constants.
- Package root: `com.coach.plugin.{module}` (modules per implementation-guide §1).
- No external deps beyond what RuneLite provides unless documented in worklog.
- Tests: JUnit 5 + Mockito, headless, ≥80% coverage target (from Sprint 30).

## Sprint workflow

Every sprint follows this loop:

1. **Plan & scope** before building — confirm scope, ask questions if needed.
2. Implement against `docs/sprint-roadmap.md` acceptance criteria.
3. **Verify** — run the build/tests; manual in-game checks are flagged to the user.
4. **Log** the sprint in `docs/worklog.md` (what was done, decisions, deviations).
5. **Commit + push** after every completed sprint.

## Backlog

- **Sprint 30b — Sentinel Integration (replay-simulator CLI)**: After Sprint 30's
  test harness exists, package a standalone headless replay-simulator CLI that
  replays recorded fight JSON through the coaching engine and asserts expected
  callouts (live stage markers, self-exit, exit 0 / `RESULT=OK`). Wire into
  Sentinel per `docs/integration.md`: shim root `package.json` + `tools\*.cmd`
  wrapper, tester-only registration (no DOM features), facts block written only
  from verified live runs.
