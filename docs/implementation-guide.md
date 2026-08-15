# Project Coach — Implementation Guide

> **Version:** 1.0
> **Status:** Draft — Sprint 0 (Pre-MVP)
> **Audience:** Developers, AI coding agents, pack authors
> **Related:** See `docs/master-architecture.md` (architecture overview), `docs/sprint-roadmap.md` (build order)

This document is the **technical reference** for implementing Project Coach. It provides concrete specifications — Java class interfaces, method signatures, JSON schema definitions, file layouts, and component hierarchies — that map directly to code. Every sprint in `docs/sprint-roadmap.md` references sections from this guide.

---

## Table of Contents

1. [Java Conventions & Coding Standards](#1-java-conventions--coding-standards)
2. [RuneLite Environment Setup](#2-runelite-environment-setup)
3. [Plugin Lifecycle Implementation](#3-plugin-lifecycle-implementation)
4. [Event System](#4-event-system)
5. [Trigger Engine](#5-trigger-engine)
6. [Encounter Engine](#6-encounter-engine)
7. [Coaching Engine](#7-coaching-engine)
8. [JSON Schema & Data Parsing](#8-json-schema--data-parsing)
9. [Overlay System](#9-overlay-system)
10. [Audio Engine](#10-audio-engine)
11. [Configuration Manager](#11-configuration-manager)
12. [Boss Development Workflow](#12-boss-development-workflow)
13. [AI Knowledge Pipeline](#13-ai-knowledge-pipeline)
14. [Testing Strategy](#14-testing-strategy)
15. [Release Process](#15-release-process)
16. [Future Modules](#16-future-modules)

---

## 1. Java Conventions & Coding Standards

### Language

- **Java 11** (RuneLite's required JDK version)
- No external dependencies beyond what RuneLite provides (use `runelite-client`, `runelite-api`, `guava`, `jshell`)
- Avoid reflection, dynamic proxies, and bytecode manipulation (RuneLite plugin hub review blocks these)
- No network calls, no file system access outside RuneLite's config directory

### Naming Conventions

| Element | Convention | Example |
|---------|-----------|---------|
| Classes | PascalCase | `TriggerEngine`, `CalloutQueue` |
| Methods | camelCase | `evaluateTriggers`, `scheduleCallout` |
| Variables | camelCase | `currentPhase`, `isEnabled` |
| Constants | UPPER_SNAKE_CASE | `TICK_MS`, `MAX_CALLQUEUE_SIZE` |
| Enum values | UPPER_CASE | `CRITICAL`, `WARNING`, `INFO` |
| Package | com.coach.plugin.{module} | `com.coach.plugin.trigger`, `com.coach.plugin.encounter` |

### Project Organization

```
src/main/java/com/coach/plugin/
├── CoachPlugin.java              # Entry point
├── config/                       # RuneLite config items
├── events/                       # Event bus + game state bridge
├── trigger/                      # Trigger engine (evaluation)
├── encounter/                    # Encounter engine (phases, mechanics, state)
├── coaching/                     # Coaching engine (decisions, scheduling)
├── overlay/                      # Visual overlays
├── audio/                        # Audio playback
├── logging/                      # Debug logging
└── accessibility/                # Accessibility modes
```

### Dependency Management

`build.gradle`:

```gradle
dependencies {
    compileOnly "com.discordmc:runelite:runelite-source-1.10.13"
    annotationProcessor "com.discordmc:runelite:runelite-source-1.10.13"
    compileOnly 'com.google.code.gson:gson:2.8.9'  // JSON parsing (comes with RuneLite)
    compileOnly 'com.google.guava:guava:30.2-jre'   // Already bundled in RuneLite
    testCompile 'org.junit.jupiter:junit-jupiter:5.8.2'
}
```

### Testing Requirements

- **JUnit 5** for all unit tests
- **Mockito** for mocking RuneLite objects (NPCC, Player, etc.)
- Tests must run headless (no RuneLite client instance)
- Test coverage target: ≥80% for all packages
- Each engine class must have at least one test class

---

## 2. RuneLite Environment Setup

### Prerequisites

| Tool | Version | Install |
|------|---------|---------|
| JDK | 11 | `sdk install java 11.0.22-tem` |
| Gradle | 7.6+ | `sdk install gradle 7.6` (or use wrapper) |
| RuneLite source | Latest | Clone from `runelite/runelite` GitHub |
| IDE | IntelliJ IDEA | Install RuneLite plugin development guide |

### Build Setup

1. Clone your fork of `coach` repo
2. Ensure JDK 11 is on PATH: `java -version` → `11.0.x`
3. Run `./gradlew build` (or `gradlew.bat build` on Windows)
4. To run in a test RuneLite instance: `./runelite-external ./gradlew run`

### IDE Setup (IntelliJ IDEA)

1. Open the project (import `build.gradle` as Gradle project)
2. Set **Project SDK** to JDK 11
3. Set **Run/Debug Configuration** → Application → Main class: `net.runelite.deob.RuneLite`
4. VM options: `-ea -XX:+EnableG1GC -jar <runelite>/runelite.jar`
5. Working directory: project root

### Debugging Strategies

| Technique | How |
|-----------|-----|
| **Local test server** | `gradle run` starts a local RuneLite with your plugin auto-loaded |
| **Event logging** | Use debug overlay (Sprint 3) to see events per tick |
| **Tick-aligned breakpoints** | Set breakpoints inside `@Subscribe Tick` methods — they fire every 600ms |
| **Mock testing** | Unit tests use Mockito to mock `Client`, `NPC`, `Player`, `WorldPoint` |
| **Log file inspection** | Logs are written to `<runelite>/logs/coach-debug.log` |

### Hot Reload

- RuneLite does not support true hot reload for plugins
- Workflow: edit → `gradle build` → JAR is auto-picked up by running RuneLite instance (if using `runelite-external`)
- For rapid iteration: use the debug overlay + config toggles to test changes without restarting

---

## 3. Plugin Lifecycle Implementation

### Entry Point

File: `src/main/java/com/coach/plugin/CoachPlugin.java`

```java
@PluginDescriptor(
    name = "Coach",
    description = "Real-time boss coaching with visual and audio callouts",
    tags = {"pvm", "bossing", "coaching", "overlay", "audio"},
    loadName = "coach"
)
public class CoachPlugin extends Plugin {
    @Inject private Client client;
    @Inject private OverlayManager overlayManager;
    @Inject private CoachConfig config;
    @Inject private EventBus eventBus;  // RuneLite's event bus

    private EventBus coachEventBus;     // Internal event bus
    private TriggerEngine triggerEngine;
    private EncounterEngine encounterEngine;
    private CoachingEngine coachingEngine;
    private AudioEngine audioEngine;
    private DebugOverlay debugOverlay;

    @Override
    protected void startUp() {
        coachEventBus = new EventBus();
        triggerEngine = new TriggerEngine(coachEventBus);
        encounterEngine = new EncounterEngine(coachEventBus);
        coachingEngine = new CoachingEngine(triggerEngine, encounterEngine);

        // Load encounter packs
        PackManager packManager = new PackManager(config.packDirectory());
        encounterEngine.loadPacks(packManager.getValidPacks());

        // Register overlays
        overlayManager.add(new CoachOverlay(coachingEngine, encounterEngine));
        if (config.debugMode()) {
            overlayManager.add(new DebugOverlay());
        }

        // Subscribe to RuneLite events
        eventBus.subscribe(Tick.class, this, this::onTick);
        eventBus.subscribe(AnimationChanged.class, this, this::onAnimationChanged);
        eventBus.subscribe(ProjectileSpawned.class, this, this::onProjectileSpawned);
        // ... (all 10 event types)
    }

    @Override
    protected void shutDown() {
        overlayManager.removeIf(o -> o instanceof CoachOverlay);
        eventBus.unsubscribe(Tick.class, this);
        // ... unsubscribe all events
        coachingEngine.shutdown();
    }

    // Event handlers
    private void onTick(Tick tick) {
        coachEventBus.post(tick);
    }
    private void onAnimationChanged(AnimationChanged event) {
        coachEventBus.post(event);
    }
    // ... etc.
}
```

### Config

File: `src/main/java/com/coach/plugin/config/CoachConfig.java`

```java
@ConfigGroup("coach")
public interface CoachConfig extends Config {
    @ConfigItem(title = "Global Settings", name = "global", type = ConfigItemType.GROUP_HEADER)
    String globalHeader();

    @ConfigItem(title = "Plugin Enabled", name = "enabled", description = "Enable Project Coach")
    default boolean enabled() { return true; }

    @ConfigItem(title = "Debug Mode", name = "debugMode", description = "Show debug overlays + logs")
    default boolean debugMode() { return false; }

    @ConfigItem(title = "Encounter Pack Directory", name = "packDir")
    default String packDirectory() {
        return Paths.get(RuneLite.CONFIG_DIR, "coach", "encounters").toString();
    }

    // Audio settings
    @ConfigItem(title = "Master Volume", name = "audioMasterVolume", description = "0-100%")
    default int audioMasterVolume() { return 70; }

    // Visual settings
    @ConfigItem(title = "Visual Opacity", name = "visualOpacity", description = "0-100%")
    default int visualOpacity() { return 85; }

    // ... (all other config items from Sprint 17)
}
```

### Dependency Injection

- All RuneLite-provided services (`Client`, `OverlayManager`, `EventBus`, `ItemContainer` etc.) are `@Inject`-ed by RuneLite's Guice container
- Custom services (TriggerEngine, EncounterEngine, etc.) are instantiated manually in `startUp()`
- For testing: inject mock implementations of RuneLite services

---

## 4. Event System

### Core Event Types

The internal `EventBus` dispatches these wrapped events:

```java
public class GameEvent {
    private final EventType type;
    private final int tick;
    private final Object payload;  // RuneLite event object
    private final Instant timestamp;
}

public enum EventType {
    TICK,
    ANIMATION_CHANGED,
    PROJECTILE_SPAWNED,
    PROJECTILE_MOVED,
    GRAPHIC_CHANGED,
    NPC_SPAWNED,
    NPC_DESPAWNED,
    NPC_HP_CHANGED,
    PLAYER_HP_CHANGED,
    PRAYER_STATE_CHANGED,
    PLAYER_POSITION_CHANGED,
    VARBIT_CHANGED
}
```

### Game State Bridge

File: `src/main/java/com/coach/plugin/events/GameStateBridge.java`

Translates RuneLite objects into internal state models:

```java
public class GameStateBridge {
    private final Client client;

    public PlayerState getPlayerState() {
        return PlayerState.builder()
            .hp(client.getBoostedLevel(Skill.HITPOINTS))
            .maxHp(client.getRealLevel(Skill.HITPOINTS))
            .prayer(client.getBoostedLevel(Skill.PRAYER))
            .position(client.getLocalPlayer().getWorldLocation())
            .animation(client.getLocalPlayer().getAnimation())
            .build();
    }

    public Optional<BossState> getBossState(int npcId) {
        Optional<NPC> npc = client.getNpcs().stream()
            .filter(n -> n.getId() == npcId)
            .findFirst();
        if (npc.isEmpty()) return Optional.empty();

        return Optional.of(BossState.builder()
            .id(npcId)
            .name(npc.get().getName())
            .hp(npc.get().getHealth())
            .maxHp(getNpcMaxHp(npc.get()))
            .position(npc.get().getWorldLocation())
            .build());
    }
}
```

### Tick Batching

The EventBus collects all events within a tick (600ms) into a batch:

```java
public class EventBus {
    private final Queue<GameEvent> tickBuffer = new ArrayDeque<>();

    public void post(GameEvent event) {
        tickBuffer.offer(event);
    }

    // Called by Tick handler
    public void flushTick(int tickNumber) {
        List<GameEvent> batch = new ArrayList<>(tickBuffer);
        tickBuffer.clear();
        notifyListeners(tickNumber, batch);
    }

    private void notifyListeners(int tick, List<GameEvent> events) {
        for (EventListener listener : listeners) {
            listener.onTick(tick, events);
        }
    }
}
```

---

## 5. Trigger Engine

### Core Interface

```java
public interface TriggerEvaluator {
    boolean evaluate(GameEvent event, EncounterState state);
    String getTriggerId();
}

public class TriggerResult {
    private final String triggerId;
    private final int tick;
    private final Object payload;  // matched event data
}
```

### Trigger Evaluator Implementations

Each trigger type is a class. Key examples:

**AnimationTriggerEvaluator:**

```java
public class AnimationTriggerEvaluator implements TriggerEvaluator {
    private final String triggerId;
    private final int npcId;       // -1 for player
    private final int animationId;

    @Override
    public boolean evaluate(GameEvent event, EncounterState state) {
        if (event.type() != EventType.ANIMATION_CHANGED) return false;
        AnimationChanged anim = (AnimationChanged) event.payload();
        if (anim.getNpc() != null) {
            return anim.getNpc().getId() == npcId
                && anim.getNpc().getAnimation() == animationId;
        }
        // Player animation
        return npcId == -1
            && state.getPlayer().getAnimation() == animationId;
    }
}
```

**ProjectileTriggerEvaluator:**

```java
public class ProjectileTriggerEvaluator implements TriggerEvaluator {
    private final int projectileId;
    private final Integer srcNpcId;  // nullable
    private final Integer dstTileX, dstTileY;  // nullable

    @Override
    public boolean evaluate(GameEvent event, EncounterState state) {
        if (event.type() != EventType.PROJECTILE_SPAWNED) return false;
        Projectile p = ((ProjectileSpawned) event.payload()).getProjectile();
        return p.getId() == projectileId
            && (srcNpcId == null || p.getClientVelocityX() == srcNpcId)
            && (dstTileX == null || p.getTargetX() == dstTileX);
    }
}
```

**CompositeTriggerEvaluator:**

```java
public class CompositeTriggerEvaluator implements TriggerEvaluator {
    enum Logic { AND, OR }
    private final Logic logic;
    private final List<TriggerEvaluator> children;
    private final BitSet childResults;

    @Override
    public boolean evaluate(GameEvent event, EncounterState state) {
        boolean result = (logic == Logic.AND) ? true : false;
        for (TriggerEvaluator child : children) {
            boolean childResult = child.evaluate(event, state);
            childResults.set(children.indexOf(child), childResult);
            if (logic == Logic.AND && !childResult) return false;
            if (logic == Logic.OR && childResult) return true;
        }
        return result;
    }
}
```

### Trigger Factory (JSON → Object)

```java
public class TriggerFactory {
    public static TriggerEvaluator fromJson(JsonObject json, String contextId) {
        String type = json.get("type").getAsString();
        switch (type) {
            case "animation":
                return new AnimationTriggerEvaluator(
                    contextId + ":anim",
                    json.get("npcId").getAsInt(),
                    json.get("animationId").getAsInt()
                );
            case "projectile":
                return new ProjectileTriggerEvaluator(...);
            case "composite":
                return buildComposite(json, contextId);
            default:
                throw new IllegalArgumentException("Unknown trigger type: " + type);
        }
    }
}
```

### Trigger Evaluation Flow

```
Tick events (batched) → TriggerEngine.evaluateTick(tick, events, encounterState)
  For each Encounter:
    For each active Mechanic:
      For each Trigger in Mechanic:
        For each event in batch:
          if trigger.evaluate(event, state):
            → MechanicManager.mechanicTriggered(mechanicId, event)
            → break (one trigger fire per mechanic per tick)
```

---

## 6. Encounter Engine

### Runtime Data Models

```java
public class Encounter {
    private final String bossId;
    private final String name;
    private final int npcId;
    private final List<Phase> phases;
    private final Map<String, Mechanic> sharedMechanics;

    // Runtime state
    private Phase currentPhase;
    private int phaseTick;           // ticks elapsed in current phase
    private int globalTick;          // total ticks since encounter start
    private Map<String, Integer> mechanicCooldowns;
    private Set<String> activeMechanicIds;
}

public class Phase {
    private final String phaseId;
    private final String name;
    private final List<TriggerEvaluator> entryTriggers;
    private final List<TriggerEvaluator> exitTriggers;
    private final List<Mechanic> mechanics;
    private boolean active = false;
}

public class Mechanic {
    private final String mechanicId;
    private final String name;
    private final List<TriggerEvaluator> triggers;
    private final List<CalloutDefinition> callouts;
    private final int cooldown;           // ticks
    private final boolean interruptible;
    private int cooldownRemaining;
    private boolean triggered;
}

public class CalloutDefinition {
    private final String calloutId;
    private final String text;
    private final String audioFile;      // .ogg filename in pack
    private final CalloutCategory category;
    private final VisualDefinition visual;
    private final int audioOffsetTicks;   // tick offset for audio
    private final int visualOffsetTicks;  // tick offset for visual
    private final int priority;           // 1-100
}

public enum CalloutCategory {
    CRITICAL, WARNING, INFO, TRANSITION
}

public class VisualDefinition {
    private final VisualType type;       // PRAYER_ICON, COUNTDOWN, TEXT, SAFE_TILE, etc.
    private final Color color;
    private final Point position;        // screen coordinates or overlay-relative
    private final float opacity;
    private final int durationTicks;
}
```

### Phase Machine

```java
public class PhaseMachine {
    private Phase currentPhase;

    public void advanceTick(List<TriggerResult> triggers) {
        // 1. Check exit conditions
        for (TriggerEvaluator exitTrigger : currentPhase.exitTriggers()) {
            if (triggerMatchesAny(exitTrigger, triggers)) {
                Phase nextPhase = determineNextPhase(currentPhase, triggers);
                if (nextPhase != null) {
                    transitionTo(nextPhase);
                    return;
                }
            }
        }

        // 2. Check entry conditions for current phase (if not yet active)
        if (!currentPhase.isActive()) {
            if (allTriggersMatch(currentPhase.entryTriggers(), triggers)) {
                currentPhase.setActive(true);
            }
        }
    }

    private void transitionTo(Phase nextPhase) {
        currentPhase.exit();
        nextPhase.enter();
        currentPhase = nextPhase;
        // Reset mechanic cooldowns
        encounter.resetMechanicCooldowns();
    }
}
```

### Condition Evaluator

```java
public class ConditionEvaluator {
    public boolean evaluate(ConditionDefinition condition, EncounterState state) {
        switch (condition.type()) {
            case "npc_hp_below":
                return state.getBossHp() < condition.threshold();
            case "player_hp_below":
                return state.getPlayerHp() < condition.threshold();
            case "tick_mod":
                return state.getPhaseTick() % condition.mod() == 0;
            case "prayer_active":
                return state.isPrayerActive(condition.prayer());
            case "player_in_region":
                return state.getPlayerPosition().isInRegion(condition.region());
            case "custom":
                return evaluateCustom(condition.expression(), state);
            default:
                throw new IllegalArgumentException("Unknown condition type");
        }
    }
}
```

---

## 7. Coaching Engine

### Core Class

```java
public class CoachingEngine {
    private final TriggerEngine triggerEngine;
    private final EncounterEngine encounterEngine;
    private final CalloutQueue calloutQueue;
    private final CooldownManager cooldownManager;
    private final PriorityResolver priorityResolver;
    private final PredictionEngine predictionEngine;
    private final List<CoachingListener> listeners;  // OverlayManager, AudioEngine

    public void onTick(int tick, List<GameEvent> events) {
        // 1. Check triggered mechanics
        List<MechanicActivation> activations = encounterEngine.checkMechanics(tick, events);

        for (MechanicActivation activation : activations) {
            List<CalloutRequest> callouts = activation.getCallouts();
            for (CalloutRequest callout : callouts) {
                // Apply config filters
                if (!isCalloutEnabled(callout)) continue;

                // Check cooldown
                if (cooldownManager.isOnCooldown(callout.id())) continue;

                // Schedule with offset
                CalloutRequest scheduled = new CalloutRequest(
                    callout,
                    tick + callout.visualOffsetTicks(),
                    tick + callout.audioOffsetTicks()
                );
                calloutQueue.enqueue(scheduled);

                cooldownManager.apply(callout.id(), callout.cooldown());
            }
        }

        // 2. Process scheduled callouts (due this tick)
        List<CalloutRequest> dueNow = calloutQueue.getDue(tick);
        for (CalloutRequest due : dueNow) {
            for (CoachingListener listener : listeners) {
                listener.onVisualCallout(due.visualRequest());
                listener.onAudioCallout(due.audioRequest());
            }
            cooldownManager.apply(due.callout().id(), due.callout().cooldown());
        }

        // 3. Generate prediction
        predictionEngine.generatePredictions(tick, encounterEngine.getState());
    }
}
```

### Callout Scheduler

```java
public class CalloutScheduler {
    private final PriorityQueue<ScheduledCallout> queue;

    public void schedule(CalloutDefinition callout, int visualTick, int audioTick) {
        queue.offer(new ScheduledCallout(callout, visualTick, audioTick));
    }

    public List<ScheduledCallout> getDue(int tick) {
        List<ScheduledCallout> due = new ArrayList<>();
        while (!queue.isEmpty() && queue.peek().visualTick() <= tick) {
            due.add(queue.poll());
        }
        return due;
    }
}
```

### Priority Resolver

```java
public class PriorityResolver {
    public int resolvePriority(CalloutDefinition callout) {
        int base = callout.priority();
        // Boost critical callouts
        if (callout.category() == CalloutCategory.CRITICAL) base += 30;
        // Reduce if on cooldown recently
        if (cooldownManager.isRecentlyFired(callout.id())) base -= 20;
        return base;
    }
}
```

### Callout Queue (with interruption)

```java
public class CalloutQueue {
    private final PriorityQueue<QueuedCallout> queue;

    public void enqueue(QueuedCallout callout) {
        // If a higher-priority callout is already queued, insert before it
        queue.offer(callout);
        // Sort by priority (higher first), then by arrival time
    }

    public Optional<QueuedCallout> poll() {
        return Optional.ofNullable(queue.poll());
    }

    public boolean canInterrupt(QueuedCallout current, QueuedCallout incoming) {
        return incoming.priority() > current.priority()
            && incoming.category() != current.category();
    }
}
```

### Prediction Engine

```java
public class PredictionEngine {
    public List<PredictedMechanic> predict(int currentTick, EncounterState state) {
        List<PredictedMechanic> predictions = new ArrayList<>();
        Phase currentPhase = state.getCurrentPhase();

        // Look ahead in the mechanism timeline
        for (Mechanic mechanic : currentPhase.mechanics()) {
            int nextFireTick = calculateNextFire(mechanic, currentTick, state);
            if (nextFireTick - currentTick <= 10) {  // within 10 ticks
                predictions.add(new PredictedMechanic(
                    mechanic.name(),
                    nextFireTick - currentTick,  // ticks until fire
                    mechanic.callouts()
                ));
            }
        }
        return predictions.stream()
            .sorted(Comparator.comparingInt(PredictedMechanic::ticksUntilFire))
            .collect(Collectors.toList());
    }
}
```

---

## 8. JSON Schema & Data Parsing

### Schema File

File: `src/main/resources/schemas/encounter_schema_v1.json`

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "Project Coach Encounter Pack Schema",
  "type": "object",
  "required": ["schemaVersion", "metadata", "bosses"],
  "properties": {
    "schemaVersion": { "type": "string", "pattern": "^\\d+\\.\\d+(\\.\\d+)?$" },
    "metadata": {
      "type": "object",
      "properties": {
        "packId": { "type": "string" },
        "name": { "type": "string" },
        "description": { "type": "string" },
        "author": { "type": "string" },
        "version": { "type": "string", "pattern": "^\\d+\\.\\d+\\.\\d+$" },
        "gameVersion": { "type": "string" }
      },
      "required": ["packId", "name", "version", "gameVersion"]
    },
    "bosses": {
      "type": "array",
      "items": { "$ref": "#/definitions/boss" },
      "minItems": 1
    },
    "triggers": {
      "type": "object",
      "additionalProperties": { "$ref": "#/definitions/trigger" }
    },
    "callouts": {
      "type": "object",
      "additionalProperties": { "$ref": "#/definitions/callout" }
    }
  },
  "definitions": {
    "boss": {
      "type": "object",
      "required": ["bossId", "name", "npcId", "phases"],
      "properties": {
        "bossId": { "type": "string" },
        "name": { "type": "string" },
        "npcId": { "type": "integer" },
        "description": { "type": "string" },
        "phases": {
          "type": "array",
          "items": { "$ref": "#/definitions/phase" },
          "minItems": 1
        },
        "mechanics": { "$ref": "#/definitions/mechanic_array" },
        "recovery": { "$ref": "#/definitions/recovery" }
      }
    },
    "phase": {
      "type": "object",
      "required": ["phaseId", "name", "entryTrigger", "exitTriggers"],
      "properties": {
        "phaseId": { "type": "string" },
        "name": { "type": "string" },
        "entryTrigger": { "$ref": "#/definitions/trigger_ref" },
        "exitTriggers": {
          "type": "array",
          "items": { "$ref": "#/definitions/trigger_ref" }
        },
        "mechanics": {
          "type": "array",
          "items": { "$ref": "#/definitions/mechanic_ref" }
        }
      }
    },
    "mechanic": {
      "type": "object",
      "required": ["mechanicId", "name", "triggers", "callouts"],
      "properties": {
        "mechanicId": { "type": "string" },
        "name": { "type": "string" },
        "triggers": {
          "type": "array",
          "items": { "$ref": "#/definitions/trigger_ref_or_def" }
        },
        "callouts": {
          "type": "array",
          "items": { "$ref": "#/definitions/callout_ref_or_def" }
        },
        "conditions": { "$ref": "#/definitions/condition_array" },
        "cooldown": { "type": "integer", "minimum": 0 },
        "interruptible": { "type": "boolean" }
      }
    },
    "trigger": {
      "type": "object",
      "required": ["triggerId", "type"],
      "properties": {
        "triggerId": { "type": "string" },
        "type": {
          "type": "string",
          "enum": ["animation", "projectile", "graphic", "npc_spawn",
                   "npc_despawn", "hp", "tick_timer", "player_state",
                   "location", "custom", "composite"]
        },
        "npcId": { "type": "integer" },
        "animationId": { "type": "integer" },
        "projectId": { "type": "integer" },
        "graphicId": { "type": "integer" },
        "hpThreshold": { "type": "integer" },
        "hpDirection": { "enum": ["below", "above"] },
        "tickMod": { "type": "integer", "minimum": 1 },
        "tickOffset": { "type": "integer" },
        "conditions": { "$ref": "#/definitions/condition_array" },
        "children": {  // for composite
          "type": "array",
          "items": { "$ref": "#/definitions/trigger_ref_or_def" }
        },
        "logic": { "enum": ["AND", "OR"] }
      }
    },
    "callout": {
      "type": "object",
      "required": ["calloutId", "text", "category"],
      "properties": {
        "calloutId": { "type": "string" },
        "text": { "type": "string" },
        "audioFile": { "type": "string" },
        "category": {
          "enum": ["critical", "warning", "info", "transition"]
        },
        "priority": { "type": "integer", "minimum": 1, "maximum": 100 },
        "audioOffset": { "type": "integer", "minimum": -5, "maximum": 10 },
        "visualOffset": { "type": "integer", "minimum": -5, "maximum": 10 },
        "visual": { "$ref": "#/definitions/visual" }
      }
    },
    "visual": {
      "type": "object",
      "properties": {
        "type": {
          "enum": ["prayer_icon", "countdown", "text", "safe_tile",
                   "status_bar", "timeline", "mini_hud"]
        },
        "color": { "type": "string", "pattern": "^#[0-9A-Fa-f]{6}$" },
        "position": { "$ref": "#/definitions/position" },
        "opacity": { "type": "number", "minimum": 0, "maximum": 1 },
        "durationTicks": { "type": "integer", "minimum": 1 }
      }
    },
    "condition": {
      "type": "object",
      "required": ["type"],
      "properties": {
        "type": {
          "enum": ["npc_hp_below", "npc_hp_above", "player_hp_below",
                   "player_hp_above", "tick_mod", "player_in_region",
                   "prayer_active", "prayer_inactive", "inventory_contains", "custom"]
        },
        "threshold": { "type": "integer" },
        "mod": { "type": "integer" },
        "region": { "type": "object",
                   "properties": { "minX": {"type":"integer"}, "maxX": {"type":"integer"},
                                   "minY": {"type":"integer"}, "maxY": {"type":"integer"} }
        },
        "prayer": { "type": "string" },
        "itemId": { "type": "integer" },
        "expression": { "type": "string" }
      }
    },
    "position": {
      "type": "object",
      "properties": {
        "anchor": { "enum": ["north_west", "north_east", "south_west", "south_east", "center", "custom"] },
        "x": { "type": "integer" },
        "y": { "type": "integer" }
      }
    },
    "recovery": {
      "type": "object",
      "properties": {
        "onPlayerDeath": { "enum": ["reset", "pause", "ignore"] },
        "onBossDespawn": { "enum": ["reset", "pause", "ignore"] },
        "resetPhase": { "type": "string" }
      }
    }
  }
}
```

### JSON Parser

File: `src/main/java/com/coach/plugin/encounter/json/JsonParser.java`

Uses RuneLite's bundled Gson for deserialization:

```java
public class JsonParser {
    private final Gson gson = new GsonBuilder()
        .registerTypeAdapter(TriggerDefinition.class, new TriggerDeserializer())
        .registerTypeAdapter(CalloutDefinition.class, new CalloutDeserializer())
        .create();

    public EncounterPack parse(String jsonContent) {
        JsonObject json = JsonParser.parse(jsonContent).getAsJsonObject();

        // Validate schema version
        String version = json.get("schemaVersion").getAsString();
        if (!isSupportedVersion(version)) {
            throw new UnsupportedVersionException("schema " + version);
        }

        // Validate against schema
        validate(json);

        // Deserialize
        return gson.fromJson(jsonContent, EncounterPack.class);
    }

    private void validate(JsonObject json) {
        // Lightweight schema validation in-plugin
        // Full JSON Schema validation is in the AI pipeline tool
        validateRequiredFields(json, "schemaVersion", "metadata", "bosses");
        for (JsonObject boss : json.getAsJsonArray("bosses")) {
            validateBoss(boss);
        }
    }
}
```

### File Loading Flow

```
PackManager.loadPacks(directory)
  for each .zip in directory:
    PackLoader.load(zip)
      → Extract encounter.json
      → Validate schema version
      → Parse JSON → Encounter objects
      → Pre-load all .ogg files into AudioEngine cache
      → Return EncounterPack
      → On error: log + quarantine (rename to .broken)
```

### Version Handling

```java
public class SchemaVersion {
    public static boolean isCompatible(String packVersion, String pluginVersion) {
        // Semantic versioning: major must match
        return getMajor(packVersion) == getMajor(pluginVersion);
    }

    public static EncounterPack migrate(EncounterPack old, String toVersion) {
        // Apply migrations if version < current
        // Migrations are simple JSON transformations
        if (getMajor(old.version()) < getMajor(toVersion)) {
            throw new UnsupportedVersionException("major version mismatch");
        }
        // Apply minor version migrations...
        return old;
    }
}
```

---

## 9. Overlay System

### Base Classes

File: `src/main/java/com/coach/plugin/overlay/CoachOverlay.java`

```java
public class CoachOverlay extends Overlay {
    private final OverlayManager overlayManager;
    private final CoachingEngine coachingEngine;
    private final EncounterEngine encounterEngine;

    public CoachOverlay(OverlayManager overlayManager,
                        CoachingEngine coachingEngine,
                        EncounterEngine encounterEngine) {
        setPosition(OverlayPosition.DYNAMIC);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        OverlayRenderMode mode = getRenderMode();  // from config

        // Render visual callouts
        for (VisualCalloutRequest request : coachingEngine.getActiveVisuals()) {
            renderCallout(graphics, request);
        }

        // Render active mechanics
        renderActiveMechanics(graphics);

        // Render prediction
        renderPredictions(graphics);

        return null;
    }
}
```

### Individual Overlay Renderers

**Prayer Indicator:**

```java
public class PrayerIndicatorRenderer {
    public void render(Graphics2D g, PrayerCallout callout, Rectangle bounds) {
        if (callout.prayer() == Prayer.PROTECT_MELEE) {
            drawPrayerIcon(g, PrayerIcon.MELEE, bounds);
        }
        drawFlashingText(g, "USE PROTECT MELEE", bounds, callout.priority());
    }
}
```

**Countdown:**

```java
public class CountdownRenderer {
    public void render(Graphics2D g, CountdownCallout callout) {
        int ticksRemaining = callout.targetTick() - getCurrentTick();
        if (ticksRemaining > 0 && ticksRemaining <= 5) {
            g.setColor(Color.YELLOW);
            g.setFont(new Font("Arial", Font.BOLD, 48));
            String text = String.valueOf(ticksRemaining);
            drawCenteredText(g, text);
        }
    }
}
```

**Safe Tile:**

```java
public class SafeTileRenderer {
    public void render(Graphics2D g, List<WorldPoint> safeTiles) {
        for (WorldPoint tile : safeTiles) {
            Polygon poly = Perspective.getCanvasTilePoly(client, tile);
            if (poly != null) {
                g.setColor(new Color(0, 255, 0, 80));  // semi-transparent green
                g.fill(poly);
                g.setColor(Color.GREEN);
                g.draw(poly);
            }
        }
    }
}
```

### Overlay Configuration

Each overlay type reads from RuneLite config:

```java
public enum OverlayType {
    PRAYER_ICON(config.prayerIndicatorEnabled(), Color.ORANGE, OverlayPosition.TOP_LEFT),
    COUNTDOWN(config.countdownEnabled(), Color.YELLOW, OverlayPosition.CENTER),
    SAFE_TILE(config.safeTileEnabled(), Color.GREEN, OverlayPosition.DYNAMIC),
    // ...
}
```

---

## 10. Audio Engine

### Core Class

File: `src/main/java/com/coach/plugin/audio/AudioEngine.java`

```java
public class AudioEngine {
    private final Map<String, ByteBuffer> audioCache = new ConcurrentHashMap<>();
    private final AudioQueue queue = new AudioQueue();
    private volatile int masterVolume = 70;
    private final Map<CalloutCategory, Integer> categoryVolumes = new EnumMap<>(CalloutCategory.class);

    public void loadAudioFromPack(EncounterPack pack) {
        for (String audioFile : pack.getAllAudioFiles()) {
            ByteBuffer buffer = loadOgg(pack.getFile(audioFile));
            audioCache.put(audioFile, buffer);
        }
    }

    public void playAudio(String audioFile, int tickOffsetMs) {
        ByteBuffer buffer = audioCache.get(audioFile);
        if (buffer == null) {
            log.warn("Audio file not found: {}", audioFile);
            return;
        }

        // Apply volume
        byte[] audioData = applyVolume(buffer, getEffectiveVolume(audioFile));

        // Play asynchronously
        CompletableFuture.runAsync(() -> {
            try (Clip clip = AudioSystem.getClip()) {
                clip.open(AudioFormat.OGG, audioData);
                FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                gain.setValue(convertToGain(getEffectiveVolume(audioFile)));
                clip.start();
            } catch (Exception e) {
                log.error("Failed to play audio: {}", audioFile, e);
            }
        });
    }

    private byte[] loadOgg(InputStream is) {
        // Use JOrbis or similar OGG decoder (bundled in RuneLite)
        // Returns raw PCM byte array
    }
}
```

### Audio Queue with Interruption

```java
public class AudioQueue {
    private final PriorityQueue<AudioRequest> queue = new PriorityQueue<>(
        Comparator.comparingInt(AudioRequest::priority).reversed()
    );
    private Optional<AudioRequest> currentlyPlaying = Optional.empty();

    public void enqueue(AudioRequest request) {
        if (currentlyPlaying.isPresent()
            && canInterrupt(currentlyPlaying.get(), request)) {
            interrupt(currentlyPlaying.get());
            currentlyPlaying = Optional.empty();
        }
        queue.offer(request);
    }

    public Optional<AudioRequest> poll() {
        return Optional.ofNullable(queue.poll());
    }

    private boolean canInterrupt(AudioRequest current, AudioRequest incoming) {
        return incoming.category().priority() > current.category().priority();
    }
}
```

### Volume Control

```java
public class VolumeController {
    private final Map<CalloutCategory, Float> categoryVolumes = Map.of(
        CRITICAL, 1.0f,
        WARNING, 0.8f,
        INFO, 0.6f,
        TRANSITION, 0.5f
    );

    public float getEffectiveVolume(String audioFile, CalloutCategory category) {
        float categoryVol = categoryVolumes.getOrDefault(category, 0.7f);
        return masterVolume / 100.0f * categoryVol;
    }
}
```

### Pre-loading Strategy

- All `.ogg` files in a pack are loaded into `ByteBuffer` at pack load time
- This ensures zero-latency playback during fights
- Memory budget: each .ogg is ~200-500KB; a 10-callout pack = ~5MB max
- If memory is tight, least-recently-used files are evicted (but this is rare)

---

## 11. Configuration Manager

### Config Structure

RuneLite config is organized into sections:

```
Global
  ├── Plugin Enabled
  ├── Debug Mode
  ├── Encounter Pack Directory
  ├── Log Level
  │
Audio
  ├── Master Volume
  ├── Critical Volume
  ├── Warning Volume
  ├── Info Volume
  ├── Transition Volume
  ├── Audio Offset (ms)
  ├── Mute All
  │
Visual
  ├── Overlay Opacity
  ├── Text Size
  ├── Colorblind Mode
  ├── Prayer Icon Scale
  ├── Countdown Size
  │
Boss Selection
  ├── Nex (enabled/disabled)
  ├── Inferno (enabled/disabled)
  ├── ... (per loaded boss)
  │
Accessibility
  ├── Audio-Only Mode
  ├── Visual-Only Mode
  ├── High Contrast Mode
  ├── Essential Only Mode
  │
Debug
  ├── Event Log Size
  ├── Show Trigger History
  ├── Show State Inspector
  ├── Export Debug Log
```

### Profile Management

```java
public class ProfileManager {
    private final Config config;

    public List<String> getProfileNames() {
        String profilesJson = config.profilesJson();  // stored as config string
        return gson.fromJson(profilesJson, new TypeToken<List<String>>(){}.getType());
    }

    public void saveProfile(String name) {
        // Serialize current config to Profile object
        // Store in profilesJson config item
    }

    public void loadProfile(String name) {
        // Deserialize profile, apply to config
        // Config changes are picked up by engines via listeners
    }

    public void exportProfile(String name, Path destination) {
        // Write profile JSON to file
    }

    public void importProfile(Path source) {
        // Read + validate profile JSON, add to profiles list
    }
}
```

### Config Change Listeners

Engines register listeners to react to config changes without restart:

```java
@ConfigListener("visualOpacity")
public void onVisualOpacityChanged(int newValue) {
    overlayManager.setGlobalOpacity(newValue / 100.0f);
}

@ConfigListener("audioMasterVolume")
public void onAudioMasterVolumeChanged(int newValue) {
    audioEngine.setMasterVolume(newValue);
}

@ConfigListener("debugMode")
public void onDebugModeChanged(boolean enabled) {
    if (enabled) overlayManager.add(debugOverlay);
    else overlayManager.remove(debugOverlay);
}
```

---

## 12. Boss Development Workflow

This section guides **pack authors** through creating a new encounter pack. Follow these steps in order.

### Step 1: Create the Pack Directory

```
<encounters_dir>/myboss.pack/
├── encounter.json
└── audio/
```

### Step 2: Define Boss Metadata

```json
{
  "schemaVersion": "1.0",
  "metadata": {
    "packId": "myboss",
    "name": "My Custom Boss",
    "description": "A custom boss for testing",
    "author": "YourName",
    "version": "1.0.0",
    "gameVersion": "2024-08-01"
  },
  "bosses": [
    {
      "bossId": "myboss",
      "name": "My Custom Boss",
      "npcId": 13541,
      "phases": [
        {
          "phaseId": "phase_1",
          "name": "Phase 1",
          "entryTrigger": { "type": "npc_spawn", "npcId": 13541 },
          "exitTriggers": [
            { "type": "hp", "npcId": 13541, "hpThreshold": 50, "hpDirection": "below" }
          ],
          "mechanics": [
            {
              "mechanicId": "special_attack",
              "name": "Special Attack",
              "triggers": [
                { "triggerId": "special_anim", "type": "animation",
                  "npcId": 13541, "animationId": 8960 }
              ],
              "callouts": [
                {
                  "calloutId": "pray_ranged",
                  "text": "Pray Ranged!",
                  "audioFile": "pray_ranged.ogg",
                  "category": "critical",
                  "priority": 90,
                  "audioOffset": -2,
                  "visualOffset": 0,
                  "visual": {
                    "type": "prayer_icon",
                    "color": "#FF0000",
                    "opacity": 0.9
                  }
                }
              ],
              "cooldown": 10
            }
          ]
        }
      ]
    }
  ]
}
```

### Step 3: Generate Audio Files

Use the AI Knowledge Pipeline's `audio_generator.py` to create `.ogg` files:

```bash
python audio_generator.py --callout-text "Pray Ranged!" --output audio/pray_ranged.ogg --voice "friendly_male"
```

Or manually: use Edge TTS or Kokoro to generate the audio:

```bash
# Using edge-tts (Node.js)
npx edge-tts --text "Pray Ranged!" --output-file audio/pray_ranged.ogg --voice en-US-GuyNeural
```

### Step 4: Package and Test

```bash
# Package
cd myboss.pack && zip -r ../myboss_1.0.0.zip .

# Place in RuneLite encounters directory
cp ../myboss_1.0.0.zip <runelite>/coach/encounters/

# Start RuneLite, verify pack loads in debug overlay
```

### Step 5: Validate and Iterate

- Check the debug overlay: does the encounter load?
- Fight the boss (or use a test world): do callouts fire?
- Check timing: are calls tick-accurate?
- Use debug logging: are triggers firing correctly?

### Step 6: Publish

- Upload to the community knowledge repository
- Include a changelog
- Include the JSON schema version for compatibility tracking

---

## 13. AI Knowledge Pipeline

The AI Knowledge Pipeline is a **separate Python tool** (`knowledge-pipeline/`) that generates encounter JSON packs from the OSRS Wiki. It runs on developer machines, not in the plugin.

### Architecture

```
OSRS Wiki Page (HTML)
        │
        ▼
  Wiki Fetcher  →  Wiki Parser  →  LLM Prompter  →  JSON Generator
        │
        ▼
Draft Encounter JSON
        │
        ▼
Schema Validator + Logic Validator + AI Validator Agent
        │
        ▼
Validation Report (issues + confidence)
        │
        ▼
Human Review Interface (web UI)
        │
        ▼
Approved JSON
        │
        ▼
Audio Generator (TTS → .ogg)
        │
        ▼
Pack Builder (.zip with JSON + audio)
        │
        ▼
Final Encounter Pack
```

### Component Implementations

#### Wiki Fetcher

```python
import requests
from bs4 import BeautifulSoup

class WikiFetcher:
    BASE_URL = "https://oldschool.wiki"

    def fetch(self, boss_name: str) -> str:
        url = f"{self.BASE_URL}/w/{boss_name.replace(' ', '_')}"
        resp = requests.get(url, headers={"User-Agent": "ProjectCoach/1.0"})
        resp.raise_for_status()
        # Remove navboxes, ads, scripts
        soup = BeautifulSoup(resp.text, "html.parser")
        # Keep only the main content
        content = soup.find("div", {"id": "mw-content-text"})
        return str(content)
```

#### Wiki Parser

```python
from dataclasses import dataclass
from typing import List

@dataclass
class ExtractedMechanic:
    name: str
    description: str
    timing_notes: str | None
    animation_id: int | None
    projectile_id: int | None
    damage: int | None
    special: bool = False

@dataclass
class ExtractedPhase:
    name: str
    hp_threshold: int | None
    mechanics: List[ExtractedMechanic]

class WikiParser:
    def parse(self, html: str) -> tuple[str, List[ExtractedPhase]]:
        soup = BeautifulSoup(html, "html.parser")
        boss_name = soup.find("h1", class_="firstHeading").text

        phases = self._extract_phases(soup)
        mechanics = self._extract_mechanics(soup)

        return boss_name, self._group_into_phases(phases, mechanics)
```

#### LLM Prompter

System prompt (stored in `prompts/system_prompt.txt`):

```
You are generating a RuneLite encounter definition JSON for a boss coaching plugin.
The user will provide extracted wiki text. Your job is to identify all boss mechanics,
phase transitions, special attacks, timing information, and map them to the JSON schema.

Key principles:
1. Every mechanic must have at least one trigger (animation, projectile, hp, etc.)
2. Callouts that are critical (require player action) must have audioOffset of -2 (2 ticks before)
3. Use tick-based timing (1 tick = 600ms in OSRS)
4. Phase transitions should have clear entry/exit triggers
5. If you cannot determine a trigger ID, mark it as "PENDING_REVIEW"

Output ONLY valid JSON. Do not include explanations.
```

```python
import openai

class LLMPrompter:
    def __init__(self, api_key: str, model: str = "gpt-4o"):
        self.client = openai.OpenAI(api_key=api_key)
        self.system_prompt = load_prompt("system_prompt.txt")

    def generate(self, wiki_data: tuple[str, List[ExtractedPhase]]) -> str:
        user_prompt = f"""
Boss: {wiki_data[0]}
Extracted Phases and Mechanics:
{format_phases_for_llm(wiki_data[1])}

Schema: {load_schema()}

Generate a complete encounter JSON for this boss. Return ONLY the JSON.
"""
        response = self.client.chat.completions.create(
            model=self.model,
            messages=[
                {"role": "system", "content": self.system_prompt},
                {"role": "user", "content": user_prompt}
            ],
            response_format={"type": "json_object"}
        )
        return response.choices[0].message.content
```

#### AI Validator Agent

The AI validator agent is a lightweight autonomous agent (can be built with the same patterns as an AI coding assistant like opencode) that:

1. Reads the draft JSON + validation report
2. Identifies issues (missing audio files, impossible triggers, unreachable phases)
3. Makes edits to fix issues (e.g., setting `PENDING_REVIEW` triggers to placeholders)
4. Produces a "fix report" for the human reviewer

```python
class AIValidatorAgent:
    def validate(self, draft_json: dict, pack: EncounterPack) -> ValidationReport:
        issues = []
        # Check: every trigger type is valid
        issues.extend(self._check_trigger_types(draft_json))
        # Check: every audio file exists in pack
        issues.extend(self._check_audio_files(draft_json, pack))
        # Check: phase graph is connected (no unreachable phases)
        issues.extend(self._check_phase_graph(draft_json))
        # Check: tick offsets are in valid range
        issues.extend(self._check_timing(draft_json))

        # For each issue, try to auto-fix
        for issue in issues:
            if issue.can_auto_fix:
                self._auto_fix(issue, draft_json)
                issue.status = "fixed"
            else:
                issue.status = "needs_human_review"

        return ValidationReport(issues)
```

#### Audio Generator

```python
import edge_tts
import asyncio
from pydub import AudioSegment

class AudioGenerator:
    async def generate_callout(self, text: str, output_path: str, voice: str = "en-US-GuyNeural"):
        communicate = edge_tts.Communicate(text, voice)
        await communicate.save(output_path)
        # Convert to .ogg
        sound = AudioSegment.from_wav(output_path + ".wav")
        sound.export(output_path + ".ogg", format="ogg")
        os.remove(output_path + ".wav")
```

#### Pack Builder

```python
import zipfile
import json

class PackBuilder:
    def build(self, json_path: str, audio_dir: str, output_zip: str):
        with zipfile.ZipFile(output_zip, 'w', zipfile.ZIP_DEFLATED) as zf:
            zf.write(json_path, "encounter.json")
            for audio_file in os.listdir(audio_dir):
                zf.write(os.path.join(audio_dir, audio_file),
                         f"audio/{audio_file}")
```

---

## 14. Testing Strategy

### Unit Testing

JUnit 5 + Mockito for all plugin components:

```java
class TriggerEngineTest {
    @Mock private Client client;
    @Mock private NPC npc;
    @Mock private Player player;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Mock basic client state
        when(client.getLocalPlayer()).thenReturn(player);
        when(player.getAnimation()).thenReturn(-1);
    }

    @Test
    void testAnimationTriggerFires() {
        // Arrange
        AnimationTriggerEvaluator trigger = new AnimationTriggerEvaluator("test", 13541, 8960);
        AnimationChanged event = new AnimationChanged(npc);
        when(npc.getId()).thenReturn(13541);
        when(npc.getAnimation()).thenReturn(8960);

        // Act
        boolean result = trigger.evaluate(toGameEvent(event), mockState());

        // Assert
        assertTrue(result);
    }

    @Test
    void testAnimationTriggerDoesNotFireForWrongNPC() {
        AnimationTriggerEvaluator trigger = new AnimationTriggerEvaluator("test", 13541, 8960);
        AnimationChanged event = new AnimationChanged(mockNpc(13540, 8960));

        boolean result = trigger.evaluate(toGameEvent(event), mockState());
        assertFalse(result);
    }
}
```

### Integration Testing

Tests the full flow: game event → trigger → encounter → callout:

```java
class EndToEndTest {
    @Test
    void testPrayerCalloutFlow() {
        // 1. Load a test encounter pack
        EncounterEngine engine = loadTestPack("test_boss.json");

        // 2. Simulate game events
        List<GameEvent> events = List.of(
            new GameEvent(EventType.NPC_SPAWNED, mockNpc(13541, 0)),
            new GameEvent(EventType.ANIMATION_CHANGED, mockAnim(13541, 8960))
        );

        // 3. Run one tick
        engine.onTick(100, events);

        // 4. Verify a callout was scheduled
        verify(audioEngine).playAudio(eq("pray_ranged.ogg"), anyInt());
        verify(overlayManager).addPrayerIcon(any());
    }
}
```

### Boss Simulation Testing

Pre-recorded fight data is replayed through the engine:

```java
class NexSimulationTest {
    @Test
    void testZarosPhaseCallouts() {
        // Load replay data (tick-by-tick events from a real Nex kill)
        List<TickEvents> replay = loadReplay("nex_kill_001.json");

        EncounterEngine engine = loadPack("nex_1.0.0.zip");
        CalloutRecorder recorder = new CalloutRecorder();

        for (TickEvents tick : replay) {
            engine.onTick(tick.tickNumber(), tick.events());
        }

        // Verify expected callouts fired on correct ticks
        assertCalloutFired(recorder, "pray_melee", 450);
        assertCalloutFired(recorder, "pray_ranged", 452);
        assertCalloutFired(recorder, "phase_transition", 620);
    }
}
```

### Replay Test Data Format

```json
{
  "boss": "Nex",
  "player": "TestPlayer",
  "ticks": [
    {
      "tick": 448,
      "events": [
        {"type": "animation", "npcId": 3133, "animId": 3975}
      ]
    },
    {
      "tick": 449,
      "events": []
    },
    {
      "tick": 450,
      "events": [
        {"type": "projectile", "projectId": 2955, "srcNpc": 3133}
      ]
    }
  ],
  "expected_callouts": [
    {"tick": 448, "callout": "pray_ranged"},
    {"tick": 450, "callout": "eat_food"}
  ]
}
```

### Test File Organization

```
tests/
├── unit/
│   ├── TriggerEngineTest.java
│   ├── EncounterEngineTest.java
│   ├── CoachingEngineTest.java
│   ├── ConditionEvaluatorTest.java
│   ├── AudioQueueTest.java
│   └── JsonParserTest.java
├── integration/
│   ├── EndToEndTest.java
│   ├── OverlayRenderingTest.java
│   └── PackLoadingTest.java
├── simulation/
│   ├── NexSimulationTest.java
│   ├── InfernoSimulationTest.java
│   └── ToBSimulationTest.java
└── resources/
    ├── test_packs/
    │   ├── valid_pack.zip
    │   ├── invalid_missing_fields.zip
    │   └── invalid_bad_trigger.zip
    └── simulations/
        ├── nex_kill_001.json
        ├── nex_kill_002.json
        └── inferno_waves_1_10.json
```

### Continuous Integration

GitHub Actions workflow (`.github/workflows/test.yml`):

```yaml
name: Tests
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v3
        with: { distribution: 'temurin', java-version: '11' }
      - uses: actions/setup-gradle@v3
      - run: gradle test
      - run: gradle jacocoTestReport
      - run: bash scripts/check_coverage.sh  # fails if < 80%
```

---

## 15. Release Process

### Versioning

- **Plugin version**: Semantic versioning (`1.0.0`, `1.0.1`, `1.1.0`)
- **Schema version**: Matches plugin major version (`1.0` for plugin `1.x.x`)
- **Pack version**: Independent per pack (`nex-1.2.0.zip`)

### Build Pipeline

```gradle
task buildRelease(type: Jar) {
    archiveClassifier = 'release'
    from sourceSets.main.output
    dependsOn configurations.runtimeClasspath

    // ProGuard optimization
    minify {
        def mapping = [
            'proguard': new ProGuard(
                keep: ['com.coach.plugin.**'],
                optimize: true,
                obfuscate: false
            )
        ]
    }

    // Sign the JAR
    doLast {
        signJar(archiveFile.get().asFile, privateKey, passphrase)
    }
}
```

### Artifact Generation

```
build/
├── libs/
│   └── coach-1.0.0-release.jar    # Release JAR (signed)
├── distributions/
│   └── coach-1.0.0.zip            # Plugin hub package
└── reports/
    └── changelog.txt              # Generated from git log
```

### Plugin Packaging for Hub

RuneLite plugin hub requires:
1. Signed JAR (RSA signature)
2. `runelite-plugin.properties` with metadata
3. README.md for the hub listing
4. No banned APIs (reflection, network, file I/O outside config dir)

Checklist:
- [ ] JAR builds without warnings
- [ ] ProGuard rules preserve all necessary classes
- [ ] Plugin hub manifest metadata is correct
- [ ] JAR size < 5 MB
- [ ] No banned API calls detected
- [ ] Changelog documents all changes

### Release Checklist

| Step | Status |
|------|--------|
| Run full test suite (`gradle test`) | ☐ |
| Run integration tests | ☐ |
| Run boss simulation tests | ☐ |
| Check coverage ≥ 80% | ☐ |
| Run ProGuard build | ☐ |
| Verify JAR size < 5 MB | ☐ |
| Sign JAR | ☐ |
| Update changelog | ☐ |
| Update documentation to match release | ☐ |
| Tag git release | ☐ |
| Submit to RuneLite plugin hub | ☐ |
| Announce in OSRS Discord communities | ☐ |

---

## 16. Future Modules

### Replay Analyzer

**Module name**: `coach-replay`
**Status**: Post-MVP
**Purpose**: Record fight events and replay them through the coaching engine for post-fight analysis.

**Planned additions**:
- Event recorder: hook into EventBus, write all events to a JSON log file
- Replay loader: read log, replay at configurable speed (0.1x - 4x)
- Analysis report: auto-detect missed callouts, late reactions, HP waste
- "What-if" mode: replay with different callout settings
- Performance grading: assign a score based on reaction timing + callout accuracy

**Files to create**:
- `src/main/java/com/coach/plugin/replay/EventRecorder.java`
- `src/main/java/com/coach/plugin/replay/ReplayLoader.java`
- `src/main/java/com/coach/plugin/replay/ReplayAnalyzer.java`
- `src/main/java/com/coach/plugin/replay/ReplayOverlay.java`

### Encounter Recorder

**Module name**: `coach-recorder`
**Status**: Post-MVP
**Purpose**: Record gameplay + coaching output for creating new encounter packs.

**Planned additions**:
- Record phase: captures all game events + coach decisions
- Export: save as a "training scenario" JSON (includes expected callout timings)
- Scenario player: replay a recorded scenario for testing new encounters

### Community Knowledge Repository

**Module name**: `knowledge-repo`
**Status**: Post-MVP
**Purpose**: Web platform for pack authors to publish + players to discover packs.

**Planned additions**:
- Pack upload (with schema validation)
- Pack search + rating system
- Version tracking + changelog
- Download analytics (opt-in)

### AI Voice Coach

**Module name**: `coach-voice`
**Status**: Post-MVP
**Purpose**: Dynamic voice generation (not just pre-recorded).

**Planned additions**:
- Runtime TTS (Edge TTS or Kokoro) for dynamic callouts
- Personality variants (chill friend, intense coach, analytical mentor)
- Context-aware volume (raise during chaos)
- Voice pack marketplace

### Visual Timeline Editor

**Module name**: `coach-editor`
**Status**: Post-MVP
**Purpose**: GUI tool for building encounter JSON without manual JSON editing.

**Planned additions**:
- Drag-drop phase builder
- Trigger wizard (select NPC + animation from a dropdown)
- Callout editor (preview audio + visual simultaneously)
- Timeline view (mechanics placed on a tick grid)
- Export to encounter pack ZIP

### Encounter Definition Designer

**Module name**: `coach-designer`
**Status**: Future
**Purpose**: Full WYSIWYG encounter pack creator.

**Planned additions**:
- Visual phase editor (drag NPC states, set HP thresholds)
- Trigger library (browse known animation/projectile IDs)
- Simulation mode (test encounter logic without RuneLite)
- Pack publishing workflow (export + upload to community repo)

### Multi-Game Adapter Layer

**Module name**: `coach-multi-game`
**Status**: Future
**Purpose**: Extend the coaching engine to other game clients.

**Planned additions**:
- Abstract `GameAdapter` interface (event source + state provider)
- RuneLite adapter (existing implementation)
- Future adapters: Blizzard Battle.net, Final Fantasy XIV, etc.
- Shared core engines (Trigger, Encounter, Coaching) unchanged

### Cloud Synchronization (opt-in)

**Module name**: `coach-cloud-sync`
**Status**: Future
**Purpose**: Sync encounter packs + profiles across devices.

**Planned additions**:
- End-to-end encryption (user provides encryption key)
- Conflict resolution (merge packs + profiles from multiple devices)
- Opt-in analytics (which bosses, which packs, crash reports)

### Plugin Marketplace Integration

**Module name**: `coach-marketplace`
**Status**: Future
**Purpose**: In-plugin pack browser.

**Planned additions**:
- Browse available packs from the community repo
- One-click download + install
- Automatic update notifications
- Rating + review system

---

*End of Implementation Guide.*
