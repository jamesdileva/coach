# Project Coach — Developer Setup

Build, test, and contribute to the RuneLite plugin and knowledge pipeline
from a clean clone.

---

## 1. Prerequisites

| Tool | Version / notes |
|------|-----------------|
| JDK | **11** (RuneLite standard). This machine: `%USERPROFILE%\tools\jdk\jdk-11.0.32+9` (user-level Adoptium zip; Chocolatey needs admin) |
| Gradle | Use the **wrapper** only (`plugin/gradlew.bat`, Gradle 7.6.4). Local install also at `%USERPROFILE%\tools\gradle\gradle-7.6.4` |
| Git | Any recent git |
| Python (pipeline only) | 3.10+ recommended; see `knowledge-pipeline/README.md` |
| ffmpeg (audio) | On `PATH` for `.wav` TTS conversion |
| RuneLite (in-game) | For manual overlay/audio checks — **not** required for headless CI |

No network calls exist in the plugin itself (rule 10). Gradle resolves
dependencies from `maven.runelite.net` / Maven Central on first build.

## 2. Clone

```powershell
git clone https://github.com/jamesdileva/coach.git
cd coach
```

## 3. Build the plugin

```powershell
# From plugin/
$env:JAVA_HOME = "$env:USERPROFILE\tools\jdk\jdk-11.0.32+9"
.\gradlew.bat --no-daemon build
```

- Output JAR: `plugin/build/libs/coach-0.1.0-SNAPSHOT.jar`
- Group/artifact: `com.coach:coach` (see `plugin/build.gradle`)

### Tasks you’ll use

| Task | Purpose |
|------|---------|
| `gradlew compileJava` | Fast compile check |
| `gradlew test` | Unit + integration tests (JUnit 5) |
| `gradlew check` | `test` + **JaCoCo coverage gate** (LINE ≥ 0.80) |
| `gradlew build` | Full jar + check |
| `gradlew jacocoTestReport` | XML/HTML report → `plugin/build/reports/jacoco/test/` |

Coverage gate lives in `plugin/build.gradle` (`jacocoTestCoverageVerification`,
wired into `check`). Failing the gate fails the build — add tests, don’t
lower the bar without a worklog note.

## 4. Tests

- **Location:** `plugin/src/test/java/com/coach/plugin/...`
- **Stack:** JUnit 5 + Mockito 4.11, headless (no live client).
- **Target:** ≥80% line coverage (Sprint 30 gate); every feature ships with tests (rule 7).
- **Sim fixtures:** `plugin/src/test/resources/simulations/`
  (`nex_full_fight.json`, `inferno_full_run.json`)
- Final RuneLite event classes (`StatChanged`, `NpcSpawned`, …) must be
  **constructed for real** — Mockito cannot mock final classes without
  inline mock-maker.

```powershell
$env:JAVA_HOME = "$env:USERPROFILE\tools\jdk\jdk-11.0.32+9"
.\gradlew.bat --no-daemon check
```

Python pipeline tests (separate):

```bash
cd knowledge-pipeline
pip install -e ".[dev]"
pytest
```

## 5. Repo layout

```
coach/
├── AGENTS.md                 # Agent/human working agreement
├── README.md                 # Project overview
├── docs/                     # Architecture, roadmap, guides, worklog
│   ├── master-architecture.md
│   ├── sprint-roadmap.md
│   ├── implementation-guide.md
│   ├── worklog.md
│   ├── USER_GUIDE.md
│   ├── DEVELOPER_SETUP.md
│   ├── API_REFERENCE.md
│   └── examples/ENCODING.md
├── plugin/                   # Gradle RuneLite plugin
│   ├── build.gradle
│   └── src/main/java/com/coach/plugin/...
├── knowledge-pipeline/       # Python wiki → pack pipeline
└── encounter-packs/          # Source packs (template + shipped)
```

## 6. Conventions

- Java 11; packages under `com.coach.plugin.{module}` (implementation-guide §1).
- PascalCase types, camelCase members, `UPPER_SNAKE_CASE` constants.
- No new external deps beyond RuneLite’s unless documented in `docs/worklog.md`.
- Constitution (never violate): no input simulation; advisory callouts only;
  packs are pure data; schema validation before load; visual **and** audio
  option per callout; tick timing authoritative (600 ms); tests with every
  feature; human-verify AI pack content; packs load from user-writable dir;
  no network in plugin; TTS audio offline at pack-build time.

## 7. Sprint workflow

1. Plan & scope against `docs/sprint-roadmap.md`
2. Implement + tests
3. Verify: `gradlew check` green; flag manual in-game checks to the human
4. Log in `docs/worklog.md` (top, after `---`)
5. Commit + push

Read `AGENTS.md` first if you’re an AI agent.

## 8. Related docs

- Player guide: [`docs/USER_GUIDE.md`](USER_GUIDE.md)
- API reference: [`docs/API_REFERENCE.md`](API_REFERENCE.md)
- Pack authoring: [`docs/examples/ENCODING.md`](examples/ENCODING.md)
- Pipeline CLI: [`knowledge-pipeline/README.md`](../knowledge-pipeline/README.md)
- Worklog (decisions/deviations): [`docs/worklog.md`](worklog.md)
