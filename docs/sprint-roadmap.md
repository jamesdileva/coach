# Project Coach — Sprint Roadmap

> **Version:** 1.0
> **Status:** Draft — Pre-MVP
> **Audience:** Developers, AI coding agents, pack authors
> **Related:** See `docs/master-architecture.md` (architecture overview), `docs/implementation-guide.md` (technical reference)

This document is the **day-to-day build guide** for Project Coach. It breaks the MVP into 34 small, verifiable sprints. Each sprint is self-contained and designed to be completed and verified before moving to the next.

---

## Sprint Planning Methodology

Each sprint follows this template:

| Field | Description |
|-------|-------------|
| **Objective** | What this sprint accomplishes in one sentence |
| **Inputs** | Data, files, or context needed to start |
| **Outputs** | Deliverables produced by this sprint |
| **Files Created** | New files to create |
| **Files Modified** | Existing files to change |
| **Plugin Changes** | Java class additions/changes |
| **JSON Schema Changes** | Any schema version updates |
| **Acceptance Criteria** | Specific, testable conditions |
| **Manual Testing** | Step-by-step verification checklist |
| **Definition of Done** | What "done" means for this sprint |
| **Estimated Time** | Rough developer time (human or AI agent) |
| **Dependencies** | Which previous sprints must be done first |

---

## Sprint Phases Overview

| Phase | Sprints | Description |
|-------|---------|-------------|
| Phase 1 | 1–3 | Foundation: Gradle build, plugin skeleton, event bus, event subscriptions, debug logging |
| Phase 2 | 4–9 | Core Coaching: Encounter engine (JSON loader), Trigger engine (all trigger types), Coaching engine (priority, scheduling, cooldowns), state management, prediction |
| Phase 3 | 10–16 | Boss Support: JSON schema formalization, encounter pack loading, Nex, Inferno, ToB Soteboss implementations |
| Phase 4 | 17–22 | User Experience: Settings overhaul, overlay improvements, audio improvements, accessibility, debug tools, profiles |
| Phase 5 | 23–28 | AI Knowledge Pipeline: Wiki parser, LLM schema generation, validation tools, knowledge editor, packaging system |
| Phase 6 | 29–34 | Polish: Optimization, comprehensive testing, documentation, release preparation, community beta, v1.0 |

---

## Phase 1: Foundation (Sprints 1–3)

### Sprint 1 — Project Scaffolding

**Objective:** Set up the RuneLite Gradle plugin project, build pipeline, and plugin manifest.

**Inputs:**
- RuneLite plugin development guide
- JDK 11 (RuneLite's required version)
- OSRS game build version for compatibility

**Outputs:**
- Compile-ready Gradle project
- Plugin JAR builds successfully
- Plugin loads in RuneLite (appears in plugin hub)

**Files Created:**
- `build.gradle`
- `runelite-plugin.properties`
- `proguard/proguard-rules.pro`
- `src/main/java/com/coach/plugin/CoachPlugin.java`
- `src/main/resources/plugin.properties`

**Files Modified:** None.

**Plugin Changes:**
- Create `CoachPlugin` class extending `Plugin`
- Implement `startUp()` and `shutDown()` stubs
- Register plugin metadata (name, version, description)

**JSON Schema Changes:** None.

**Acceptance Criteria:**
- `gradle build` produces a `.jar` in `build/libs/`
- Plugin appears in RuneLite's External Media → Plugin Hub section
- Plugin can be enabled/disabled without errors
- `startUp()` and `shutDown()` log hello/goodbye messages

**Manual Testing:**
1. Clone the repo
2. Run `gradle build`
3. Place JAR in RuneLite's `plugins` directory
4. Start RuneLite, enable the plugin
5. Check the logs for startup/shutdown messages

**Definition of Done:** Plugin builds, installs, and loads cleanly in RuneLite. Logs show startup/shutdown.

**Estimated Time:** 45 minutes

**Dependencies:** None.

---

### Sprint 2 — Event System

**Objective:** Subscribe to core RuneLite game events and establish the internal Event Bus.

**Inputs:**
- RuneLite event API (`Tick`, `AnimationChanged`, `ProjectileSpawned`, `GraphicChanged`, `NpcSpawned`, `NpcDespawned`, `NpcHPChanged`, `PlayerHPChanged`)

**Outputs:**
- Working event subscriptions for all core game events
- Internal `EventBus` that fans out events to registered listeners
- Log output showing event counts per tick

**Files Created:**
- `src/main/java/com/coach/plugin/events/EventBus.java`
- `src/main/java/com/coach/plugin/events/GameEvent.java`
- `src/main/java/com/coach/plugin/events/GameStateBridge.java`

**Files Modified:**
- `CoachPlugin.java` (add `@Subscribe` handlers for each event)

**Plugin Changes:**
- `EventBus`: simple pub-sub dispatcher with tick-batching
- `GameEvent`: internal wrapper for all game events (carries tick number, event type, payload)
- `GameStateBridge`: translates RuneLite objects → internal `PlayerState` / `BossState` models
- `CoachPlugin`: add `@Subscribe` methods for 10 core events, log counts

**JSON Schema Changes:** None.

**Acceptance Criteria:**
- All 10 event types are received and logged
- `EventBus.post()` dispatches to registered listeners
- Tick batching works: events within the same 600ms tick arrive together
- No events dropped under normal gameplay
- `GameStateBridge` correctly extracts NPC ID, animation ID, HP, player position

**Manual Testing:**
1. Start RuneLite with plugin enabled + debug mode
2. Enter a boss fight (e.g., Giant Mole for projectile/animation testing)
3. Check debug overlay or log file: see events firing per tick
4. Verify tick batching: events in the same tick are logged together

**Definition of Done:** All 10 event types received and dispatched via EventBus. GameStateBridge extracts state correctly. No events dropped.

**Estimated Time:** 90 minutes

**Dependencies:** Sprint 1.

---

### Sprint 3 — Logging System + Debug Overlay

**Objective:** Implement debug logging and an in-game debug overlay for event inspection.

**Inputs:**
- RuneLite's `Overlay` system
- Completed event subscriptions from Sprint 2

**Outputs:**
- Rotating file logger (`coach-debug.log`)
- In-game debug overlay showing recent events + trigger activity
- Config toggle for debug mode

**Files Created:**
- `src/main/java/com/coach/plugin/logging/EventLogger.java`
- `src/main/java/com/coach/plugin/logging/TriggerLogger.java`
- `src/main/java/com/coach/plugin/logging/CalloutLogger.java`
- `src/main/java/com/coach/plugin/logging/FileLogWriter.java`
- `src/main/java/com/coach/plugin/overlay/DebugOverlay.java`

**Files Modified:**
- `CoachPlugin.java` (add debug config + debug overlay provider)
- `CoachConfig.java` (add debug toggle)

**Plugin Changes:**
- `EventLogger`: logs all game events with tick number
- `TriggerLogger`: logs trigger evaluations (fired / not fired)
- `CalloutLogger`: logs callout decisions (what was called, at what tick)
- `FileLogWriter`: writes logs to RuneLite's `logs/` directory (ring buffer, 1000 entries max)
- `DebugOverlay`: scrollable panel showing last 100 events + trigger results

**JSON Schema Changes:** None.

**Acceptance Criteria:**
- Debug mode toggle appears in plugin config
- Debug overlay appears on screen when enabled
- Log file (`coach-debug.log`) is written to disk with correct format
- Events are logged with tick number, event type, and payload summary
- Overlay shows last N events + any trigger evaluations

**Manual Testing:**
1. Enable debug mode in plugin config
2. Fight a mob for 30 seconds
3. Verify debug overlay shows event log
4. Stop RuneLite, check `logs/coach-debug.log` has entries
5. Disable debug mode, verify overlay disappears

**Definition of Done:** Logging system writes to file. Debug overlay displays events when enabled. All loggers are wired into EventBus.

**Estimated Time:** 75 minutes

**Dependencies:** Sprint 2.

---

## Phase 2: Core Coaching (Sprints 4–9)

### Sprint 4 — Encounter Engine (JSON Loader)

**Objective:** Build the Encounter Engine with JSON loading and schema validation.

**Inputs:**
- JSON schema for encounter packs (design in this sprint)
- Test encounter JSON file (minimal boss with one phase, one mechanic)
- RuneLite's `FileSystem` API for pack directory

**Outputs:**
- Encounter packs load from `<runelite>/coach/encounters/`
- JSON validated against schema before loading
- Runtime encounter objects created from JSON

**Files Created:**
- `src/main/java/com/coach/plugin/encounter/EncounterEngine.java`
- `src/main/java/com/coach/plugin/encounter/EncounterLoader.java`
- `src/main/java/com/coach/plugin/encounter/model/Encounter.java`
- `src/main/java/com/coach/plugin/encounter/model/Phase.java`
- `src/main/java/com/coach/plugin/encounter/model/Mechanic.java`
- `src/main/java/com/coach/plugin/encounter/model/Callout.java`
- `src/main/resources/schemas/encounter_schema_v1.json`

**Files Modified:**
- `CoachPlugin.java` (add pack loading in `startUp()`)
- `CoachConfig.java` (add pack directory path config)

**Plugin Changes:**
- `EncounterEngine`: main coordinator, holds all loaded encounters
- `EncounterLoader`: scans pack directory, validates JSON, creates runtime objects
- Model classes: `Encounter`, `Phase`, `Mechanic`, `Callout` (data classes matching JSON schema)
- Plugin loads all `.zip` packs from the encounters directory at startup

**JSON Schema Changes:** Initial schema version `1.0` defined — boss, phases, mechanics, triggers, callouts.

**Acceptance Criteria:**
- Plugin loads a valid `.zip` pack from encounters directory
- Invalid packs (bad JSON, schema mismatch) are rejected with a log message
- Runtime `Encounter` object has correct phases + mechanics
- Pack can be reloaded (config change without restart)

**Manual Testing:**
1. Create a test pack directory at `<runelite>/coach/encounters/`
2. Place a minimal valid `test.pack.zip` with `encounter.json`
3. Restart RuneLite, enable plugin
4. Check logs: "Loaded 1 encounter pack: test"
5. Place an invalid pack (bad JSON), verify it's rejected with error

**Definition of Done:** Encounter packs load from directory, JSON validated against schema, runtime objects created. Invalid packs rejected.

**Estimated Time:** 120 minutes

**Dependencies:** Sprint 3.

---

### Sprint 5 — Trigger Engine (Core Triggers)

**Objective:** Implement the Trigger Engine with Animation, Projectile, and Graphic trigger types.

**Inputs:**
- Event bus dispatches events from Sprint 2
- Encounter JSON with trigger definitions from Sprint 4

**Outputs:**
- TriggerEngine subscribes to EventBus
- Animation, Projectile, Graphic triggers evaluate correctly
- Triggers fire and notify the Encounter Engine

**Files Created:**
- `src/main/java/com/coach/plugin/trigger/TriggerEngine.java`
- `src/main/java/com/coach/plugin/trigger/TriggerEvaluator.java`
- `src/main/java/com/coach/plugin/trigger/AnimationTriggerEvaluator.java`
- `src/main/java/com/coach/plugin/trigger/ProjectileTriggerEvaluator.java`
- `src/main/java/com/coach/plugin/trigger/GraphicTriggerEvaluator.java`
- `src/main/java/com/coach/plugin/trigger/TriggerRegistry.java`

**Files Modified:**
- `CoachPlugin.java` (subscribe EventBus to TriggerEngine)

**Plugin Changes:**
- `TriggerEngine`: coordinates all trigger evaluation per tick
- `TriggerEvaluator` (interface): `evaluate(EventBatch batch) -> List<TriggerFire>`
- `AnimationTriggerEvaluator`: matches NPC/player animation IDs
- `ProjectileTriggerEvaluator`: matches projectile IDs + source NPC
- `GraphicTriggerEvaluator`: matches graphic IDs on NPCs/players
- `TriggerRegistry`: maps JSON trigger `type` strings to evaluator classes

**JSON Schema Changes:** Add `triggers` array to `mechanic` with `type`, `npcId`, `animationId`, `projectId`, `graphicId` fields.

**Acceptance Criteria:**
- Each trigger type matches correct game events
- Triggers fire once per event (not duplicated)
- Trigger results include tick number and matched event data
- Unknown trigger types are logged as warnings, not errors

**Manual Testing:**
1. Create a test encounter with an animation trigger for a known NPC
2. Fight that NPC in-game
3. Enable debug overlay, verify trigger fires when animation plays
4. Verify trigger does NOT fire for wrong NPC or wrong animation

**Definition of Done:** Animation, Projectile, and Graphic triggers all evaluate correctly. TriggerEngine dispatches fires to EncounterEngine.

**Estimated Time:** 120 minutes

**Dependencies:** Sprints 2, 4.

---

### Sprint 6 — Trigger Engine (Advanced Triggers)

**Objective:** Implement remaining trigger types: NPC Spawn/Despawn, HP, Tick Timer, Player State, Location, and Composite triggers.

**Inputs:**
- Core triggers from Sprint 5
- RuneLite's `WorldPoint` and `NPC` APIs for location + state triggers

**Outputs:**
- All 11 trigger types implemented
- Composite triggers support AND/OR logic
- Trigger evaluation is batched per tick

**Files Created:**
- `src/main/java/com/coach/plugin/trigger/NpcSpawnTriggerEvaluator.java`
- `src/main/java/com/coach/plugin/trigger/HpTriggerEvaluator.java`
- `src/main/java/com/coach/plugin/trigger/TickTimerTriggerEvaluator.java`
- `src/main/java/com/coach/plugin/trigger/PlayerStateTriggerEvaluator.java`
- `src/main/java/com/coach/plugin/trigger/LocationTriggerEvaluator.java`
- `src/main/java/com/coach/plugin/trigger/CustomRuleTriggerEvaluator.java`
- `src/main/java/com/coach/plugin/trigger/CompositeTriggerEvaluator.java`

**Files Modified:**
- `TriggerRegistry.java` (register new trigger types)

**Plugin Changes:**
- NPC Spawn/Despawn: matches NPC IDs on spawn/despawn events
- HP Trigger: matches NPC HP crossing a threshold (above/below)
- Tick Timer: matches `currentTick % interval == 0`
- Player State: matches player animation, HP, prayer, position, inventory contents
- Location: matches player entering a tile region
- Custom Rule: evaluates a condition definition (see ConditionEvaluator from Phase 2, Sprint 7)
- Composite: AND/OR combinations of multiple triggers

**JSON Schema Changes:** Add schema definitions for each new trigger type's fields.

**Acceptance Criteria:**
- All 11 trigger types match correct events
- Composite triggers correctly evaluate AND/OR logic
- Tick timer triggers fire on correct tick boundary
- HP triggers fire when HP crosses threshold (not every tick while below)
- Location triggers respect region boundaries

**Manual Testing:**
1. Create test encounters for each trigger type
2. Fight or simulate each scenario
3. Verify debug overlay shows correct trigger fires
4. Test a composite trigger (e.g., "NPC spawns AND player in region")

**Definition of Done:** All 11 trigger types implemented and tested. Composite triggers work. TriggerEngine handles all types without errors.

**Estimated Time:** 150 minutes

**Dependencies:** Sprint 5.

---

### Sprint 7 — Encounter Engine (Phase Machine + Conditions)

**Objective:** Implement the phase state machine, condition evaluation, and mechanic state tracking.

**Inputs:**
- Trigger fires from Trigger Engine
- Encounter JSON with phases, entry/exit conditions, mechanics

**Outputs:**
- Phase transitions happen when exit conditions are met
- Active mechanics tracked per phase
- Recovery logic handles edge cases (player death, boss reset)

**Files Created:**
- `src/main/java/com/coach/plugin/encounter/PhaseMachine.java`
- `src/main/java/com/coach/plugin/encounter/MechanicManager.java`
- `src/main/java/com/coach/plugin/encounter/ConditionEvaluator.java`
- `src/main/java/com/coach/plugin/encounter/RecoveryHandler.java`

**Files Modified:**
- `EncounterEngine.java` (integrate PhaseMachine + MechanicManager)

**Plugin Changes:**
- `PhaseMachine`: manages current phase, evaluates entry/exit conditions, handles transitions
- `MechanicManager`: tracks which mechanics are active, their cooldowns, their tick timers
- `ConditionEvaluator`: evaluates JSON condition definitions (NPC HP, player state, tick mod, etc.)
- `RecoveryHandler`: resets encounter state on player death, boss despawn, or phase skip

**JSON Schema Changes:** Add `conditions` to triggers and phases. Add `recovery` section to boss definition.

**Acceptance Criteria:**
- Phase transitions fire when exit conditions are met
- Mechanics activate when their triggers fire
- Mechanics respect cooldown (don't fire twice within cooldown period)
- Recovery logic resets state correctly on death/despawn
- Unknown conditions are logged as warnings

**Manual Testing:**
1. Create a 2-phase test encounter with a clear exit condition
2. Fight and verify phase transition happens correctly
3. Force a player death, verify recovery logic resets the encounter
4. Verify mechanic cooldown is respected

**Definition of Done:** Phase machine transitions correctly. Mechanic states tracked. Recovery handles edge cases.

**Estimated Time:** 100 minutes

**Dependencies:** Sprints 4, 6.

---

### Sprint 8 — Coaching Engine (Priority + Queue)

**Objective:** Build the Coaching Engine with priority resolution, callout scheduling, and queue management.

**Inputs:**
- Mechanic activations from Encounter Engine
- Player state from GameStateBridge
- Configuration (callout toggles, timing offsets)

**Outputs:**
- Callouts scheduled with tick-precise timing
- Priority system resolves conflicts (critical vs. informational)
- Queue management prevents callout spam

**Files Created:**
- `src/main/java/com/coach/plugin/coaching/CoachingEngine.java`
- `src/main/java/com/coach/plugin/coaching/PriorityResolver.java`
- `src/main/java/com/coach/plugin/coaching/CalloutScheduler.java`
- `src/main/java/com/coach/plugin/coaching/CooldownManager.java`
- `src/main/java/com/coach/plugin/coaching/CalloutQueue.java`
- `src/main/java/com/coach/plugin/coaching/CoachStateManager.java`

**Files Modified:**
- `CoachPlugin.java` (wire CoachingEngine into EventBus)

**Plugin Changes:**
- `CoachingEngine`: receives mechanic activations, decides what to call out
- `PriorityResolver`: assigns priority (1-100) to callouts based on category + urgency
- `CalloutScheduler`: schedules callouts at tick offsets (e.g., "2 ticks before impact")
- `CooldownManager`: prevents duplicate callouts within a cooldown window
- `CalloutQueue`: FIFO queue with priority reordering, supports interruption
- `CoachStateManager`: holds current player + boss state for decision making

**JSON Schema Changes:** Add `priority`, `audioOffset`, `visualOffset`, `category` to callout definitions.

**Acceptance Criteria:**
- Critical callouts preempt informational ones
- Callouts scheduled at correct tick offsets
- Duplicate callouts suppressed by cooldown
- Queue handles concurrent callouts without overlap
- Callouts respect per-callout enable/disable toggles

**Manual Testing:**
1. Create a test encounter with multiple simultaneous mechanics
2. Fight and verify high-priority callout fires, low-priority is queued
3. Trigger two of the same callout quickly, verify second is suppressed
4. Check debug overlay shows scheduled callouts with correct tick offsets

**Definition of Done:** Coaching Engine schedules, prioritizes, and queues callouts correctly. Priority + cooldown logic works.

**Estimated Time:** 120 minutes

**Dependencies:** Sprints 7, 3.

---

### Sprint 9 — Coaching Engine (Prediction + Audio/Visual Dispatch)

**Objective:** Add prediction (anticipating upcoming mechanics) and wire up audio + visual callout dispatch.

**Inputs:**
- Scheduled callouts from Coaching Engine
- Loaded audio files from encounter packs

**Outputs:**
- Next-mechanic prediction displayed to player
- Audio callouts play via AudioEngine
- Visual callouts render via OverlayManager

**Files Created:**
- `src/main/java/com/coach/plugin/coaching/PredictionEngine.java`
- `src/main/java/com/coach/plugin/audio/AudioEngine.java`
- `src/main/java/com/coach/plugin/overlay/OverlayManager.java`
- `src/main/java/com/coach/plugin/overlay/CoachOverlay.java`

**Files Modified:**
- `CoachingEngine.java` (add PredictionEngine integration)
- `CoachPlugin.java` (register overlays, wire audio)

**Plugin Changes:**
- `PredictionEngine`: looks ahead in the encounter timeline, predicts next mechanic for "next callout" display
- `AudioEngine`: receives audio callout requests, plays `.ogg` files with timing offsets
- `OverlayManager`: coordinates which overlays to render on the screen
- `CoachOverlay`: main RuneLite `Overlay` implementation that delegates to OverlayManager

**JSON Schema Changes:** Add `visual` type (prayer icon, countdown, text) to callout definitions. Add `prediction` flag to mechanics.

**Acceptance Criteria:**
- Audio callouts play when CoachingEngine requests them
- Visual overlays render on screen (text + icons)
- Prediction shows next mechanic + timing
- Audio + visual callouts fire together (same callout)
- Mute toggle suppresses all callouts
- Audio timing offset is configurable and within ±1 tick

**Manual Testing:**
1. Run a test encounter with known callout timings
2. Verify audio plays at correct moment
3. Verify visual overlay appears with correct text/icon
4. Check prediction shows next mechanic before it fires
5. Disable a callout type, verify it doesn't fire

**Definition of Done:** Audio + visual callouts fire correctly from CoachingEngine. Prediction works. CoachingEngine fully integrated with Trigger + Encounter engines.

**Estimated Time:** 135 minutes

**Dependencies:** Sprints 8, 5, 4.

---

## Phase 3: Boss Support (Sprints 10–16)

### Sprint 10 — JSON Schema Formalization

**Objective:** Finalize the encounter JSON schema, add migration support, and write comprehensive validation tests.

**Inputs:**
- Schema drafts from Sprints 4, 7, 8, 9
- Example boss encounters (from community knowledge)

**Outputs:**
- Finalized schema version `1.0` with full type definitions
- Schema migration framework (for future versions)
- Validation test suite (valid + invalid examples)

**Files Created:**
- `src/main/resources/schemas/encounter_schema_v1.json` (final version)
- `src/main/resources/schemas/migration_v0_to_v1.json`
- `tests/java/com/coach/plugin/SchemaValidationTest.java`
- `tests/resources/test_packs/valid_pack.zip`
- `tests/resources/test_packs/invalid_missing_fields.zip`
- `tests/resources/test_packs/invalid_bad_trigger_type.zip`

**Files Modified:**
- `EncounterLoader.java` (use finalized schema)

**Plugin Changes:**
- Finalize schema: boss, phases, mechanics, triggers, callouts, conditions, recovery, visual, audio
- Add migration framework: load + apply JSON transformations for older schema versions
- Validation test suite: 10+ test cases covering edge cases

**JSON Schema Changes:** Finalize `1.0` — all fields documented, all types validated, all constraints enforced.

**Acceptance Criteria:**
- Valid packs pass validation
- Invalid packs fail with clear error messages
- Schema migration framework applies transformations correctly
- All test packs load or reject as expected
- Documentation for each schema field exists

**Manual Testing:**
1. Load each test pack, verify valid ones load + invalid ones are rejected
2. Check error messages are clear and actionable
3. Verify migration works on a v0.9 → v1.0 test pack

**Definition of Done:** Schema is finalized, validated, and documented. Migration framework works. Test suite passes.

**Estimated Time:** 90 minutes

**Dependencies:** Sprint 4, 7, 8.

---

### Sprint 11 — Boss Loader + Pack Management

**Objective:** Implement pack management: load/unload, reload, version tracking, and dependency resolution.

**Inputs:**
- Finalized schema from Sprint 10
- Test encounter packs

**Outputs:**
- Packs can be installed (placed in directory) and loaded at runtime
- Packs can be reloaded without restart
- Version conflicts are detected and reported
- Debug overlay shows loaded packs

**Files Created:**
- `src/main/java/com/coach/plugin/encounter/PackManager.java`
- `src/main/java/com/coach/plugin/encounter/PackMetadata.java`
- `src/main/java/com/coach/plugin/encounter/PackLoader.java`

**Files Modified:**
- `EncounterEngine.java` (use PackManager)
- `DebugOverlay.java` (show loaded packs)
- `CoachPlugin.java` (pack reload on config change)

**Plugin Changes:**
- `PackManager`: tracks loaded packs, their versions, dependencies, load status
- `PackLoader`: handles ZIP extraction, JSON loading, audio pre-loading
- `PackMetadata`: parsed from pack's `encounter.json` metadata section
- Reload mechanism: config change triggers re-scan of encounters directory

**JSON Schema Changes:** Add `dependencies` array to pack metadata (for packs that require other packs).

**Acceptance Criteria:**
- Packs load from encounters directory without restart
- Packs reload correctly on directory change (new pack added, old one removed)
- Version conflicts are detected (e.g., two packs for the same boss)
- Missing dependencies are reported in debug overlay
- Audio files pre-load into memory

**Manual Testing:**
1. Place a valid pack in encounters directory
2. Enable plugin, verify pack loads (debug overlay shows it)
3. Remove the pack while RuneLite is running, trigger reload
4. Verify pack unloads gracefully
5. Add invalid pack, verify it's skipped with error log

**Definition of Done:** Pack management works: load, unload, reload, version tracking. Dependencies + conflicts handled.

**Estimated Time:** 90 minutes

**Dependencies:** Sprint 10.

---

### Sprint 12 — Nex Implementation

**Objective:** Implement full encounter definition for Nex (including all phases, mechanics, triggers, and callouts).

**Inputs:**
- OSRS Wiki Nex page
- Community knowledge of Nex mechanics
- Finalized JSON schema
- Test encounter from earlier sprints

**Outputs:**
- Complete Nex encounter pack (`nex_1.0.0.zip`)
- All phases, mechanics, triggers, and callouts implemented
- TTD callouts verified (test against known fight patterns)

**Files Created:**
- `encounter-packs/nex.pack/encounter.json`
- `encounter-packs/nex.pack/audio/*.ogg` (7 callouts)

**Files Modified:** None (pure data pack).

**Plugin Changes:** None (uses existing engine).

**JSON Schema Changes:** None (schema is already finalized).

**Acceptance Criteria:**
- All 5 Nex phases (Smoke, Ranged, Mage, Melee, Zaros) are represented
- All special attacks (Shadow Smash, Contain, Ice Tomb, Blood Sacrifice, etc.) have triggers
- All prayer swap callouts have correct timing (2 ticks before impact)
- Phase transition callouts fire correctly
- Callouts are tick-accurate (verified against fight replay)
- Audio files are present for all visual callouts

**Manual Testing:**
1. Load Nex pack into encounters directory
2. Enter Nex fight (can use a private test world)
3. Verify all callouts fire at correct times (compare against known timing)
4. Fight until kill, verify no callouts missed or misfired
5. Record fight (manual log), replay events through test harness

**Definition of Done:** Nex encounter pack is complete, all callouts verified against actual fight timing. Pack passes validation.

**Estimated Time:** 240 minutes (includes fight research + verification)

**Dependencies:** Sprint 11, 9.

---

### Sprint 13 — Inferno Implementation

**Objective:** Implement full encounter definition for the Inferno (waves 1-69, all mechanics, prayer timings, and wave transitions).

**Inputs:**
- OSRS Wiki Inferno page
- Community wave-by-wave guides
- Finalized JSON schema

**Outputs:**
- Complete Inferno encounter pack (`inferno_1.0.0.zip`)
- All wave mechanics, prayer flick timings, and wave transitions
- "Wave starting" + "prayer flick" callouts

**Files Created:**
- `encounter-packs/inferno.pack/encounter.json`
- `encounter-packs/inferno.pack/audio/*.ogg` (12 callouts)

**Files Modified:** None.

**Plugin Changes:** None.

**JSON Schema Changes:** May need to add `wave` concept (array of phases or special phase type for sequential fights).

**Acceptance Criteria:**
- Waves 1-69 are covered (or waves are generated programmatically with rules)
- Each wave's mob layout triggers correct callouts
- Prayer flick timings for rangers, magers, and meleers are accurate
- Wave transition callouts ("Wave N starting") fire
- Tz-Kek spawn + healers are handled
- Tick accuracy verified for prayer flick callouts (±1 tick)

**Manual Testing:**
1. Load Inferno pack
2. Start wave 1, verify startup callout
3. Verify prayer flick callouts fire correctly for each mob type
4. Reach wave 30+, verify "Wave N starting" fires
5. Verify callouts stop correctly between waves

**Definition of Done:** Inferno encounter pack complete, prayer timings verified, wave transitions correct.

**Estimated Time:** 300 minutes (Inferno is complex — many wave patterns)

**Dependencies:** Sprint 12.

---

### Sprint 14 — Theatre of Blood (Soteboss) Implementation

**Objective:** Implement encounter for Soteboss (Theater of Blood first boss).

**Inputs:**
- OSRS Wiki Soteboss page
- Community knowledge of the fight
- Finalized JSON schema

**Outputs:**
- Complete Soteboss encounter pack (`tob_soteboss_1.0.0.zip`)
- All mechanics (crab rave, green orbs, red orbs, lightning, web bombs)

**Files Created:**
- `encounter-packs/tob_soteboss.pack/encounter.json`
- `encounter-packs/tob_soteboss.pack/audio/*.ogg` (8 callouts)

**Files Modified:** None.

**Plugin Changes:** None.

**JSON Schema Changes:** May need to add `arena` concept (region bounds for safe tile overlays).

**Acceptance Criteria:**
- All Soteboss mechanics are covered
- Safe tile overlays are accurate for green/red orbs
- Lightning callout timing is correct
- Web bomb timer callout fires
- Crab rave phase is detected
- Tick accuracy verified

**Manual Testing:**
1. Load Soteboss pack
2. Enter Theatre of Blood, start Soteboss fight
3. Verify all callouts fire at correct times
4. Verify safe tile overlays are correct
5. Verify phase transitions (normal → enrage → crab rave)

**Definition of Done:** Soteboss encounter pack complete, all mechanics + overlays verified.

**Estimated Time:** 150 minutes

**Dependencies:** Sprint 13.

---

### Sprint 15 — Tombs of Amascut Implementation

**Objective:** Implement encounter for Tombs of Amascut (key mechanics: Akkhan, Zebak, Kephri, Ba-Ba, Wardens).

**Inputs:**
- OSRS Wiki ToA page
- Community knowledge of invocation effects

**Outputs:**
- Complete ToA encounter pack (`toa_1.0.0.zip`)
- At least 3 boss implementations (Akkhan, Zebak, Kephri for MVP; Ba-Ba + Wardens optional)

**Files Created:**
- `encounter-packs/toa.pack/encounter.json`
- `encounter-packs/toa.pack/audio/*.ogg` (10+ callouts)

**Files Modified:** None.

**Plugin Changes:** None.

**JSON Schema Changes:** If needed, to support invocation-level mechanics.

**Acceptance Criteria:**
- At least 3 bosses fully implemented with all key mechanics
- Invocation-specific callouts are configurable (e.g., "Feeling Special" reduces healing)
- Tick-accurate callouts verified for each boss

**Manual Testing:**
1. Load ToA pack
2. Enter ToA raid, verify callouts for each boss
3. Adjust invocation settings, verify callouts adapt

**Definition of Done:** ToA encounter pack complete with 3+ bosses.

**Estimated Time:** 240 minutes

**Dependencies:** Sprint 14.

---

### Sprint 16 — Chambers of Xeric + Community Pack Template

**Objective:** Implement Chambers of Xolo'r (key rooms) and create a community pack template/example pack.

**Inputs:**
- OSRS Wiki CoX page
- Community knowledge of room mechanics

**Outputs:**
- CoX encounter pack (`cox_1.0.0.zip`) with 3-4 key rooms (Tekton, Olm, Vanguard, Vet'ion)
- Community pack template (minimal JSON + documentation for pack authors)

**Files Created:**
- `encounter-packs/cox.pack/encounter.json`
- `encounter-packs/cox.pack/audio/*.ogg`
- `encounter-packs/template.pack/encounter.json` (template)
- `docs/examples/ENCODING.md` (pack author guide)

**Files Modified:** None.

**Plugin Changes:** None.

**JSON Schema Changes:** None.

**Acceptance Criteria:**
- At least 3 CoX rooms fully implemented
- Template pack loads and shows placeholder callouts
- Pack author guide documents all JSON fields
- Template includes examples for each trigger type

**Manual Testing:**
1. Load CoX pack, test 3 rooms
2. Load template pack, verify it loads with no errors
3. Follow ENCODING.md to create a new pack, verify it works

**Definition of Done:** CoX pack + community template complete. Documentation published.

**Estimated Time:** 210 minutes

**Dependencies:** Sprint 15.

---

## Phase 4: User Experience (Sprints 17–22)

### Sprint 17 — Settings Overhaul + Profiles

**Objective:** Implement comprehensive config settings and profile management.

**Inputs:**
- Current debug-only config from earlier sprints
- MVP feature list (audio volume, visual opacity, callout toggles, pack selection)

**Outputs:**
- Full settings panel in RuneLite config
- Per-boss callout toggles
- Config profiles (save/load named configurations)

**Files Created:**
- `src/main/java/com/coach/plugin/config/CoachConfigV2.java`
- `src/main/java/com/coach/plugin/config/ProfileManager.java`
- `src/main/java/com/coach/plugin/config/ConfigProfile.java`

**Files Modified:**
- `CoachPlugin.java` (add config section registration)
- All engine classes (read new config fields)

**Plugin Changes:**
- Settings categories: Global, Audio, Visual, Boss Selection, Accessibility, Debug
- Per-boss toggles (checkbox per loaded boss)
- Per-callout-category toggles (critical, warning, info, transition)
- Profile manager: save current config as named profile, load profile

**JSON Schema Changes:** None.

**Acceptance Criteria:**
- All settings appear in RuneLite config panel
- Per-boss toggles work (disable a boss, callouts stop)
- Per-category toggles work (mute all informational callouts)
- Profiles save/load correctly
- Config changes apply without restart

**Manual Testing:**
1. Open plugin config, verify all sections present
2. Disable a boss, enter fight, verify no callouts
3. Save a profile, change settings, load profile, verify settings restored
4. Adjust audio volume, verify it takes effect immediately

**Definition of Done:** Full settings panel + profile management implemented and tested.

**Estimated Time:** 120 minutes

**Dependencies:** Sprint 9, 11.

---

### Sprint 18 — Overlay Improvements

**Objective:** Implement all overlay types: prayer indicators, countdowns, boss timeline, safe tiles, status indicators, mini HUD.

**Inputs:**
- Basic overlay from Sprint 9
- Community knowledge of visual cue preferences

**Outputs:**
- All 9 overlay types implemented
- Each overlay configurable (position, color, opacity, size)
- Visual styling matches "friendly coach" aesthetic

**Files Created:**
- `src/main/java/com/coach/plugin/overlay/PrayerIndicatorOverlay.java`
- `src/main/java/com/coach/plugin/overlay/CountdownOverlay.java`
- `src/main/java/com/coach/plugin/overlay/TimelineOverlay.java`
- `src/main/java/com/coach/plugin/overlay/SafeTileOverlay.java`
- `src/main/java/com/coach/plugin/overlay/StatusIndicatorOverlay.java`
- `src/main/java/com/coach/plugin/overlay/MiniHudOverlay.java`

**Files Modified:**
- `OverlayManager.java` (coordinate all overlay types)
- `CoachOverlay.java` (delegate rendering to sub-overlays)
- `CoachConfigV2.java` (add overlay config items)

**Plugin Changes:**
- Prayer indicators: large icon (prot melee/range/mage) + "USE" text, flashes
- Countdowns: large number (3→2→1) for pre-mechanic timing
- Boss timeline: horizontal progress bar showing phase progress
- Safe tiles: tile highlights (green = safe, red = danger) with pulsing animation
- Status indicators: player HP%, boss HP%, special attack ready
- Mini HUD: small panel showing current boss + phase + next mechanic
- Configurable styling: color, opacity, size, position per overlay type

**JSON Schema Changes:** Add `visual` config to callouts (type, color, size, position, animation).

**Acceptance Criteria:**
- All 9 overlay types render correctly
- Each overlay appears at the right position (configurable)
- Colors are colorblind-safe by default
- Opacity + size adjustments work in real-time
- Overlays don't overlap or obscure gameplay
- "Quiet hours" suppresses non-critical overlays after a callout

**Manual Testing:**
1. Enter a boss fight with full pack loaded
2. Verify each overlay type appears when expected
3. Adjust each overlay's config (position, color, opacity)
4. Verify settings apply immediately
5. Check colorblind mode (simulate with online tool)

**Definition of Done:** All overlay types implemented, configurable, and visually verified.

**Estimated Time:** 150 minutes

**Dependencies:** Sprint 9, 17.

---

### Sprint 19 — Audio Improvements

**Objective:** Implement audio priority system, interruption handling, and per-category volume control.

**Inputs:**
- Basic audio playback from Sprint 9
- Audio files from encounter packs

**Outputs:**
- Audio callouts with priority-based queuing
- Interruption of lower-priority callouts by higher-priority ones
- Per-category audio volume sliders

**Files Created:**
- `src/main/java/com/coach/plugin/audio/AudioPriorityResolver.java`
- `src/main/java/com/coach/plugin/audio/AudioInterruptManager.java`

**Files Modified:**
- `AudioEngine.java` (add priority + interruption)
- `CoachConfigV2.java` (add audio category volume sliders)

**Plugin Changes:**
- Audio priority: critical > warning > info > transition
- Interruption: higher-priority audio stops lower-priority audio currently playing
- Per-category volume: critical volume, warning volume, info volume, transition volume
- Audio timing offsets: configurable per-category timing adjustment
- Queue visualization: debug overlay shows queued audio callouts

**JSON Schema Changes:** Add `audioOffset` and `category` to callout definitions (for priority assignment).

**Acceptance Criteria:**
- High-priority callouts interrupt low-priority ones
- Same-category callouts queue (don't interrupt each other)
- Per-category volume sliders work independently
- Audio timing is tick-precise (verified with stopwatch)
- No audio overlap when multiple same-priority callouts fire

**Manual Testing:**
1. Create a encounter with multiple overlapping callout types
2. Fight and verify audio priority + interruption works
3. Adjust per-category volumes, verify independence
4. Use debug overlay to check audio queue ordering

**Definition of Done:** Audio priority, interruption, and volume controls work correctly.

**Estimated Time:** 90 minutes

**Dependencies:** Sprint 9.

---

### Sprint 20 — Accessibility Features

**Objective:** Implement accessibility options: audio-only mode, visual-only mode, high-contrast mode, and text scaling.

**Inputs:**
- Visual + audio systems from Sprints 18, 19
- Accessibility guidelines (WebAIM, WCAG contrast ratios)

**Outputs:**
- Audio-only mode (visual overlays hidden, audio enhanced)
- Visual-only mode (audio muted, all callouts visual)
- High-contrast color palette option
- Text scaling (small/medium/large)
- Screen reader compatibility (tooltips for all visual elements)

**Files Created:**
- `src/main/java/com/coach/plugin/accessibility/AccessibilityManager.java`
- `src/main/java/com/coach/plugin/accessibility/ColorPalette.java`
- `src/main/java/com/coach/plugin/accessibility/TextScaler.java`

**Files Modified:**
- `OverlayManager.java` (respect accessibility modes)
- `AudioEngine.java` (respect audio-only mode)
- `CoachConfigV2.java` (add accessibility config items)

**Plugin Changes:**
- Audio-only mode: hide all visual overlays, keep audio (for visually impaired players)
- Visual-only mode: mute all audio, show all callouts visually (for hearing-impaired players)
- High-contrast mode: switch to WCAG AA-compliant color palette
- Text scaling: 0.8x, 1.0x, 1.2x, 1.5x for all overlay text
- Toggle: "essential only" mode (only critical callouts, nothing else)

**JSON Schema Changes:** Add `accessibility_tags` to callouts (which senses they target: visual, auditory, both).

**Acceptance Criteria:**
- Audio-only mode hides all visuals, works for all bosses
- Visual-only mode mutes audio, shows all callouts
- High-contrast palette passes WCAG AA contrast checks
- Text scaling works for all overlay text
- "Essential only" mode shows only critical callouts

**Manual Testing:**
1. Enable audio-only mode, verify overlays disappear
2. Enable visual-only mode, verify audio stops
3. Enable high-contrast, verify colors meet contrast requirements
4. Change text scale, verify all text resizes
5. Enable essential-only, verify only critical callouts show

**Definition of Done:** All accessibility modes work. High-contrast meets WCAG AA. Text scaling functional.

**Estimated Time:** 105 minutes

**Dependencies:** Sprints 18, 19.

---

### Sprint 21 — Debug Tools

**Objective:** Build comprehensive debug tools: state inspector, trigger fire history, and event timeline.

**Inputs:**
- Logging system from Sprint 3
- Debug overlay from Sprint 3
- All engines from Phases 1-2

**Outputs:**
- Full-screen debug overlay with multiple tabs
- State inspector (live player/boss state)
- Trigger fire history (chronological log)
- Event timeline (graphical event sequence)
- Export debug log to file

**Files Created:**
- `src/main/java/com/coach/plugin/debug/DebugOverlayV2.java`
- `src/main/java/com/coach/plugin/debug/StateInspector.java`
- `src/main/java/com/coach/plugin/debug/TriggerHistory.java`
- `src/main/java/com/coach/plugin/debug/EventTimeline.java`
- `src/main/java/com/coach/plugin/debug/LogExporter.java`

**Files Modified:**
- `DebugOverlay.java` (deprecated, replaced by DebugOverlayV2)
- `CoachConfigV2.java` (add debug tools config)

**Plugin Changes:**
- Debug overlay has 4 tabs: Events, Triggers, State, Timeline
- State inspector: live JSON view of PlayerState + BossState
- Trigger history: chronological list of trigger fires with timestamps
- Event timeline: graphical timeline showing events + trigger fires + callouts per tick
- Log export: save debug data to `<runelite>/coach/debug_logs/` as JSON

**JSON Schema Changes:** None.

**Acceptance Criteria:**
- Debug overlay opens/closes via config toggle or hotkey
- State inspector shows live player/boss state
- Trigger history is chronological and filterable
- Event timeline shows visual sequence (events → triggers → callouts)
- Log export produces valid JSON file

**Manual Testing:**
1. Enable debug mode, open full debug overlay
2. Fight a boss, verify all 4 tabs populate
3. Check state inspector matches actual game state
4. Verify trigger history shows fires in correct order
5. Export log, verify JSON opens in a text editor

**Definition of Done:** Full debug overlay with 4 tabs + state inspector + log export works.

**Estimated Time:** 135 minutes

**Dependencies:** Sprint 3, 7, 9.

---

### Sprint 22 — Profile Management

**Objective:** Implement save/load profiles with export/import functionality.

**Inputs:**
- Config from Sprint 17
- Profile manager stub from Sprint 17

**Outputs:**
- Profiles can be saved (current config → named profile)
- Profiles can be loaded (named profile → current config)
- Profiles can be exported (.json file)
- Profiles can be imported (.json file)
- Default profiles: Learning Mode, Practice Mode, Performance Mode

**Files Created:**
- `src/main/java/com/coach/plugin/config/ProfileStorage.java`
- `src/main/java/com/coach/plugin/config/ProfileImporter.java`
- `src/main/java/com/coach/plugin/config/ProfileExporter.java`

**Files Modified:**
- `ProfileManager.java` (add export/import)
- `CoachPlugin.java` (add profile management commands to debug overlay)

**Plugin Changes:**
- Profile storage: RuneLite config stores named profiles as JSON strings
- Export: save profile to `<runelite>/coach/profiles/<name>.json`
- Import: load profile from file, validate, add to profiles list
- Default profiles:
  - **Learning Mode**: all callouts on, verbose, audio + visual
  - **Practice Mode**: only critical callouts, minimal visual
  - **Performance Mode**: no callouts (for experienced players, just logging)

**JSON Schema Changes:** None.

**Acceptance Criteria:**
- Save/load profiles works without restart
- Export produces valid JSON (can be inspected in text editor)
- Import validates JSON structure before loading
- Default profiles are available out of the box
- Profile deletion works

**Manual Testing:**
1. Save current config as "Test Profile"
2. Change settings, load "Test Profile", verify settings restored
3. Export "Test Profile", verify JSON file created
4. Import JSON file, verify profile loads
5. Verify default profiles exist

**Definition of Done:** Profile management fully functional with export/import + default profiles.

**Estimated Time:** 75 minutes

**Dependencies:** Sprint 17.

---

## Phase 5: AI Knowledge Pipeline (Sprints 23–28)

> **Note:** The AI Knowledge Pipeline is a separate Python tool (not part of the RuneLite plugin). It generates encounter JSON packs from the OSRS Wiki using an LLM, then validates them with an AI agent + human review.

### Sprint 23 — Wiki Parser + Raw Extraction

**Objective:** Build a tool that fetches OSRS Wiki pages and extracts structured text content (mechanic descriptions, phase descriptions, special attacks, timing info).

**Inputs:**
- OSRS Wiki URLs for target bosses (Nex, Inferno, etc.)
- Python environment with `requests`, `beautifulsoup4`

**Outputs:**
- `wiki_fetcher.py`: fetches + caches wiki page HTML
- `wiki_parser.py`: extracts mechanics, phases, timings from wiki text
- Structured output for each boss (mechanics list, phase descriptions, special attack names + descriptions)

**Files Created:**
- `knowledge-pipeline/pyproject.toml`
- `knowledge-pipeline/src/wiki_fetcher.py`
- `knowledge-pipeline/src/wiki_parser.py`
- `knowledge-pipeline/src/models.py` (extraction data models)
- `knowledge-pipeline/fixtures/sample_wiki_pages/nex.html` (for offline testing)

**Files Modified:** None.

**JSON Schema Changes:** None.

**Acceptance Criteria:**
- Wiki page downloads correctly (HTML)
- Parser extracts: boss name, phases, each mechanic with name + description
- Special attacks identified with timing info if present in wiki
- Output is structured JSON (not raw HTML)
- Parser handles wiki markup quirks (tables, infoboxes, navboxes)

**Manual Testing:**
1. Run `wiki_fetcher.py` on the Nex wiki page
2. Run `wiki_parser.py` on the downloaded HTML
3. Inspect output: verify phases + mechanics are extracted
4. Check that special attack names + descriptions are present

**Definition of Done:** Wiki parser extracts structured boss data from OSRS Wiki pages.

**Estimated Time:** 120 minutes

**Dependencies:** None (standalone tool).

---

### Sprint 24 — AI Schema Generation

**Objective:** Use an LLM (Claude/ChatGPT) to convert extracted wiki data into encounter JSON matching the schema, with structured prompting.

**Inputs:**
- Extracted wiki data from Sprint 23
- Finalized JSON schema from Sprint 10
- Prompt templates for LLM interaction

**Outputs:**
- `llm_prompter.py`: sends prompts to LLM API
- `json_generator.py`: converts LLM response to encounter JSON
- Draft encounter JSON for Nex (first boss)

**Files Created:**
- `knowledge-pipeline/src/llm_prompter.py`
- `knowledge-pipeline/src/json_generator.py`
- `knowledge-pipeline/src/prompts/system_prompt.txt` (LLM system prompt)
- `knowledge-pipeline/src/prompts/mechanic_extraction_prompt.txt`
- `knowledge-pipeline/fixtures/drafts/nex_draft.json` (AI-generated draft)

**Files Modified:**
- `models.py` (add LLM response models)

**JSON Schema Changes:** None (uses existing schema from Sprint 10).

**Acceptance Criteria:**
- LLM receives structured wiki data + schema spec
- LLM outputs valid JSON matching the schema structure
- JSON includes: boss metadata, phases, mechanics, triggers, callouts
- Draft JSON for Nex is created (even if incomplete)
- Prompt templates are reusable for other bosses

**Manual Testing:**
1. Run the full pipeline on Nex wiki page
2. Inspect `nex_draft.json` — verify structure matches schema
3. Check that triggers + callouts are present for each mechanic
4. Verify phase transitions are defined

**Definition of Done:** LLM generates draft encounter JSON from wiki data + schema prompt.

**Estimated Time:** 135 minutes

**Dependencies:** Sprint 23.

---

### Sprint 25 — Validation Tools + AI Validator Agent

**Objective:** Build automated validation for generated encounter JSON: schema checks, logic checks, and a semi-autonomous AI agent that can suggest fixes.

**Inputs:**
- Draft JSON from Sprint 24
- JSON schema from Sprint 10
- AI agent framework (like opencode coding agent)

**Outputs:**
- `schema_validator.py`: validates JSON against schema
- `logic_validator.py`: checks for logic issues (impossible conditions, unreachable phases)
- `ai_validator_agent.py`: autonomous agent that reviews JSON, suggests fixes, can edit
- Validation report (issues found + confidence scores)

**Files Created:**
- `knowledge-pipeline/src/schema_validator.py`
- `knowledge-pipeline/src/logic_validator.py`
- `knowledge-pipeline/src/ai_validator_agent.py`
- `knowledge-pipeline/src/validation_report.py`
- `knowledge-pipeline/tests/test_validators.py`

**Files Modified:**
- `pyproject.toml` (add jsonschema, pytest)

**JSON Schema Changes:** None (validates against existing schema).

**Acceptance Criteria:**
- Schema validator rejects invalid JSON with clear error messages
- Logic validator detects: missing entry conditions, unreachable phases, impossible triggers, missing audio files
- AI validator agent can review a draft JSON, identify issues, and propose fixes (via file edits)
- Validation report lists all issues + severity (critical, warning, info)
- Tests pass for both valid and invalid examples

**Manual Testing:**
1. Run validators on Nex draft JSON
2. Introduce a deliberate error (wrong trigger type), verify validator catches it
3. Run AI validator agent on the draft, verify it suggests fixes
4. Check validation report format

**Definition of Done:** Automated validators + AI validator agent work. Validation report generated.

**Estimated Time:** 150 minutes

**Dependencies:** Sprint 24.

---

### Sprint 26 — Human Review Interface

**Objective:** Build a simple web-based interface for human reviewers to inspect AI-generated drafts, approve/reject, and add notes.

**Inputs:**
- Draft JSON + validation report from Sprints 24, 25
- Flask/FastAPI for the web UI (simple, local-only)

**Outputs:**
- `human_review_interface/`: simple web app for review
- Review workflow: view → approve/reject → add notes → mark for audio generation
- Review status tracked (approved, needs_work, rejected)

**Files Created:**
- `knowledge-pipeline/src/human_review_interface/app.py`
- `knowledge-pipeline/src/human_review_interface/templates/review.html`
- `knowledge-pipeline/src/human_review_interface/templates/diff.html`
- `knowledge-pipeline/src/human_review_interface/static/style.css`

**Files Modified:**
- `ai_validator_agent.py` (add review status API)
- `validation_report.py` (add review metadata)

**JSON Schema Changes:** Add `review_status` and `review_notes` to pack metadata.

**Acceptance Criteria:**
- Web UI loads in browser (localhost:8080)
- Draft JSON displayed with collapsible sections per boss / phase / mechanic
- Diff view shows changes between AI draft and AI agent fixes
- Reviewer can approve, reject, or mark "needs work" with free-text notes
- Review status + notes saved to JSON metadata
- Only approved packs proceed to audio generation

**Manual Testing:**
1. Start the review interface: `python -m human_review_interface.app`
2. Open browser, view Nex draft
3. Approve a mechanic, add note "timing verified"
4. Reject another, add note "missing HP threshold"
5. Verify status is saved to JSON

**Definition of Done:** Human review interface works. Approval workflow enforced.

**Estimated Time:** 105 minutes

**Dependencies:** Sprint 25.

---

### Sprint 27 — Audio Generation + Pack Builder

**Objective:** Run TTS (Kokoro/Edge) on approved callout text, generate `.ogg` files, and assemble the final encounter pack ZIP.

**Inputs:**
- Approved JSON from Sprint 26
- TTS engine (Kokoro for offline, Edge TTS as fallback)
- Callout text from approved JSON

**Outputs:**
- `audio_generator.py`: generates `.ogg` files from callout text
- `pack_builder.py`: assembles JSON + audio into `.zip` pack
- Final distributable pack (`nex_1.0.0.zip`)

**Files Created:**
- `knowledge-pipeline/src/audio_generator.py`
- `knowledge-pipeline/src/pack_builder.py`
- `knowledge-pipeline/src/audio_config.yaml` (voice, speed, volume per category)
- `knowledge-pipeline/fixtures/dist/nex_1.0.0.zip` (final output)

**Files Modified:**
- `pyproject.toml` (add gTTS, edge-tts, kokoro, pydub, soundfile)

**JSON Schema Changes:** None.

**Acceptance Criteria:**
- TTS generates audio for each callout's text
- Audio files are `.ogg` format, < 500KB each
- Pack builder assembles JSON + audio into ZIP with correct structure
- Pack loads in RuneLite plugin without errors
- Audio files play correctly through the plugin's AudioEngine
- Voice settings are configurable per callout category

**Manual Testing:**
1. Run audio_generator on approved Nex JSON
2. Run pack_builder to create `nex_1.0.0.zip`
3. Place pack in RuneLite encounters directory
4. Enter Nex fight, verify audio callouts play correctly
5. Check file sizes + audio quality

**Definition of Done:** Audio files generated, pack ZIP built, loads in plugin, audio plays correctly.

**Estimated Time:** 120 minutes

**Dependencies:** Sprint 26, 9.

---

### Sprint 28 — Packaging System + End-to-End Pipeline

**Objective:** Create a CLI tool that runs the full Knowledge Pipeline end-to-end (Wiki → LLM → JSON → Validate → Human Review → Audio → Pack), with error handling and progress reporting.

**Inputs:**
- Wiki URL for target boss
- API keys for LLM + TTS
- Approval status from human review

**Outputs:**
- `pipeline.py`: CLI tool that runs the full pipeline
- Progress reporting (console + optional file log)
- Error handling (retries, fallback paths)
- Final pack output with version + changelog

**Files Created:**
- `knowledge-pipeline/src/pipeline.py`
- `knowledge-pipeline/src/changelog_generator.py`
- `knowledge-pipeline/fixtures/dist/changelog_nex_1.0.0.md`
- `knowledge-pipeline/README.md` (pipeline usage guide)

**Files Modified:**
- All component files (add pipeline entry points)

**JSON Schema Changes:** None.

**Acceptance Criteria:**
- `python pipeline.py --boss "Nex" --wiki-url "..."` runs the full pipeline
- Progress is reported at each stage (fetching, parsing, generating, validating, reviewing)
- Errors are handled gracefully (LLM timeout, TTS failure, audio file missing)
- Final pack is versioned (semver)
- Changelog is generated (diff from previous version, if updating)
- Pipeline can be run for all 6 supported bosses

**Manual Testing:**
1. Run full pipeline on Nex (use API keys)
2. Verify all stages complete, pack produced
3. Verify changelog is generated
4. Test error handling (block internet, simulate LLM failure)

**Definition of Done:** End-to-end pipeline runs successfully. Error handling + progress reporting work. Pack + changelog produced.

**Estimated Time:** 120 minutes

**Dependencies:** Sprints 23–27.

---

## Phase 6: Polish (Sprints 29–34)

### Sprint 29 — Performance Optimization

**Objective:** Profile the plugin under load and optimize tick processing, memory usage, and audio latency.

**Inputs:**
- All engines from Phases 1-3
- Performance requirements from Master Architecture (§12)

**Outputs:**
- Profiling data (per-tick breakdown)
- Optimized trigger evaluation (batching, short-circuit)
- Memory optimization (audio buffering, event log ring buffer)
- Audio latency under 50ms

**Files Created:**
- `src/main/java/com/coach/plugin/performance/Profiler.java`
- `src/main/java/com/coach/plugin/performance/MemoryMonitor.java`

**Files Modified:**
- `TriggerEngine.java` (add batching + short-circuit)
- `AudioEngine.java` (add pre-buffering)
- `EventLogger.java` (ring buffer instead of unbounded list)
- `DebugOverlay.java` (show profiling data)

**JSON Schema Changes:** None.

**Acceptance Criteria:**
- Tick processing time < 600ms (measured per component)
- Memory growth < 1 MB over 10-minute fight
- Audio latency < 50ms (measured with stopwatch)
- Debug overlay shows per-tick profiling data
- Trigger short-circuiting skips evaluation when preconditions not met

**Manual Testing:**
1. Enable profiling in debug overlay
2. Fight a complex boss (Nex or Inferno) for 5 minutes
3. Check debug overlay: per-component tick times
4. Check memory usage in RuneLite's performance monitor
5. Measure audio latency (start timer when visual fires, stop when audio starts)

**Definition of Done:** All performance targets met. Profiling tools integrated into debug overlay.

**Estimated Time:** 135 minutes

**Dependencies:** All engine sprints.

---

### Sprint 30 — Comprehensive Testing

**Objective:** Write full test suites: unit tests, integration tests, boss simulations, and replay tests.

**Inputs:**
- All plugin code from Phases 1-3
- Boss simulation event logs from earlier sprints

**Outputs:**
- Unit tests for every engine component (>80% coverage)
- Integration tests (event → callout flow)
- Boss simulation tests (replayed events, verified callouts)
- Replay tests (recorded fight data)

**Files Created:**
- `tests/java/com/coach/plugin/unit/TriggerEngineTest.java`
- `tests/java/com/coach/plugin/unit/EncounterEngineTest.java`
- `tests/java/com/coach/plugin/unit/CoachingEngineTest.java`
- `tests/java/com/coach/plugin/integration/EndToEndTest.java`
- `tests/java/com/coach/plugin/simulation/NexSimulationTest.java`
- `tests/java/com/coach/plugin/simulation/InfernoSimulationTest.java`
- `tests/resources/simulations/nex_fight_log.json`
- `tests/resources/simulations/inferno_waves_1_20.json`

**Files Modified:**
- `build.gradle` (add test dependencies + test task config)

**JSON Schema Changes:** None.

**Acceptance Criteria:**
- Unit test coverage ≥ 80% for all package classes
- All unit tests pass (0 failures)
- Integration tests cover full pipeline (event → trigger → encounter → callout → audio + visual)
- Boss simulation tests: all callouts fire on correct ticks (verified against known fight data)
- Replay tests: event logs replay correctly, callouts match expected

**Manual Testing:**
1. Run `gradle test`
2. Verify all tests pass
3. Check coverage report (JaCoCo)
4. Run a simulation test manually, inspect debug log for correctness

**Definition of Done:** Full test suite passes. Coverage ≥ 80%. Boss simulations verified.

**Estimated Time:** 210 minutes

**Dependencies:** All engine sprints.

---

### Sprint 31 — Documentation

**Objective:** Write complete documentation: pack author guide, plugin user guide, API reference, and developer setup guide.

**Inputs:**
- All code from Phases 1-3
- Master Architecture document
- Sprint Roadmap

**Outputs:**
- Pack author guide (JSON schema, trigger types, callout config)
- Plugin user guide (installation, configuration, troubleshooting)
- Developer setup guide (build from source, run tests)
- API reference (class hierarchy, key methods)

**Files Created:**
- `docs/examples/ENCODING.md` (pack author guide)
- `docs/USER_GUIDE.md` (player guide)
- `docs/DEVELOPER_SETUP.md` (setup guide)
- `docs/API_REFERENCE.md` (class reference)
- `README.md` (project overview + links)

**Files Modified:** None (new doc files).

**JSON Schema Changes:** None.

**Acceptance Criteria:**
- Pack author can create a new pack following the guide
- Player can install + configure the plugin following the guide
- Developer can build + test from source following the guide
- API reference documents all public classes and key methods
- No broken links in any doc

**Manual Testing:**
1. Have a new pack author follow ENCODING.md to create a test pack
2. Have a new user follow USER_GUIDE.md to install + configure
3. Have a new dev follow DEVELOPER_SETUP.md to build + test

**Definition of Done:** All documentation written, verified by walkthrough, no broken links.

**Estimated Time:** 165 minutes

**Dependencies:** All sprints.

---

### Sprint 32 — Release Preparation

**Objective:** Prepare the first release build: version bump, changelog, JAR signing, RuneLite hub submission prep.

**Inputs:**
- All code + tests from earlier sprints
- Documentation from Sprint 31

**Outputs:**
- Release-ready JAR (signed, optimized)
- Changelog (v1.0.0)
- RuneLite plugin hub submission package
- Release notes

**Files Created:**
- `CHANGELOG.md` (v1.0.0 release notes)
- `release/run.sh` (release build script)

**Files Modified:**
- `build.gradle` (release config, ProGuard, signing)
- `runelite-plugin.properties` (version bump to 1.0.0)

**JSON Schema Changes:** Version bump to schema `1.0` (final).

**Acceptance Criteria:**
- `gradle buildRelease` produces optimized, signed JAR
- ProGuard removes unused code, JAR is < 5 MB
- Changelog documents all features + known issues
- Plugin hub submission package is complete (JAR + metadata)
- v1.0.0 tags in git

**Manual Testing:**
1. Run release build
2. Verify JAR size + signature
3. Install in clean RuneLite, verify all 6 bosses work
4. Verify changelog is accurate

**Definition of Done:** Release build ready. Changelog complete. JAR signed + optimized.

**Estimated Time:** 105 minutes

**Dependencies:** Sprints 30, 31.

---

### Sprint 33 — Community Beta

**Objective:** Release beta to community testers, collect feedback, fix critical issues.

**Inputs:**
- Release build from Sprint 32
- Community testers (from RuneLite Discord, OSRS community)
- Feedback collection mechanism

**Outputs:**
- Beta release published to RuneLite plugin hub (beta channel)
- Issue tracking (GitHub Issues)
- Feedback summary + action items

**Files Created:**
- `docs/BETA_GUIDE.md` (how to join beta, report issues)
- `tests/resources/beta_feedback_summaries/` (issue categorization)

**Files Modified:**
- `CHANGELOG.md` (beta notes)
- Any bug fix PRs from Sprint 32

**JSON Schema Changes:** None.

**Acceptance Criteria:**
- Beta published to RuneLite plugin hub beta channel
- 10+ community testers install and use the plugin
- At least 3 boss packs verified in real fights
- All reported critical issues fixed
- Feedback summary documents common pain points + requested features

**Manual Testing:**
1. Publish beta to plugin hub
2. Recruit testers from OSRS community
3. Collect feedback via GitHub Issues + Discord
4. Verify fixes for all reported critical issues

**Definition of Done:** Beta released. 10+ testers. Critical issues fixed. Feedback documented.

**Estimated Time:** 180 minutes (over 2-week beta period)

**Dependencies:** Sprint 32.

---

### Sprint 34 — Version 1.0

**Objective:** Final v1.0 release — all fixes from beta applied, final performance check, publish stable release.

**Inputs:**
- Beta feedback from Sprint 33
- Release build process from Sprint 32
- All test suites from Sprint 30

**Outputs:**
- v1.0.0 stable release published to RuneLite plugin hub
- Updated documentation (v1.0 versions)
- 6 fully supported boss packs (Nex, Inferno, Soteboss, ToA, CoX, + 1 bonus)
- Community announcement

**Files Created:**
- `docs/v1_RELEASE_NOTES.md` (final release notes)

**Files Modified:**
- All bug fixes from beta
- `CHANGELOG.md` (final v1.0.0 entry)

**JSON Schema Changes:** Final `1.0` schema (no changes from Sprint 32).

**Acceptance Criteria:**
- All beta issues resolved
- 6 boss packs verified in real fights
- Performance meets all targets (tick < 600ms, memory < 10MB, audio < 50ms)
- Documentation updated to v1.0
- v1.0.0 published to plugin hub stable channel

**Manual Testing:**
1. Final fight tests for all 6 bosses
2. Performance profile under worst-case scenario (Inferno with max mechanics)
3. Verify all documentation matches v1.0 code
4. Publish stable release

**Definition of Done:** v1.0.0 stable released. 6 boss packs working. Documentation current. All performance targets met.

**Estimated Time:** 120 minutes

**Dependencies:** Sprint 33.

---

*End of Sprint Roadmap — 34 sprints total across 6 phases.*
