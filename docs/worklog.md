# Project Coach — Worklog

Running log of sprints: what was done, key decisions, deviations from the docs.

---

## Sprint 1 — Project Scaffolding (2026-08-25)

**Objective:** RuneLite Gradle plugin project, build pipeline, plugin manifest,
minimal settings page. (Roadmap Sprint 1.)

### Done

- Installed Temurin **JDK 11.0.32** to `%USERPROFILE%\tools\jdk\jdk-11.0.32+9`.
- Scaffolded `plugin/` Gradle project:
  - `plugin/build.gradle` (RuneLite external-plugin setup, Java 11)
  - `plugin/settings.gradle`
  - Gradle wrapper 7.6.4 (`plugin/gradlew.bat`)
  - `src/main/resources/runelite-plugin.properties` (manifest)
  - `CoachPlugin.java` — lifecycle stubs logging start/shutdown
  - `config/CoachConfig.java` — settings page (Enabled, Debug Mode toggles)
  - Root `.gitignore`
- Created `AGENTS.md` (project rules, conventions, sprint workflow).

### Verified

- `gradlew.bat --no-daemon build` → **BUILD SUCCESSFUL**
- JAR produced: `plugin/build/libs/coach-0.1.0-SNAPSHOT.jar`

### Decisions

- **Repo layout:** plugin lives in `plugin/` subfolder (per architecture §11),
  leaving room for `knowledge-pipeline/` and `encounter-packs/` later.
- **Sentinel:** `docs/integration.md` kept as reference but NOT wired now — a
  RuneLite plugin has no launchable exe/DOM for Sentinel to drive. Deferred to
  **Sprint 30b** (see backlog): replay-simulator CLI becomes the Sentinel smoke
  target once the Sprint 30 test harness exists.
- Worklog lives at `docs/worklog.md`.

### Deviations from docs

- `implementation-guide.md` §1 lists `compileOnly "com.discordmc:runelite:runelite-source-1.10.13"`
  — these coordinates do not exist. Used the real-world RuneLite external-plugin
  setup instead: repo `https://repo.runelite.net`, dependency `net.runelite:client`
  (dynamic `latest.release`), which bundles `runelite-api`, Guice, Gson, SLF4J.
- Chocolatey could not install Temurin (no admin elevation available); installed
  the official Temurin 11 zip directly under `%USERPROFILE%\tools\jdk`. Build
  requires `JAVA_HOME` set to that path (documented in AGENTS.md).
- Skipped `proguard/proguard-rules.pro` for now (release packaging is Sprint 32).

### Manual testing (pending, user)

1. Copy `plugin/build/libs/coach-0.1.0-SNAPSHOT.jar` into RuneLite's sideboard
   plugins dir (or use the external-plugin loader).
2. Enable the Coach plugin → log shows "Project Coach started".
3. Disable it → log shows "Project Coach shut down".

---

## Backlog

- **Sprint 30b — Sentinel Integration (replay-simulator CLI):** Standalone
  headless CLI that replays recorded fight JSON through the coaching engine and
  asserts expected callouts fire on correct ticks (live stage markers,
  self-exit, `RESULT=OK` + exit 0). Sentinel wiring per `docs/integration.md`:
  shim root `package.json` + `tools\*.cmd` wrappers, tester-only registration,
  facts block from verified live runs only.
