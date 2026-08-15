# Project Coach — Master Architecture

> **Version:** 1.0
> **Status:** Draft — Sprint 0 (Pre-MVP)
> **Audience:** Developers, AI coding agents, project maintainers
> **Language:** Java 11 (RuneLite plugin standard)
> **Platform:** RuneLite / Old School RuneScape

This is the single source of truth for what Project Coach is and how it is structured. Every future document — the Implementation Guide, the Sprint Roadmap, API references — derives from and references this document. Read this first.

---

## Table of Contents

1. [Vision](#1-vision)  
2. [Design Philosophy](#2-design-philosophy)  
3. [Project Rules](#3-project-rules)  
4. [Goals](#4-goals)  
5. [Non-Goals](#5-non-goals)  
6. [MVP Definition](#6-mvp-definition)  
7. [High-Level Architecture](#7-high-level-architecture)  
8. [Component Deep-Dives](#8-component-deep-dives)  
   - 8.1 [RuneLite Plugin](#81-runelite-plugin)  
   - 8.2 [Coaching Engine](#82-coaching-engine)  
   - 8.3 [Encounter Engine](#83-encounter-engine)  
   - 8.4 [Trigger Engine](#84-trigger-engine)  
   - 8.5 [Overlay System](#85-overlay-system)  
   - 8.6 [Audio Engine](#86-audio-engine)  
   - 8.7 [Knowledge Compiler / AI Knowledge Generator](#87-knowledge-compiler--ai-knowledge-generator)  
   - 8.8 [Configuration Manager](#88-configuration-manager)  
   - 8.9 [Logging System](#89-logging-system)  
9. [Data Models](#9-data-models)  
10. [JSON Schema Specification](#10-json-schema-specification)  
11. [File Structure](#11-file-structure)  
12. [Performance](#12-performance)  
13. [Security](#13-security)  
14. [Testing Strategy](#14-testing-strategy)  
15. [Future Expansion](#15-future-expansion)  

---

## 1. Vision

> **North Star:** *Project Coach is a real-time game coaching assistant that acts like a patient, knowledgeable friend sitting next to you on Discord — giving you visual and audio callouts for boss mechanics so you can focus on executing, not memorizing.*

Project Coach is a **RuneLite plugin** that provides real-time, tick-accurate coaching for Old School RuneScape boss encounters. It delivers callouts — both visual overlays and audio cues — exactly when needed, modeled on the experience of having a friend guide you through a fight over Discord.

Unlike automation tools or "helper" plugins that press keys for you, Project Coach **never simulates input**. Every callout is advisory. The player retains full agency. The coach *suggests* when to prayer-swap, when to move, when to eat, or when to anticipate a special attack — the player acts on that information themselves.

The coaching knowledge is **data-driven**: encounter definitions are JSON files (community-maintained "knowledge packs") that describe each boss's phases, mechanics, triggers, and callouts. An optional AI Knowledge Pipeline can generate these JSON files from the OSRS Wiki, with human verification as the final gatekeeper.

### The Coaching Pipeline

```
Game State (RuneLite client events)
        |
        v
Event Bus (Tick, Animation, Projectile, Graphic, NPC...)
        |
        v
Trigger Engine — "Did the boss start animation X?"
        |
        v
Encounter Engine — "What phase are we in? What mechanic just fired?"
        |
        v
Coaching Engine — "What should we call out? When? How loudly?"
        |                                    |
        +---> Visual Callout —> Overlay System
        +---> Audio Callout   —> Audio Engine
```

---

## 2. Design Philosophy

| Principle | Description |
|-----------|-------------|
| **Coach, don't automate** | Project Coach never presses keys, moves the camera, or interferes with player input. All callouts are advisory. This is both a design constraint and a RuneLite ToS compliance requirement. |
| **Human remains in control** | The player decides whether to act on every callout. The coach can be silenced or disabled per-mechanic, per-boss, or globally at any time. |
| **Accessibility first** | Every callout has a visual *and* audio component by default. Colorblind-safe palettes, scalable text, independent audio volume per callout category. Players with hearing or vision impairments are not left behind. |
| **Modular encounter definitions** | Boss knowledge lives in JSON packs, not compiled code. New bosses can be added without recompiling the plugin. Community members can publish their own packs. |
| **Tick-exact determinism** | Callouts fire on the correct game tick. Timing is computed from tick-aligned events (game ticks are 600ms). No "fuzzy" timing that drifts. |
| **Extensible architecture** | The Trigger, Encounter, and Coaching engines are generic enough to support new games beyond OSRS (future work). Game-specific logic lives in the RuneLite Plugin layer. |
| **Data-driven, not code-driven** | The plugin is a general-purpose coaching runtime. Boss-specific logic lives in JSON data files, keeping the plugin JAR small and approval-friendly. |
| **Community-powered knowledge** | Encounter packs are maintained by the community. The plugin loads packs from a local directory. AI-generated drafts are validated before community publication. |

---

## 3. Project Rules

These are the "constitution" of Project Coach. They must be upheld in every decision.

1. **The plugin never simulates input.** No key presses, no mouse movement, no camera control. If RuneLite's anti-cheat flags it, we remove it.
2. **All callouts are advisory.** The coach *suggests*; the player *decides*.
3. **Encounter definitions are data-only.** No game logic in JSON packs — only triggers, conditions, callouts, and timing. All logic lives in the plugin runtime.
4. **All encounter packs must pass schema validation before loading.** Invalid packs are rejected at startup with a clear error.
5. **Every callout must have a visual and audio option.** A player should be able to use the coach with audio only or visual only if they choose.
6. **Tick timing is authoritative.** Callouts that fire on the wrong tick are bugs. Tick accuracy is tested against replayed fight data.
7. **Every feature must be testable independently.** No feature ships without unit or integration tests.
8. **AI-generated content must be human-verified.** The AI Knowledge Pipeline produces drafts. An AI agent validator (like an autonomous coding assistant) validates schema conformance and logic. A human reviewer does the final check before publication.
9. **The plugin loads packs from a user-writable directory.** No encounter JSON is bundled in the JAR (except the AI pipeline's test fixtures).
10. **No network calls in the MVP plugin.** The plugin is fully offline. Audio assets, JSON packs, and configurations all live on the local filesystem.
11. **All audio assets are pre-recorded TTS output.** We do not generate audio at runtime. Audio files are produced by running TTS (Kokoro or Edge TTS) on callout text during pack creation, then packaged as `.ogg` files in the pack.

---

## 4. Goals

### Must-Have (MVP)

- RuneLite plugin that loads community encounter packs from a local directory
- Real-time tick-aligned visual overlays for boss mechanics (safe tiles, incoming attacks, prayer swaps)
- Real-time audio callouts (pre-recorded TTS `.ogg` files) with tick-precise timing
- Support for at least 3 boss encounters (Nex, Inferno, and one Theater of Blood boss) with full trigger/mechanic coverage
- An AI Knowledge Pipeline that reads the OSRS Wiki, generates draft encounter JSON, and validates it
- Schema validation for encounter packs (reject invalid packs at load time)
- Per-boss, per-category callout toggling (player can disable individual callouts)
- Configurable audio volume and visual opacity
- Local-only operation (no network calls)
- Debug overlay showing event history (for development and player verification)

### Should-Have (Post-MVP)

- Full boss roster coverage (all major PvM bosses)
- Community pack repository (sharing platform, not bundled)
- Replay recording and analysis ("Review Mode" — see Future Expansion)
- Voice pack variants (different AI voices per callout category)
- Colorblind mode presets
- Keyboard shortcut reference overlay

### Could-Have (Future)

- Multi-game support (extension to other RuneLite-based servers or other game clients)
- Cloud sync for encounter packs (opt-in)
- In-game encounter editor (visual JSON builder)
- Adaptive coaching (adjust callout verbosity based on player performance)
- Machine learning research layer (predict player mistakes, adjust timing)

---

## 5. Non-Goals

- **Not an automation tool.** No input simulation. No "auto-pray" or "auto-move" features.
- **Not a combat statistics tracker.** We call out mechanics; we don't track DPS or efficiency metrics.
- **Not a replacement for learning.** The coach is a training wheel. Long-term, players should internalize the timings.
- **No hosted services.** The plugin is fully offline. The AI pipeline runs locally by developers, not by end users.
- **No real-time collaboration.** This is a single-player local tool.
- **No mobile version.** Desktop RuneLite only.
- **No built-in pack browser/downloader.** Users manually place packs in a folder. A community repository is future work.

---

## 6. MVP Definition

The MVP is the minimal set of features that demonstrates the core value proposition: **real-time, tick-precise visual and audio coaching for boss fights, with data-driven encounter definitions.**

### In Scope

| Component | Status |
|-----------|--------|
| RuneLite plugin skeleton (Gradle + manifest) | Core |
| Event bus (Tick, Animation, Projectile, Graphic, NPC, HP, Prayer, Player state) | Core |
| Trigger engine (all trigger types) | Core |
| Encounter engine (JSON loader, phase machine, state management) | Core |
| Coaching engine (priority scheduling, callout queueing, cooldowns) | Core |
| Overlay system (visual callouts, debug overlay) | Core |
| Audio engine (pre-recorded .ogg playback, priority queue) | Core |
| Configuration system (RuneLite config items) | Core |
| JSON schema for encounter definitions | Core |
| Schema validation at load time | Core |
| AI Knowledge Pipeline (Wiki parser → LLM → JSON draft → AI agent validator → human review) | Core |
| Support for 3 boss encounters (Nex, Inferno, ToB boss) | Core |
| Debug tools (event log, trigger fire history, state inspector) | Core |
| Logging system | Core |

### Out of Scope (MVP)

- Community pack sharing platform
- Replay/recording mode
- In-game encounter editor
- Colorblind mode presets (basic color choices included; advanced presets are post-MVP)
- Multi-game support
- Voice pack variants beyond default

### Success Criteria

The MVP is complete when:

1. A user installs the plugin via RuneLite's plugin hub and it loads without errors.
2. The user places an encounter pack (.zip) in the designated directory and the plugin loads it (validating schema).
3. During a Nex fight, the plugin fires:
   - Visual overlay: "Pray Melee" (2 ticks before impact)
   - Audio callout: "Red phase incoming" (1 tick before phase transition)
4. The audio fires within ±1 tick of the correct moment.
5. The user can toggle individual callouts off per boss.
6. The debug overlay shows the last 50 game events and which triggers fired.
7. The AI Knowledge Pipeline can generate a valid encounter JSON from a sample boss wiki page, validated by both the AI agent validator and human review.
8. The plugin runs without causing RuneLite performance issues (CPU < 5% during active fights).

---

## 7. High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        RuneLite Client (OSRS)                              │
│                                                                             │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐              │
│  │ Game State      │  │ Event System    │  │ Overlay Manager │              │
│  │ (NPCs, HP,      │  │ (Tick, Anim,    │  │ (UI rendering)  │              │
│  │  Player, etc.)  │  │  Proj, Graphic) │  │                 │              │
│  └────────┬────────┘  └────────┬────────┘  └────────┬────────┘              │
│           │                     │                    │                      │
└───────────┼─────────────────────┼────────────────────┼────────────────────────┘
            │                     │                    │
            ▼                     ▼                    ▼
┌─────────────────────────────────────────────────────────────────┐
│              Project Coach Plugin (Java/JVM)                     │
│                                                                  │
│  ┌─────────────────────┐                                        │
│  │  Plugin Lifecycle    │  (startUp / shutDown)                  │
│  │  Event Subscriptions │  (@Subscribe annotations)                │
│  └──────────┬──────────┘                                        │
│             │                                                   │
│             ▼                                                   │
│  ┌─────────────────────┐                                        │
│  │  Event Bus          │  (dispatches game events to listeners)   │
│  └──────────┬──────────┘                                        │
│             │                                                   │
│             ▼                                                   │
│  ┌─────────────────────┐  ┌─────────────────────────────────┐   │
│  │  Trigger Engine     │──▶  Encounter Engine                 │   │
│  │  (fire detection)   │  │  (phase machine, state)          │   │
│  └─────────────────────┘  └──────────┬───────────────────────┘   │
│                                      │                           │
│                                      ▼                           │
│  ┌─────────────────────┐  ┌─────────────────────────────────┐   │
│  │  Configuration       │  │  Coaching Engine                │   │
│  │  Manager             │  │  (priority, scheduling, queue)  │   │
│  └─────────────────────┘  └──────────┬───────────────────────┘   │
│                                      │                           │
│                            ┌──────────┴───────────┐              │
│                            │                      │              │
│                            ▼                      ▼              │
│                   ┌────────────────────┐  ┌────────────────────┐ │
│                   │  Overlay System    │  │  Audio Engine      │ │
│                   │  (visual callouts) │  │  (TTS .ogg files)  │ │
│                   └────────────────────┘  └────────────────────┘ │
│                                                                 │
│  ┌─────────────────────┐  ┌─────────────────────────────────┐   │
│  │  File System        │  │  Logging System                 │   │
│  │  (encounter packs,     │  (debug log, event history)         │   │
│  │   audio assets,        │                                   │   │
│  │   configs)           │                                   │   │
│  └─────────────────────┘  └─────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
            │                     │
            ▼                     ▼
┌───────────────────────────────┐  ┌───────────────────────────────┐
│  AI Knowledge Pipeline         │  │  Encounter Pack Repository     │
│  (separate tool, not in plugin)  │  (community-maintained ZIPs)   │
│  Wiki → LLM → JSON → Validator   │  (placed in plugin directory)   │
└─────────────────────────────────┘  └───────────────────────────────┘
```

### Data Flow Summary

1. **Event**: RuneLite client fires game events (Tick, AnimationChanged, etc.) → Plugin's `@Subscribe` handlers → Event Bus
2. **Detect**: Trigger Engine receives events, evaluates each loaded encounter's trigger conditions → fires matching triggers
3. **Decide**: Encounter Engine updates phase/state based on triggered conditions → Coaching Engine receives "mechanic active" signals
4. **Callout**: Coaching Engine prioritizes callouts by importance and timing, schedules visual + audio delivery
5. **Deliver**: Overlay System renders visual callouts; Audio Engine plays pre-recorded `.ogg` callouts at the correct tick
6. **Validate**: (Offline) AI Knowledge Pipeline reads OSRS Wiki, LLM generates encounter JSON, AI agent validator checks schema + logic, human reviewer approves

---

## 8. Component Deep-Dives

### 8.1 RuneLite Plugin

**Purpose**: The entry point and integration layer between OSRS game events and the coaching runtime.

**Responsibilities**:
- Plugin lifecycle management (`startUp`, `shutDown`)
- RuneLite event subscriptions (Tick, AnimationChanged, ProjectileSpawned, GraphicChanged, NpcSpawned, NpcDespawned, etc.)
- Game state bridging (extract NPC IDs, player HP, prayer state, inventory, tile positions, animation IDs, projectile IDs, graphic IDs)
- Overlay provider (`@Overlay` methods for visual callouts)
- Audio provider (play .ogg files via RuneLite's audio API or `javax.sound.sampled`)
- Config item registration (toggle bosses, adjust volumes, set pack directory)

**RuneLite Event Subscriptions**:

| Event | What It Detects | Used For |
|-------|----------------|----------|
| `Tick` | 600ms game loop pulse | Timeline tracking, tick countdown overlays |
| `AnimationChanged` | NPC/player animation ID | Boss special attacks, player actions |
| `ProjectileSpawned` | Projectile ID, source, destination | Ranged/magic attacks, orb attacks |
| `GraphicChanged` | Graphic ID on NPC/player | Special effects, damage indicators |
| `NpcSpawned` | NPC ID, position | Boss spawn, minion spawns |
| `NpcDespawned` | NPC removal | Phase transitions, death |
| `NpcHPChanged` | NPC current/max HP | Phase thresholds, enrage detection |
| `PlayerHPChanged` | Player HP value | "Eat" callouts |
| `PrayerStateChanged` | Player prayer status | Confirm player swapped |
| `VarbitChanged` | Game state variables | Phase variables, special mechanics |

**Design Notes**:
- The plugin layer should contain **zero encounter-specific logic**. All boss knowledge lives in JSON packs.
- Event handlers should be lightweight — dispatch to the Event Bus quickly, never block the event thread.
- The plugin reads encounter packs from `<runelite dir>/coach/encounters/` (a directory the user creates). Each pack is a `.zip` containing JSON + audio files.
- On startup, the plugin scans the directory, validates each pack's schema, and loads valid ones into the Encounter Engine.

**Internal Components**:

| Component | Class | Description |
|-----------|-------|-------------|
| Plugin Entry | `CoachPlugin` | Main class, extends `Plugin`, handles lifecycle |
| Event Dispatcher | `EventBus` | Routes game events to Trigger + Coaching engines |
| GameStateAdapter | `GameStateBridge` | Translates RuneLite objects into internal state models |
| OverlayProvider | `CoachOverlay` | Renders visual callouts via `Overlay.render` |
| AudioProvider | `CoachAudio` | Plays audio callouts |
| ConfigProvider | `CoachConfig` | RuneLite config items |

### 8.2 Coaching Engine

**Purpose**: The decision-making core. Receives trigger signals and "current encounter state," decides what to call out, when, and how.

**Responsibilities**:
- **Decision making**: Given a set of possible callouts, which one(s) to fire now
- **Priority resolution**: Critical (e.g., "Pray Melee" 2 ticks before impact) vs. informational ("30s into phase")
- **Callout scheduling**: Time callouts to fire at the right tick relative to game events
- **Cooldown handling**: Don't repeat the same callout within X ticks
- **Queue management**: If multiple callouts are queued, play them in priority order without overlap
- **State management**: Track current encounter state (phase, tick count, recent events)
- **Prediction**: Anticipate upcoming mechanics (e.g., "Next: special attack in 3 ticks")

**Inputs**:
- Trigger fire signals from the Trigger Engine
- Encounter state from the Encounter Engine (current phase, mechanic history, tick timers)
- Player state from the GameStateBridge (HP, prayer, position)
- Configuration (what callouts are enabled, volume levels, timing offsets)

**Outputs**:
- Visual callout requests → Overlay System
- Audio callout requests → Audio Engine

**Internal Components**:

| Component | Class | Description |
|-----------|-------|-------------|
| CoachBrain | `CoachingEngine` | Main entry point, receives trigger events, decides callouts |
| PriorityResolver | `PriorityResolver` | Assigns priority to callouts based on urgency + type |
| CalloutScheduler | `CalloutScheduler` | Schedules callouts at tick-precise offsets |
| CooldownManager | `CooldownManager` | Tracks recently fired callouts, suppresses duplicates |
| CalloutQueue | `CalloutQueue` | FIFO queue with priority reordering for pending callouts |
| StateManager | `CoachStateManager` | Holds current encounter + player state for decision-making |
| PredictionEngine | `PredictionEngine` | Looks ahead in the encounter timeline to pre-callout |

**Design Notes**:
- Callout timing is always expressed in **game ticks** (not milliseconds). Tick = 600ms. Callouts scheduled "2 ticks before" fire 1200ms before the event's estimated tick.
- The queue supports interruption: a high-priority callout ("Pray Melee NOW") can preempt a lower-priority queued callout ("Phase transitioning in 3... 2... 1").
- State is passed by value (immutable snapshots) to avoid race conditions in the tick handler.

### 8.3 Encounter Engine

**Purpose**: Manages the lifecycle of a loaded boss encounter — phases, mechanics, state transitions, and recovery logic.

**Responsibilities**:
- Parse encounter JSON definitions into runtime objects
- Manage phase state machine (phase transitions, entry/exit conditions)
- Track active mechanics (which mechanics are currently "live" and need monitoring)
- Handle failure / recovery logic (player died, boss reset, etc.)
- Provide state queries for the Coaching Engine (current phase, active mechanics, timers)

**Data Model**:

```
Encounter (boss definition)
├── metadata (name, version, game_version, author)
├── phases (ordered list)
│   └── Phase
│       ├── id
│       ├── entry_conditions (what starts this phase)
│       ├── mechanics (list of Mechanic objects)
│       ├── exit_conditions (what ends this phase)
│       └── transitions (to other phases)
├── mechanics (optional: shared across phases)
│   └── Mechanic
│       ├── id
│       ├── triggers (list of Trigger objects)
│       ├── callouts (list of Callout objects)
│       ├── conditions (optional: extra conditions to fire)
│       ├── cooldown (ticks before this mechanic can fire again)
│       └── interruptible (can be overridden by higher-priority mechanic)
└── recovery (optional: rules for handling player death / reset)
```

**Internal Components**:

| Component | Class | Description |
|-----------|-------|-------------|
| EncounterLoader | `EncounterLoader` | Loads + validates JSON packs into runtime objects |
| PhaseMachine | `PhaseMachine` | Manages phase state (current phase, transitions, entry/exit) |
| MechanicManager | `MechanicManager` | Tracks active mechanics, their state, and timers |
| ConditionEvaluator | `ConditionEvaluator` | Evaluates JSON-defined conditions (NPC HP < 50%, tick % 4 == 0, etc.) |
| RecoveryHandler | `RecoveryHandler` | Handles edge cases (player death, boss reset, phase skip) |

**Design Notes**:
- Encounter definitions should be **pure data** — no code execution in JSON. Conditions are evaluated by the ConditionEvaluator, which supports a fixed set of condition types (HP thresholds, tick counters, player state, NPC state).
- Phases can be sequential (Phase 2 starts when Phase 1 ends) or conditional (Phase 2 starts when a special attack fires).
- Recovery logic handles edge cases like "boss died mid-phase" or "player respawned" — the encounter resets to a known state.

### 8.4 Trigger Engine

**Purpose**: The detection layer. Listens to game events and determines "something happened" that maps to an encounter mechanic.

**Responsibilities**:
- Subscribe to game events from the Event Bus
- Evaluate trigger conditions for all loaded encounters
- Fire trigger events when conditions are met
- Support composite triggers (AND/OR combinations)
- Batch trigger evaluation per tick (all events in a tick are processed together)

**Trigger Types** (all from idea.md):

| Trigger Type | Game Event | Detection Logic |
|-------------|-----------|-----------------|
| **Animation Trigger** | `AnimationChanged` | NPC or player animation ID matches configured value |
| **Projectile Trigger** | `ProjectileSpawned` | Projectile ID + source NPC matches configured values |
| **Graphic Object Trigger** | `GraphicChanged` | Graphic ID on NPC/player matches configured value |
| **NPC Spawn Trigger** | `NpcSpawned` | NPC ID matches configured value |
| **NPC Despawn Trigger** | `NpcDespawned` | NPC ID matches configured value |
| **HP Trigger** | `NpcHPChanged` / `PlayerHPChanged` | HP value crosses configured threshold |
| **Tick Timer Trigger** | `Tick` | Game tick counter matches pattern (e.g., `tick % 4 == 0`) |
| **Player State Trigger** | Multiple | Player animation, HP, prayer, position, inventory |
| **Location Trigger** | `Tick` + player position | Player is in a configured tile region |
| **Custom Rule Trigger** | Various | Arbitrary condition evaluated by ConditionEvaluator |
| **Composite Trigger** | Any | AND/OR combination of multiple triggers |

**Internal Components**:

| Component | Description |
|-----------|-------------|
| `TriggerEngine` | Main entry point, receives events, dispatches to trigger evaluators |
| `TriggerEvaluator` (interface) | Base class for each trigger type |
| `AnimationTriggerEvaluator` | Handles Animation Triggers |
| `ProjectileTriggerEvaluator` | Handles Projectile Triggers |
| `GraphicTriggerEvaluator` | Handles Graphic Object Triggers |
| `NpcSpawnTriggerEvaluator` | Handles NPC Spawn/Despawn Triggers |
| `HpTriggerEvaluator` | Handles HP Triggers |
| `TickTimerTriggerEvaluator` | Handles Tick Timer Triggers |
| `PlayerStateTriggerEvaluator` | Handles Player State Triggers |
| `LocationTriggerEvaluator` | Handles Location Triggers |
| `CustomRuleTriggerEvaluator` | Handles Custom Rule Triggers |
| `CompositeTriggerEvaluator` | Handles AND/OR composite triggers |
| `TriggerRegistry` | Maps trigger type strings to evaluator classes |

**Design Notes**:
- Each tick, all game events that occurred during that tick are batched and evaluated together. This prevents race conditions where one event in a tick affects another.
- Triggers are stateless evaluators — they don't track state themselves. State tracking belongs to the Encounter Engine.
- Composite triggers support nesting (e.g., "Animation 1234 ON boss OR Projectile 5678 FROM boss").
- Trigger definitions in JSON map to these evaluator classes via a `type` field.

### 8.5 Overlay System

**Purpose**: Renders visual callouts on the game screen as overlays.

**Responsibilities**:
- Render prayer swap indicators (large icons showing which prayer to use)
- Render countdowns ("3... 2... 1...") for upcoming mechanics
- Render boss timeline (current phase progress, next mechanic)
- Render safe tile markers (highlights on tiles the player should stand on)
- Render status indicators (player HP, boss HP, special attack cooldown)
- Render mini HUD (persistent overlay with encounter info)
- Render debug overlays (event log, trigger history, state inspector)

**Overlay Types** (all from idea.md):

| Overlay | Purpose | Display |
|---------|---------|---------|
| **Prayer indicators** | Show which prayer to use | Icon + text on screen |
| **Countdowns** | Tick until next mechanic | Large numeric overlay |
| **Boss timeline** | Phase progress bar | Bar at screen edge |
| **Next mechanic** | What's coming up | Text box |
| **Safe tiles** | Where to stand | Tile highlights |
| **Status indicators** | Player/boss state | Small HUD elements |
| **Boss HP** | Current HP % | Bar + number |
| **Mini HUD** | Persistent encounter info | Small panel |
| **Debug overlays** | Event/trigger log | Scrollable panel |

**Internal Components**:

| Component | Class | Description |
|-----------|-------|-------------|
| `OverlayManager` | Main coordinator, decides which overlays to render |
| `PrayerIndicatorOverlay` | Renders prayer swap icons |
| `CountdownOverlay` | Renders numeric tick countdown |
| `TimelineOverlay` | Renders boss phase timeline |
| `SafeTileOverlay` | Renders tile highlights |
| `StatusIndicatorOverlay` | Renders status HUD elements |
| `MiniHudOverlay` | Renders persistent encounter info |
| `DebugOverlay` | Renders event log and trigger history |
| `OverlayRenderer` | Low-level drawing calls (uses RuneLite's `Graphics2D` / `OverlayUtil`) |

**Design Notes**:
- Overlays are positioned by RuneLite's overlay system — we provide `Overlay` implementations that return `BufferedImage` or draw directly on `Graphics2D`.
- Visual styling is configurable (color, opacity, size, position) via RuneLite config items.
- A global "debug mode" config option toggles the debug overlays.
- Overlays respect a "quiet hours" config — after a callout fires, suppress non-critical visual overlays for N ticks to avoid screen clutter.
- Colorblind-safe palettes are provided as config options from the start (MVP includes basic color choices; advanced presets are post-MVP).

### 8.6 Audio Engine

**Purpose**: Plays audio callouts (pre-recorded TTS `.ogg` files) at tick-precise moments.

**Responsibilities**:
- **Callout queue**: FIFO queue for audio callouts, with priority reordering
- **Priority system**: Critical callouts ("Pray Melee!") preempt informational ones
- **Interruptions**: Stop currently-playing callout if higher-priority one fires
- **Voice packs**: Different TTS voice variants per callout category (future)
- **Localization**: Voice files keyed by language/culture (future)
- **Volume control**: Per-category volume (critical, warning, informational)
- **Timing offsets**: Fine-tune playback timing to align with tick boundaries

**Inputs**:
- Audio callout requests from the Coaching Engine
- Audio files from loaded encounter packs (`.ogg` files in the pack's `audio/` directory)

**Outputs**:
- Audio playback via `javax.sound.sampled` or RuneLite's audio API

**Internal Components**:

| Component | Class | Description |
|-----------|-------|-------------|
| `AudioEngine` | Main entry point, manages callout queue and playback |
| `AudioQueue` | FIFO queue with priority reordering |
| `AudioPriorityResolver` | Assigns priority to audio callouts |
| `AudioPlayer` | Low-level `.ogg` playback via `javax.sound.sampled` |
| `VolumeController` | Manages per-category volume + master volume |
| `TimingAdjuster` | Applies tick-precision timing offsets |

**Design Notes**:
- Audio files are packaged in encounter packs as `.ogg` (Vorbis) format. They are generated offline by running TTS (Kokoro or Edge TTS) on callout text during pack creation.
- The audio engine never blocks the tick thread. Playback is initiated asynchronously.
- Callouts have a `category` (critical, warning, informational, phase_transition). Each category has its own volume slider and cooldown.
- The `TimingAdjuster` applies a configurable millisecond offset to align audio playback with tick boundaries (typically -50 to +50ms).
- Interrupt rules: a "critical" callout interrupts and cancels a currently-playing "warning" or "informational" callout. "Warning" interrupts "informational" but not "critical". Same-category callouts are queued.

### 8.7 Knowledge Compiler / AI Knowledge Generator

**Purpose**: Offline tool that reads the OSRS Wiki, uses an LLM to generate draft encounter JSON definitions, then validates them before human review.

**Responsibilities**:
- **Input sources**: OSRS Wiki pages (via Wiki API or scraped HTML), optionally supplemented by manual notes
- **Wiki parsing**: Extract boss mechanics, phase descriptions, special attacks, timing info
- **Prompt engineering**: LLM is prompted to structure extracted info into the encounter JSON schema
- **Schema conversion**: Map LLM output to the validated JSON schema (with fallback/manual editing)
- **Validation**: AI agent validator (like an autonomous coding assistant) checks:
  - Schema conformance (required fields, valid trigger types, valid callout references)
  - Logic sanity (no contradictory phases, triggers that can never fire, missing audio files referenced)
  - Tick timing plausibility (callout offsets are within reasonable ranges)
- **Human review**: Final approval step — human checks that the mechanics are correct and complete
- **Publishing pipeline**: Assemble validated JSON + generated audio files into a distributable `.zip` pack

**Inputs**:
- OSRS Wiki URL (e.g., `https://oldschool.wiki/w/Nex`)
- Manual notes (optional, for mechanics the wiki doesn't cover well)
- TTS engine configuration (for generating audio files from callout text)

**Outputs**:
- Encounter pack `.zip` (JSON + audio `.ogg` files)
- Validation report (issues found + confidence scores)
- Changelog (diff from previous version, if updating)

**Internal Components**:

| Component | Description |
|-----------|-------------|
| `WikiFetcher` | Downloads wiki page content |
| `WikiParser` | Extracts structured data (mechanics, timings, phases) from wiki text |
| `LLMPrompter` | Constructs and sends prompts to the LLM (ChatGPT/Claude API) |
| `JSONGenerator` | Converts LLM response to encounter JSON |
| `SchemaValidator` | Validates JSON against the encounter schema |
| `AIValidatorAgent` | Autonomous agent that checks logic + completeness (can make edits) |
| `HumanReviewInterface` | Simple web page or CLI for human review + approval |
| `AudioGenerator` | Runs TTS on all callout text, packages `.ogg` files |
| `PackBuilder` | Assembles JSON + audio into `.zip` pack |
| `PublishingPipeline` | Tags version, generates changelog, writes distribution metadata |

**Design Notes**:
- The AI Knowledge Pipeline is a **separate tool** (not part of the RuneLite plugin). It runs on developer machines.
- The LLM is prompted with a detailed system prompt: "You are generating an encounter definition for a RuneLite coaching plugin. Extract all boss mechanics, phase transitions, special attacks, and timing. Output valid JSON matching schema X."
- The AI Validator Agent (an autonomous coding assistant like opencode) performs static analysis on the JSON: checks that every referenced audio file exists, every trigger type is valid, every phase transition has entry conditions, etc. It can suggest fixes.
- Human review is the final gate — the reviewer checks that mechanics are actually correct (e.g., "does Nex really use ranged after 3 special attacks?").
- Audio generation runs after human approval — TTS processes all callout text and produces `.ogg` files at the correct naming convention.
- Packs are versioned (semantic versioning). The pipeline can diff against a previous version and generate a changelog.

### 8.8 Configuration Manager

**Purpose**: Manages all plugin settings via RuneLite's config system.

**Responsibilities**:
- Global settings (plugin enabled, debug mode, pack directory path)
- Per-boss settings (which bosses are active, per-callout toggles)
- Audio settings (master volume, per-category volume, TTS voice selection)
- Visual settings (overlay opacity, text size, colorblind mode, overlay positions)
- Accessibility settings (audio-only mode, visual-only mode, high-contrast mode)
- Profile management (save/load different config profiles)

**Internal Components**:

| Component | Description |
|-----------|-------------|
| `CoachConfig` | RuneLite `@ConfigItem` definitions |
| `ConfigManager` | Loads/saves config to RuneLite's config storage |
| `ProfileManager` | Manages named config profiles |

**Design Notes**:
- Configuration is stored in RuneLite's standard config files (no custom file I/O).
- Per-boss settings are stored as a JSON string in a single config item (RuneLite doesn't have native per-boss config).
- Profiles allow players to switch between "learning mode" (verbose callouts), "practice mode" (minimal callouts), "performance mode" (audio-only).

### 8.9 Logging System

**Purpose**: Provides debug logging and an in-game debug overlay for development and player verification.

**Responsibilities**:
- Log all game events received from RuneLite (with tick number)
- Log all trigger evaluations (which triggers fired, which didn't)
- Log all callout decisions (what was called out, at what tick)
- Log encounter state changes (phase transitions, mechanic activations)
- Provide a scrollable debug overlay showing recent events
- Write detailed logs to a file (`coach-debug.log`) for post-fight analysis

**Internal Components**:

| Component | Description |
|-----------|-------------|
| `Logger` | Wrapper around `java.util.logging` with custom formatting |
| `EventLogger` | Logs game events + tick numbers |
| `TriggerLogger` | Logs trigger evaluations |
| `CalloutLogger` | Logs callout decisions |
| `DebugOverlay` | In-game scrollable panel showing recent log entries |
| `FileLogWriter` | Writes logs to disk (configurable, disabled by default) |

**Design Notes**:
- Logging is disabled by default. Players enable "debug mode" via config to see the debug overlay.
- Log files include full event history (useful for the future Replay/Practice Mode).
- The debug overlay can be filtered (show only critical callouts, show all events, etc.).

---

## 9. Data Models

### Core Domain Models

| Model | Purpose |
|-------|---------|
| **Boss** | Top-level encounter definition (name, ID, description, phases) |
| **Encounter** | Runtime instance of a Boss (loaded from JSON, with state) |
| **Phase** | A segment of the boss fight (has entry conditions, mechanics, exit conditions) |
| **Mechanic** | A single boss behavior pattern (triggers → callouts) |
| **Trigger** | A condition that detects a game event (animation, projectile, etc.) |
| **Condition** | A boolean check used in triggers and phase transitions |
| **Callout** | A visual + audio notification (text + audio file reference + timing offset) |
| **Overlay** | Visual rendering configuration for a callout (type, position, color) |
| **Timeline** | Ordered list of mechanics within a phase (for prediction) |
| **Configuration** | Plugin settings (global + per-boss + audio + visual) |
| **PlayerState** | Snapshot of player status (HP, prayer, position, animation) |
| **BossState** | Snapshot of boss status (phase, HP, active mechanics, timers) |

### Runtime State Models

```
PlayerState {
  hp: int
  maxHp: int
  prayer: PrayerState       // active prayers
  position: WorldPoint      // player tile
  animation: int             // current animation ID
  tile: Tile                 // detailed tile info
}

BossState {
  id: int                    // NPC ID
  name: String               // boss name
  hp: int                    // current HP
  maxHp: int                 // max HP
  phase: String              // current phase ID
  phaseTick: int             // ticks elapsed in current phase
  activeMechanics: Set<String>  // mechanics currently "live"
  timers: Map<String, Integer>  // countdown timers for mechanics
  position: WorldPoint       // boss position
}
```

---

## 10. JSON Schema Specification

The encounter JSON schema is the contract between the Knowledge Compiler (which produces JSON) and the Encounter Engine (which consumes it). Every encounter pack must conform to this schema.

### Schema Structure

```
encounter_pack.json
├── schemaVersion: "1.0"        // schema version for migration
├── metadata:                   // pack metadata
│   ├── packId: string
│   ├── name: string
│   ├── description: string
│   ├── author: string
│   ├── version: string
│   └── gameVersion: string     // OSRS game version this pack targets
├── bosses:                     // array of boss definitions
│   └── BossDefinition:
│       ├── bossId: string      // unique identifier (e.g., "nex")
│       ├── name: string        // display name
│       ├── npcId: int          // RuneScape NPC ID
│       ├── description: string
│       ├── phases: PhaseDefinition[]
│       │   └── PhaseDefinition:
│       │       ├── phaseId: string
│       │       ├── name: string
│       │       ├── entryTrigger: TriggerDefinition   // what starts this phase
│       │       ├── exitTriggers: TriggerDefinition[]  // what ends this phase
│       │       ├── mechanics: MechanicDefinition[]   // mechanics active in this phase
│       │       └── transitions: TransitionDefinition[] // to other phases
│       ├── mechanics: MechanicDefinition[]            // (optional) shared mechanics
│       └── recovery: RecoveryDefinition               // (optional) reset behavior
├── mechanics:                  // (optional) shared mechanics reusable across phases
│   └── MechanicDefinition:
│       ├── mechanicId: string
│       ├── name: string
│       ├── triggers: TriggerDefinition[]
│       ├── callouts: CalloutDefinition[]
│       ├── conditions: ConditionDefinition[]         // extra conditions
│       ├── cooldown: int                              // ticks
│       └── interruptible: boolean
├── callouts:                   // (optional) global callouts
│   └── CalloutDefinition:
│       ├── calloutId: string
│       ├── text: string                               // visual text
│       ├── audioFile: string                          // .ogg filename (optional)
│       ├── category: "critical" | "warning" | "info" | "transition"
│       ├── visual: VisualDefinition                   // overlay config
│       ├── audioOffset: int                           // tick offset for audio
│       ├── visualOffset: int                          // tick offset for visual
│       └── priority: int                              // 1-100 (100 = highest)
├── triggers:                   // (optional) reusable trigger definitions
│   └── TriggerDefinition:
│       ├── type: "animation" | "projectile" | "graphic" | "npc_spawn" |
│               "npc_despawn" | "hp" | "tick_timer" | "player_state" |
│               "location" | "custom" | "composite"
│       ├── [type-specific fields]
│       └── conditions: ConditionDefinition[]          // AND conditions
└── conditions:                 // (optional) reusable condition definitions
    └── ConditionDefinition:
        ├── type: "npc_hp_below" | "npc_hp_above" | "player_hp_below" |
        │         "player_hp_above" | "tick_mod" | "player_in_region" |
        │         "prayer_active" | "prayer_inactive" | "inventory_contains" |
        │         "custom"
        └── [type-specific fields]
```

### Validation Rules

1. `schemaVersion` must be present and match a known version.
2. `metadata.bossId` values must be unique.
3. Every `phaseId` must be unique within its boss.
4. Every `mechanicId` must be unique within its scope (boss or shared).
5. Every `calloutId` must be unique.
6. Every `triggerId` referenced in mechanics must be defined (either in shared triggers or inline).
7. Every `calloutId` referenced in mechanics must be defined.
8. Every `audioFile` referenced in callouts must exist in the pack's `audio/` directory (checked at load time).
9. Tick offsets must be `>= -5` and `<= 10` (callouts shouldn't fire more than 5 ticks early or 10 ticks late).
10. Every boss must have at least one phase.
11. Every phase must have at least one exit trigger (or mark as "final phase").
12. Phase entry conditions must be achievable (no impossible triggers).
13. No duplicate trigger definitions within the same mechanic.

### Version Compatibility

- Schema version `1.0` is the initial version.
- Future schema versions must be backward-compatible or provide a migration path.
- The plugin checks the schema version at load time. If the version is newer than the plugin supports, the pack is rejected with an error message.
- If the version is older, a migration function is applied (if available) or the pack is rejected with a warning.

### Migration Strategy

- Pack authors must specify a `schemaVersion` in their JSON.
- The plugin bundles migration functions for the last N versions.
- When loading a pack with an older version, the plugin applies migrations until the JSON matches the current schema.
- If no migration path exists, the pack is rejected.

---

## 11. File Structure

```
coach/
├── docs/
│   ├── master-architecture.md      ← This file
│   ├── sprint-roadmap.md
│   └── implementation-guide.md
├── plugin/
│   ├── build.gradle
│   ├── runelite-plugin.properties
│   ├── proguard/
│   │   └── proguard-rules.pro
│   └── src/
│       └── main/
│           ├── java/com/coach/plugin/
│           │   ├── CoachPlugin.java           # Plugin entry point
│           │   ├── config/
│           │   │   ├── CoachConfig.java        # RuneLite config items
│           │   │   └── ProfileManager.java     # Config profiles
│           │   ├── events/
│           │   │   ├── EventBus.java           # Event dispatch
│           │   │   ├── GameStateBridge.java    # RuneLite → internal state
│           │   │   └── GameEvent.java          # Internal event wrapper
│           │   ├── trigger/
│           │   │   ├── TriggerEngine.java       # Main trigger coordinator
│           │   │   ├── TriggerEvaluator.java    # Interface
│           │   │   ├── AnimationTriggerEvaluator.java
│           │   │   ├── ProjectileTriggerEvaluator.java
│           │   │   ├── GraphicTriggerEvaluator.java
│           │   │   ├── NpcSpawnTriggerEvaluator.java
│           │   │   ├── HpTriggerEvaluator.java
│           │   │   ├── TickTimerTriggerEvaluator.java
│           │   │   ├── PlayerStateTriggerEvaluator.java
│           │   │   ├── LocationTriggerEvaluator.java
│           │   │   ├── CustomRuleTriggerEvaluator.java
│           │   │   ├── CompositeTriggerEvaluator.java
│           │   │   └── TriggerRegistry.java
│           │   ├── encounter/
│           │   │   ├── EncounterEngine.java       # Main encounter coordinator
│           │   │   ├── EncounterLoader.java       # JSON → runtime objects
│           │   │   ├── PhaseMachine.java          # Phase state machine
│           │   │   ├── MechanicManager.java        # Active mechanics tracker
│           │   │   ├── ConditionEvaluator.java    # JSON condition evaluation
│           │   │   ├── RecoveryHandler.java        # Reset/death handling
│           │   │   ├── Encounter.java              # Runtime encounter
│           │   │   ├── Phase.java                  # Runtime phase
│           │   │   ├── Mechanic.java               # Runtime mechanic
│           │   │   └── model/                     # JSON model classes
│           │   ├── coaching/
│           │   │   ├── CoachingEngine.java        # Main coach
│           │   │   ├── PriorityResolver.java
│           │   │   ├── CalloutScheduler.java
│           │   │   ├── CooldownManager.java
│           │   │   ├── CalloutQueue.java
│           │   │   ├── CoachStateManager.java
│           │   │   ├── PredictionEngine.java
│           │   │   └── Callout.java                # Callout request
│           │   ├── overlay/
│           │   │   ├── CoachOverlay.java           # Main overlay provider
│           │   │   ├── OverlayManager.java
│           │   │   ├── PrayerIndicatorOverlay.java
│           │   │   ├── CountdownOverlay.java
│           │   │   ├── TimelineOverlay.java
│           │   │   ├── SafeTileOverlay.java
│           │   │   ├── StatusIndicatorOverlay.java
│           │   │   ├── MiniHudOverlay.java
│           │   │   ├── DebugOverlay.java
│           │   │   └── OverlayRenderer.java
│           │   ├── audio/
│           │   │   ├── AudioEngine.java
│           │   │   ├── AudioQueue.java
│           │   │   ├── AudioPriorityResolver.java
│           │   │   ├── AudioPlayer.java
│           │   │   ├── VolumeController.java
│           │   │   └── TimingAdjuster.java
│           │   └── logging/
│           │       ├── EventLogger.java
│           │       ├── TriggerLogger.java
│           │       ├── CalloutLogger.java
│           │       └── FileLogWriter.java
│           └── resources/
│               ├── schemas/
│               │   ├── encounter_schema_v1.json     # JSON schema for validation
│               │   └── migration_v0_to_v1.json
│               └── audio/
│                   └── (default placeholder .ogg files)
│
├── knowledge-pipeline/              # Separate tool (not in plugin)
│   ├── pyproject.toml              # Python project
│   ├── src/
│   │   ├── wiki_fetcher.py          # Downloads wiki pages
│   │   ├── wiki_parser.py           # Extracts mechanics from wiki text
│   │   ├── llm_prompter.py          # Sends prompts to ChatGPT/Claude
│   │   ├── json_generator.py        # Converts LLM output to encounter JSON
│   │   ├── schema_validator.py      # Validates JSON schema
│   │   ├── ai_validator_agent.py    # Autonomous agent for logic checks
│   │   ├── human_review_interface/  # Simple web UI for human review
│   │   │   └── app.py
│   │   ├── audio_generator.py        # Runs TTS (Kokoro/Edge) on callout text
│   │   ├── pack_builder.py          # Assembles .zip pack
│   │   └── publishing_pipeline.py   # Version + changelog + distribution
│   └── fixtures/
│       └── sample_wiki_pages/
│
├── encounter-packs/                 # Example packs (for development)
│   ├── nex.pack/
│   │   ├── encounter.json
│   │   └── audio/
│   │       ├── nex_pray_melee.ogg
│   │       └── nex_phase_transition.ogg
│   ├── inferno.pack/
│   │   ├── encounter.json
│   │   └── audio/
│   └── tob_soteboss.pack/
│       ├── encounter.json
│       └── audio/
│
└── tests/
    ├── java/com/coach/plugin/
    │   ├── TriggerEngineTest.java
    │   ├── EncounterEngineTest.java
    │   ├── CoachingEngineTest.java
    │   ├── OverlayManagerTest.java
    │   └── AudioEngineTest.java
    └── resources/
        └── test_packs/
            ├── valid_pack.zip
            ├── invalid_schema_pack.zip
            └── missing_audio_pack.zip
```

### Plugin Layout

The RuneLite plugin follows the standard RuneLite plugin structure:

- **`build.gradle`**: Gradle build script with RuneLite plugin repository dependency
- **`runelite-plugin.properties`**: Plugin metadata (name, description, author, loader)
- **`src/main/java/com/coach/plugin/`**: All Java source files
- **`src/main/resources/schemas/`**: JSON schema files for pack validation
- **`src/main/resources/audio/`**: Default audio assets (minimal)

### Resources

- **JSON schema**: `schemas/encounter_schema_v1.json` — the authoritative schema for encounter packs
- **Migration files**: `schemas/migration_*.json` — JSON transformations for schema version upgrades
- **Audio assets**: `.ogg` files are shipped with encounter packs, not with the plugin JAR
- **Default pack**: A minimal example pack ships with development builds for testing

### Knowledge Packs

Encounter packs are ZIP files (`*.zip`) placed by the user in the plugin's pack directory:

```
<punfolder>/coach/encounters/
    nex-1.2.3.zip
    inferno-1.0.0.zip
    tob_soteboss-1.1.0.zip
```

Each pack contains:
```
<nex-1.2.3.zip>
├── encounter.json       # main encounter definition
└── audio/               # audio assets
    ├── pray_melee.ogg
    ├── pray_ranged.ogg
    ├── phase_transition.ogg
    └── ...
```

### Tests

- **Unit tests**: JUnit 5 tests for each engine component (`TriggerEngineTest`, `EncounterEngineTest`, etc.)
- **Integration tests**: Full event → callout flow tests using mock game state
- **Boss simulations**: Pre-recorded tick sequences from actual boss fights, replayed through the engine
- **Schema tests**: Validation of test encounter packs (valid + invalid examples)
- **Replay testing**: Load a fight replay, verify callouts fire on correct ticks
- **Regression testing**: After schema changes, verify existing packs still validate

### Documentation

- `docs/master-architecture.md` (this file)
- `docs/sprint-roadmap.md`
- `docs/implementation-guide.md`
- `docs/examples/ENCODING.md` — guide for pack authors

### Assets

- Default color palette (colorblind-safe)
- Placeholder audio (single `.ogg` file for "test callout")
- Icon assets (plugin logo, overlay icons)

---

## 12. Performance

### Tick Processing Budget

OSRS ticks are 600ms. The plugin must complete all event processing within each tick.

| Phase | Max Time | Details |
|-------|----------|---------|
| Event batch processing | ~200ms | Process all events from the tick (animation, projectile, graphic, NPC, HP) |
| Trigger evaluation | ~100ms | Evaluate all triggers against events |
| Encounter state update | ~100ms | Update phase state, mechanic timers |
| Coaching decision | ~100ms | Decide callouts, schedule visual + audio |
| Overlay rendering | ~100ms | Render visual overlays |
| Audio initiation | ~50ms | Start audio playback (async) |
| **Total per tick** | **~600ms** | **Hard limit** |

**Optimization strategies**:
- Event batching: all events in a tick are collected, then processed as a batch. No per-event processing.
- Trigger short-circuiting: if a trigger's preconditions aren't met (e.g., wrong NPC), skip detailed evaluation.
- Mechanic caching: active mechanics are cached; only re-evaluate when relevant events arrive.
- Audio pre-loading: audio files are pre-loaded into memory when the pack loads, not on first use.
- Overlay culling: overlays outside the viewport are not rendered.

### Memory Usage

- Encounter pack JSON: ~50-200 KB per boss (parsed into memory as Java objects)
- Audio files: ~500 KB - 2 MB total per pack (pre-loaded as `ByteBuffer`)
- Event log (debug mode): ring buffer of last 1000 events (~1 MB)
- **Target**: < 10 MB plugin memory footprint during active coaching

### Caching

- Loaded encounter packs are cached in memory for the session
- Parsed JSON is cached (no re-parsing unless the file changes on disk)
- Audio `ByteBuffer` objects are cached in memory after first load
- Trigger evaluation results are cached per tick (within the same tick, a trigger that already fired doesn't re-evaluate)

### Audio Latency

- Target: audio playback starts within 50ms of the tick boundary
- Achieved by pre-loading all `.ogg` files into memory at pack load
- Using `javax.sound.sampled.Clip` for low-latency playback
- The `TimingAdjuster` applies a configurable offset (default: -100ms to fire audio slightly early, compensating for human reaction time)

### Rendering Optimization

- RuneLite's `Overlay` system is already optimized — we only provide the `render` method
- Visual overlays use `OverlayUtil` primitives (rectangles, circles, text) — no complex graphics
- Overlay opacity is configurable; lower opacity = faster rendering
- Debug overlays are only rendered when debug mode is enabled

### Profiling

- A built-in profiling overlay (debug mode) shows CPU time per subsystem per tick
- A log file (`coach-profiling.log`) records per-tick timing breakdown (debug mode only)
- Memory usage is reported in the debug overlay

---

## 13. Security

### Plugin Safety

- The plugin **never sends data over the network**. All pack loading is from the local filesystem.
- The plugin **never reads or writes files outside** the RuneLite plugin directory (packs, logs, configs).
- All file I/O is validated — pack files are checked for path traversal before extraction.
- Encounter packs are loaded as ZIP files; the plugin only reads JSON + audio from within the ZIP (no arbitrary file access).

### Local-Only Operation

- No telemetry, no analytics, no crash reporting by default.
- An opt-in crash log can be generated to a local file (user chooses to submit).

### No Automation Guarantees

- The plugin performs no `Input` API calls (no key presses, no mouse control).
- The plugin does not modify game state — it only reads and displays information.
- RuneLite's plugin hub review will verify this claim.

### Input Validation

- All encounter pack JSON is validated against the schema before loading.
- All trigger conditions are type-checked at load time (no arbitrary code execution).
- Audio file references are verified to exist before playback (missing files fall back to text-only).

### Data Integrity

- Encounter packs are versioned. The plugin checks the schema version and applies migrations if needed.
- If a pack fails to load, all other packs continue to function (failure isolation).
- A corrupted pack is quarantined (renamed to `.broken`) and logged, not silently ignored.

---

## 14. Testing Strategy

### Unit Tests

| Component | Test Coverage |
|-----------|---------------|
| TriggerEngine | Each trigger type evaluates correctly against mock events |
| EncounterEngine | Phase transitions, mechanic activation, recovery logic |
| CoachingEngine | Priority resolution, callout scheduling, cooldown enforcement |
| OverlayManager | Correct overlays rendered for given state |
| AudioEngine | Queue ordering, priority preemption, volume application |
| ConditionEvaluator | All condition types evaluate correctly |
| SchemaValidator | Valid packs pass, invalid packs fail with clear messages |

### Integration Tests

- Full pipeline: mock game events → Trigger Engine → Encounter Engine → Coaching Engine → Overlay + Audio
- Verify callouts fire on the correct tick
- Verify audio + visual callouts are scheduled together

### Boss Simulations

- Pre-recorded tick-by-tick event logs from actual boss fights (Nex, Inferno, ToB)
- Replay the events through the engine and verify callouts match expected timings
- Use these as regression tests (if a boss pack update breaks expected callouts, the test fails)

### Replay Testing

- Record game events to a JSON log file during a fight
- Replay the log through the engine in "review mode"
- Verify that callouts would have fired correctly
- (This is the foundation for the future Replay/Practice Mode)

### Regression Testing

- After any schema change, all example packs must still validate + load
- After any engine change, all boss simulation tests must still pass
- Automated in CI (GitHub Actions runs on PR)

### Performance Benchmarks

- Tick processing time must stay under 600ms (measured per component)
- Memory growth must not exceed 1 MB over a 10-minute fight
- Audio playback latency must be under 50ms

---

## 15. Future Expansion

### Post-MVP Features

#### Replay Analyzer & Practice Mode

By recording fight events and replaying them through the same coaching engine, users could review exactly where they missed prayers, delayed movement, or used supplies inefficiently. The architecture supports this without major redesign — the EventBus can accept recorded events instead of live ones, and the CoachingEngine + OverlayManager work on any event source.

**Planned additions**:
- Event recorder: saves all game events to a JSON file during a fight
- Replay loader: reads event log, replays at 1x / 2x / 4x speed
- Performance report: highlights missed callouts, late reactions, HP waste
- "What if?" mode: replay with different callout settings to find optimal configuration

#### Community Knowledge Repository

A web-based platform where pack authors can publish, version, and share encounter packs. Users can browse, download, and rate packs. The plugin checks for updates (opt-in, not automatic).

#### AI Voice Coach

Enhancements to the audio system:
- Multiple TTS voice variants (different personalities: "chill friend", "intense coach", "analytical mentor")
- Context-aware volume adjustment (raise volume during chaos, lower during quiet phases)
- Voice pack marketplace (community-created voice sets)

#### Adaptive Coaching

- The coach learns from the player's performance (missed callouts, delayed reactions)
- Adjusts callout verbosity over time (start verbose, become subtle as player improves)
- Personalized timing offsets (shift callout timing based on player's reaction latency)

#### Multi-Game Adapter Layer

- Abstract the RuneLite-specific event layer
- Add adapter layers for other game clients (Blizzard Battle.net, etc.)
- Shared core engines (Coaching, Encounter, Overlay) remain generic
- Game-specific plugins handle event translation

### Future Architecture Additions

#### Cloud Sync (opt-in)

- Sync encounter packs and config profiles across devices
- End-to-end encrypted (user provides encryption key)
- Optional analytics for pack authors (how many players used their pack, completion rates)

### Machine Learning Research Layer

- Predict player mistakes before they happen (based on movement patterns, HP trends)
- Automatically detect missed mechanics from event logs
- Recommend optimal pack configurations for specific player skill levels

### Plugin Marketplace Integration

- In-plugin pack browser (if RuneLite allows marketplace-style plugins)
- Rating system for encounter packs
- Automatic pack update notifications

### Telemetry & Diagnostics (opt-in)

- Anonymous performance metrics (tick processing times, memory usage)
- Pack usage statistics (which bosses, which packs)
- Crash reporting with automatic log submission
