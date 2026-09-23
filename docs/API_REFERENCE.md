# Project Coach — API Reference

Class-level reference for the RuneLite plugin under
`plugin/src/main/java/com/coach/plugin/`.

**Stability:** pack JSON (`encounter.model.*`) and settings (`CoachConfig`)
are the primary contracts. Engine packages are public for tests and the
Sprint 30b replay CLI; treat deep internals as evolving unless noted.

Nested helper types are listed only under their owning class.

---

## 1. Package map

| Package | Role |
|---------|------|
| `com.coach.plugin` | RuneLite entry point (`CoachPlugin`) |
| `.accessibility` | Modes, contrast palette, text scale |
| `.audio` | Clip cache, priority/interrupt, categories |
| `.coaching` | Callout scheduling, queue, prediction, cooldowns |
| `.config` | Settings, filters/gates, profiles |
| `.debug` | Debug overlay views, history, export |
| `.encounter` | Pack load/validate, phases, mechanics, sessions |
| `.encounter.model` | Gson POJOs — 1:1 JSON schema contract |
| `.events` | Internal bus + `GameEvent` / `EventType` |
| `.logging` | Ring buffer + loggers |
| `.model` | Runtime snapshots (`PlayerState`, `BossState`) |
| `.overlay` | Coach overlay + sub-renderers |
| `.performance` | Profiler, memory, tick replay / FightScript |
| `.trigger` | Trigger engine, registry, evaluators |

---

## 2. Entry point

### `CoachPlugin` (`com.coach.plugin`)

RuneLite `@PluginDescriptor(name = "Coach")` plugin.

| Member | Purpose |
|--------|---------|
| `startUp` / `shutDown` | Wire/tear down bus, engines, overlay, packs |
| `onGameTick` | Advance one coach tick (batch → triggers → encounter → coaching) |
| `onAnimationChanged`, `onProjectileMoved`, `onGraphicChanged`, `onNpcSpawned`, `onNpcDespawned`, `onChatMessage`, … | `@Subscribe` → internal `GameEvent`s |
| `onStatChanged`, `onVarbitChanged`, `onItemContainerChanged` | Stats/varbits/items → bus |
| `onConfigChanged` | Reapply audio/accessibility; reload packs if directory changed |
| `reloadPacks(reason)` | Rescan pack directory |

Lifecycle methods are RuneLite overrides (protected); not a stable external API.

---

## 3. `encounter` — packs, phases, mechanics

| Class | Key API |
|-------|---------|
| **`EncounterEngine`** | `loadPacks(dir)`, `setPacks`, `onTriggersFired`, `onTickBatch`, `getPackStatuses`, `getCurrentPhaseId`, `getActiveSessions` |
| **`EncounterLoader`** | `parseJson(content, sourceName)`, `loadZip(path)` — migrate → parse → validate → audio refs |
| **`PackManager`** | `loadDirectory(dir)` → packs + `PackStatus` list; conflict rules (first wins) |
| **`SchemaValidator`** | `validate(pack)` → all rule violations (empty = valid) |
| **`SchemaMigrations`** | `bringToCurrent(root, sourceName)` → schema `"1.0"` |
| **`PhaseMachine`** | `enterPhase`, `advanceIfExit`, `isFinalPhase` |
| **`ConditionEvaluator`** | `satisfies(condition, phaseTick)` — unknown types fail closed (false + warn) |
| **`MechanicManager`** | `tryActivate` (cooldown), `wasTriggered` |
| **`ActiveEncounter`** | Session: phase id/tick, global tick, cooldown helpers |
| **`RecoveryHandler`** | `shouldReset(event, trackedNpcIds)` on death/despawn |
| **`PackStatus`** | `describe()` → `file -> packId@version [LOADED\|REJECTED\|CONFLICT]` |
| **`PackLoadException`** | Carries aggregated validation message |

### `encounter.model` (JSON contract)

Field-only Gson POJOs — no behavior. Constant:
`EncounterPack.SUPPORTED_SCHEMA_VERSION = "1.0"`.

| Type | Notable fields |
|------|----------------|
| **`EncounterPack`** | `schemaVersion`, `metadata`, `bosses[]` |
| **`PackMetadata`** | `packId`, `name`, `version`, `gameVersion`, `description?`, `author?`, `dependencies?` |
| **`BossDefinition`** | `bossId`, `name`, `npcId`, `phases[]`, `mechanics?`, `recovery?` |
| **`PhaseDefinition`** | `phaseId`, `name`, `entryTrigger`, `exitTriggers?`, `mechanics?` |
| **`MechanicDefinition`** | `mechanicId`, `name`, `triggers[]`, `callouts?`, `conditions?`, `cooldown?` |
| **`CalloutDefinition`** | `calloutId`, `text`, `category`, `audioFile?`, `priority?`, `audioOffset?`, `visualOffset?`, `visual?` |
| **`TriggerDefinition`** | `type` + type fields (see ENCODING §3); `children?`, `logic?` for composite |
| **`ConditionDefinition`** | `type` + fields (`npcId`, `threshold`, `mod`, bounds, …) |
| **`VisualDefinition`** | `type?`, `color?`, `opacity?`, `durationTicks?` |

Full machine schema:
`plugin/src/main/resources/schemas/encounter_schema_v1.json`.

---

## 4. `trigger`

| Class | Key API |
|-------|---------|
| **`TriggerEvaluator`** (interface) | `interestedIn()`, `matches(GameEvent)`, `describe()` |
| **`TriggerEngine`** | `rebuild(packs)`, `onTickBatch(tick, events)`, `addFireListener`, `getLastFires`, `getMatchesCallsLastBatch` |
| **`TriggerRegistry`** | `create(TriggerDefinition)` → evaluator or empty |
| **`TriggerFire`** | Immutable fire record (tick, boss, context, description) |
| **`EdgeDetector`** | `onNext(boolean)` rising-edge only; `reset()` |

### Evaluators (all implement `TriggerEvaluator`)

`AnimationTriggerEvaluator`, `ProjectileTriggerEvaluator`,
`GraphicTriggerEvaluator`, `NpcSpawnTriggerEvaluator`,
`HpTriggerEvaluator`, `TickTimerTriggerEvaluator`,
`PlayerStateTriggerEvaluator`, `LocationTriggerEvaluator`,
`ShoutTriggerEvaluator`, `WaveClearedEvaluator`,
`CompositeTriggerEvaluator` (static `parseLogic`: `AND`/`OR`/null).

---

## 5. `events`

| Class | Key API |
|-------|---------|
| **`EventBus`** | `subscribe`/`unsubscribe`, `post`, `pendingCount`; nested `Listener` |
| **`GameEvent`** | Final: `getType()`, `getTick()`, `getPayload()` — construct with `new GameEvent(type, tick, payload)` |
| **`EventType`** | `TICK`, `ANIMATION_CHANGED`, `PROJECTILE_MOVED`, `GRAPHIC_CHANGED`, `GRAPHICS_OBJECT_CREATED`, `NPC_SPAWNED`, `NPC_DESPAWNED`, `PLAYER_STATS_CHANGED`, `VARBIT_CHANGED`, `ITEM_CONTAINER_CHANGED`, `CHAT_MESSAGE` |
| **`GameStateBridge`** | `getPlayerState(client)`, `getBossState(npc)`, `isBoss`, static `findNpc` |

---

## 6. `coaching`

| Class | Key API |
|-------|---------|
| **`CoachingEngine`** | `onActivation`, `onTick`, `addListener`, `setEnabledFilter`; nested `Listener`, `DeliveredCallout` |
| **`CalloutScheduler`** | `schedule(...)` — due tick from offsets → queue |
| **`CalloutQueue`** | `enqueue`, `drainDue(tick)` (priority order), `clear` |
| **`PriorityResolver`** | `resolve(callout)` — explicit priority or category default |
| **`PredictionEngine`** | `predict(sessions, currentTick)` upcoming countdowns |
| **`CooldownManager`** | `isOnCooldown`, `apply` |
| **`CoachStateManager`** | `update(bridge, client, tick)` → cached `PlayerState` |

---

## 7. `config`

| Class | Key API |
|-------|---------|
| **`CoachConfig`** | RuneLite `@ConfigGroup("coach")` — all user settings (see USER_GUIDE §3). Nested `DebugTab`: `EVENTS`, `TRIGGERS`, `STATE`, `TIMELINE`, `PROFILING` |
| **`CalloutGate`** | `test(bossId, callout)`, `disabledBosses()` — live predicate |
| **`CalloutFilter`** | Static `isEnabled(...)`, `categoryEnabled`, `disabledBosses` |
| **`AccessibilityMode`** | `BOTH`, `AUDIO_ONLY`, `VISUAL_ONLY` |
| **`ProfileManager`** | `listProfiles`, `saveProfile`, `applyProfile`, `deleteProfile`, `importProfile`, `ensureDefaultProfiles` |
| **`ProfileStorage` / `ProfileImporter` / `ProfileExporter`** | JSON file I/O for named profiles |
| **`ConfigProfile`** | Serializable settings snapshot fields |

---

## 8. `audio`

| Class | Key API |
|-------|---------|
| **`AudioEngine`** | `play(packId, file, category)`, `loadFromZip`, `setMasterVolume`, `setCategoryVolume`, `setMuted`, `resetQueue`, `clear` — never throws; missing clip → visual-only |
| **`AudioInterruptManager`** | `submit(category, starter)`, `onPlaybackFinished`, `reset` |
| **`AudioPriorityResolver`** | `resolve(category)` |
| **`AudioCategory`** | `fromCalloutCategory(String)` |

---

## 9. `overlay`

| Class | Key API |
|-------|---------|
| **`CoachOverlay`** | `render(Graphics2D)` composite |
| **`OverlayManager`** | `addVisual`, `prune(tick)`, `setPredictions`, `getActiveVisuals`, `noteCriticalDelivered`, `isQuiet`, boss/phase labels; nested `ActiveVisual` |
| Renderers | Static `render(...)` → `OverlayLine`: `TimelineRenderer`, `CountdownRenderer`, `MiniHudRenderer`, `PrayerIndicatorRenderer`, `SafeTileRenderer`, `StatusIndicatorRenderer` |

---

## 10. `accessibility`

| Class | Key API |
|-------|---------|
| **`AccessibilityManager`** | `getMode`, `isAudioEnabled`, `isVisualEnabled`; nested `Mode` |
| **`ColorPalette`** | `colorFor(category, highContrast)` |
| **`TextScaler`** | `factor`, `scale` from config percent |

---

## 11. `debug` / `logging` / `performance` / `model`

| Class | Key API |
|-------|---------|
| **`DebugOverlayV2`** | `render(Graphics2D)` — tabs via `CoachConfig.DebugTab` |
| **`StateInspector`** | `update`, `format`, `toExportData` |
| **`TriggerHistory`** | `record`, `filter`, `format`, `clear`; `MAX_ENTRIES = 200` |
| **`EventTimeline`** | `recordTick`, `recent`, `format`, `clear`; `MAX_TICKS = 120` |
| **`LogExporter`** | `export(...)` → debug JSON bundle path or null |
| **`LogBuffer`** | `log`, `snapshot`, `setFileWriter`, `clear`; ring |
| **`FileLogWriter`** | `write`, `close` |
| **`EventLogger` / `CalloutLogger` / `TriggerLogger`** | Bus/history formatting into `LogBuffer` |
| **`TickReplayHarness`** | `replay`, `replayScript(FightScript)`, `fromJson`, `fromDirectory` — headless pipeline |
| **`Profiler`** | `beginTick`/`endTick`, `start`/`stop` per `Component` |
| **`MemoryMonitor`** | `sample`, `usedBytes`, `reset` |
| **`FightScript` / `FightScriptLoader`** | Gson fight-script JSON for replays |
| **`PlayerState` / `BossState`** | Immutable snapshots (HP, pos, animation, …) |

**Internal-leaning (ok for tests/CLI; not a stable product API):**
`performance.*`, `logging.*`, `debug.*`, `CoachPlugin` handlers,
`CoachStateManager`, `EdgeDetector`, `RecoveryHandler`, `MechanicActivation`,
`TriggerFire`, `CalloutRequest`, `PredictedMechanic`, `ActiveEncounter`,
`OverlayLine`, `model.*`.

---

## 12. Related docs

- Architecture: [`master-architecture.md`](master-architecture.md)
- Class specs / schema prose: [`implementation-guide.md`](implementation-guide.md)
- Pack fields: [`examples/ENCODING.md`](examples/ENCODING.md)
- Player settings: [`USER_GUIDE.md`](USER_GUIDE.md)
- Build/test: [`DEVELOPER_SETUP.md`](DEVELOPER_SETUP.md)
