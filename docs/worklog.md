# Project Coach — Worklog

Running log of sprints: what was done, key decisions, deviations from the docs.

---

## Sprint 22 — Profile Management (2026-08-26)

**Objective:** Profile export/import as JSON files, validation before load,
and the three default presets. Phase 4 complete.

### Done

- **`ProfileStorage`** — file I/O for profile JSONs: pretty-printed writes
  with parent-dir creation; reads validate structure (missing `name` rejected,
  corrupt JSON carries the parse error, out-of-range masterVolume clamped).
- **`ProfileExporter`** — writes named profiles to
  `coach/profiles/<name>.json` (filename sanitised); unknown names logged.
- **`ProfileImporter`** — validates then imports via ProfileManager;
  invalid files never touch stored config (tested).
- **Default presets seeded once per installation**: Learning (all callouts),
  Practice (criticals only), Performance (silent). Guarded by the hidden
  `defaultsSeeded` flag so user deletions are respected — deleting all three
  does not resurrect them.
- Plugin seeds defaults at startUp.
- Tests: storage roundtrip/corruption/missing-name, export→import through real
  files into a fresh manager, unknown-export null, invalid-import rejection,
  volume clamping, default seeding semantics (×2).

### Verified

- Tests: **186/186 pass** (+9 ProfileFileLifecycleTest).

### Decisions

- Import validates **before** touching stored config — a bad file can never
  clobber existing profiles.
- Defaults tracked with an explicit seeded flag rather than "profiles empty"
  heuristics, so wiping profiles is a supported user action.
- Export filenames sanitise non-[A-Za-z0-9_-] characters to underscores.

### Deviations from docs

- Roadmap's UI buttons deferred again (no Swing panel yet) — but the full
  logic layer is complete and tested; wiring buttons later is trivial.

---

## Sprint 21 — Debug Tools v2 (2026-08-26)

**Objective:** Tabbed debug overlay with live state inspection, trigger
history, per-tick event timeline, and JSON log export.

### Done

- **`TriggerHistory`** — 200-entry ring of trigger fires, newest-first,
  filterable by boss/context/description substring.
- **`EventTimeline`** — per-tick aggregation: event counts by type + trigger
  and callout totals; merges late-arriving meta (fires/callouts land after the
  tick's events) into the existing tick entry; 120-tick ring.
- **`StateInspector`** — live player snapshot + active encounter sessions
  (boss/phase/phaseTick), formatted for overlay and export.
- **`LogExporter`** — writes a pretty-printed JSON bundle (state, trigger
  history, timeline, log lines) to `coach/debug_logs/coach-debug-export-<ts>.json`;
  never throws. Wired to **auto-export when Debug Mode is switched off**, so
  session data survives for analysis.
- **`DebugOverlayV2`** replaces the Sprint 3 overlay: header + tab view driven
  by a new `debugTab` config dropdown (Events / Triggers / State / Timeline).
  Old DebugOverlay removed.
- Plugin feeds all cores only while Debug Mode is on (zero overhead otherwise);
  event counts accumulate in post(), trigger/callout deltas merge into the
  timeline at their delivery points.

### Verified

- Tests: **177/177 pass** (+7 DebugToolsTest: history cap/filter/order,
  timeline aggregation/merge/ring, state formatting, exporter JSON validity
  parsed back through Gson).

### Decisions

- Tabs via a config dropdown instead of Swing tabs or hotkeys — keeps the
  overlay single-surface and avoids KeyManager plumbing; hotkey cycling can
  come later if wanted.
- Auto-export on debug-off chosen over a UI button (no Swing panel yet): the
  moment you stop debugging, the session bundle is already on disk.
- Timeline stores counts, not raw events — compact enough to cover long fights
  in 120 entries while staying readable.

---

## Sprint 20 — Accessibility Features (2026-08-26)

**Objective:** Audio-only mode, visual-only mode, essential-only filter,
high-contrast WCAG AA palette, and overlay text scaling.

### Done

- **`AccessibilityMode` config enum**: Both / Audio-only / Visual-only.
  - Audio-only: `CoachOverlay.render` returns null (every visual hidden)
  - Visual-only: `applyAudioAccessibility()` silences the audio engine
  - Live-reactive via ConfigChanged; composes correctly with the mute toggle
- **Essential Only toggle**: `CalloutFilter` passes criticals only — wired
  through `CalloutGate`, so it gates both visuals and audio at the source.
- **`ColorPalette`**: default + high-contrast palettes; includes a WCAG 2.x
  relative-luminance contrast-ratio function, unit-tested to assert every
  palette entry ≥ 4.5:1 against the dark panel background. Hue choices avoid
  red/green-only distinctions (luminance always differs too).
- **`TextScaler`**: clamps 50–200%, applied to every overlay font (large and
  small) in CoachOverlay.
- `AccessibilityManager`: single live-config resolver for mode/audio/visual
  questions used by plugin + overlay.
- Tests: palette WCAG assertions (incl. sanity checks white=21:1, black=1:1),
  scaler clamp/scale cases, essential-only gate cases.

### Verified

- Tests: **170/170 pass** (+9).

### Decisions

- Essential-only implemented **inside the callout gate**, not as an overlay
  hack — silenced callouts never schedule audio or visuals at all (rule 5
  preserved per-callout).
- High-contrast changes text colours only, not layout/size (independent of
  text scaling — they compose).
- Screen-reader "tooltips" from the roadmap deferred: RuneLite overlays are
  canvas-drawn with no accessible widget tree; nothing meaningful to expose.

### Deviations from docs

- Roadmap's `CoachConfigV2` again skipped (single config group, as before).
- Roadmap wanted `accessibility_tags` added to pack schema; unnecessary —
  categories already drive mode behaviour (audio-only still speaks criticals,
  visual-only still shows them).

---

## Sprint 19 — Audio Improvements (2026-08-26)

**Objective:** Priority-based audio: higher categories interrupt, same/lower
queue, per-category volume sliders.

### Done

- `AudioCategory` enum (TRANSITION < INFO < WARNING < CRITICAL) with
  callout-category mapping; `AudioPriorityResolver` wraps it.
- `AudioInterruptManager` — the arbitration core (pure, fully unit-tested):
  - higher priority interrupts the playing clip
  - same/lower priority queues
  - on playback finish, highest-priority queued request starts,
    FIFO within a category; re-arms automatically
- `AudioEngine` refactor:
  - submits through the interrupt manager; starters hand off to the single
    playback thread
  - interruption = stop current clip before starting new; stale Clip STOP
    events ignored via identity check so an interrupted clip can't falsely
    trigger queue draining
  - per-category volume map (critical 100 / warning 80 / info 60 /
    transition 50 defaults), effective gain = master × category, both live
- Config: Critical/Warning/Info volume sliders (transition uses default),
  all live-reactive via ConfigChanged.
- Plugin passes callout category into every play request.

### Verified

- Tests: **161/161 pass** (+6: AudioInterruptManagerTest ×6 covering idle
  start, same-category queuing, lower-priority queuing, interruption,
  drain ordering (priority then FIFO), and reset).

### Decisions

- Same-category requests **queue rather than overlap** — two "Pray Ranged!"
  back-to-back play sequentially, matching the acceptance criteria.
- Interruption safety: engine keeps an identity reference to the active Clip;
  a stopped-but-stale clip's STOP event is ignored so it cannot pull the next
  queued item while its replacement is still playing.
- Transition volume keeps its guide-default (50) without a slider for now;
  adding the fourth slider is trivial if wanted.

### Deviations from docs

- Roadmap's `ticksOnNpc`-style timing verification ("stopwatch") is an
  in-game check; headless we assert the arbitration logic instead. The queued
  starter hands off to the playback pool, so tick-thread latency is unchanged.

---

## Sprint 18 — Overlay Improvements (2026-08-26)

**Objective:** Dedicated overlay types with per-overlay config toggles and
quiet-hours suppression.

### Done

- **Six sub-renderers** (`overlay/` package), each pure + headless-testable,
  returning `OverlayLine` data that `CoachOverlay` composes:
  - `PrayerIndicatorRenderer` — flashing large `[PRAY]` line from
    prayer_icon visuals
  - `SafeTileRenderer` — flashing `[MOVE]` advisories from safe_tile visuals
  - `CountdownRenderer` — big "mechanic in Nt!" for predictions ≤5 ticks
    (red at ≤2)
  - `TimelineRenderer` — boss/phase progress bar (▰▱ segments)
  - `StatusIndicatorRenderer` — HP% line colour-coded by threshold
  - `MiniHudRenderer` — one compact persistent summary line
- `OverlayManager`: quiet hours after critical callouts (4 ticks; non-critical
  visuals suppressed, criticals always through), plus live state slots fed by
  the plugin: player HP%, phase progress, current boss/phase labels.
- `CoachOverlay` rewritten as a composer honouring five new config toggles
  (Prayer Indicator / Countdowns / Phase Timeline / Status Indicator /
  Mini HUD), all real-time.
- Plugin plumbing: per-tick player HP%, phase index progress, boss/phase
  labels; critical deliveries trigger quiet hours.

### Verified

- Tests: **155/155 pass** (+8: OverlayRendererTest ×7 covering every renderer,
  OverlayManager quiet-hours test).

### Decisions

- Sub-renderers return data, not draw calls: full renderer logic is testable
  without a graphics context; only CoachOverlay touches Graphics2D.
- Prayer/safe-tile overlays are text-first with flashing colour — colour is a
  secondary cue (colourblind-safe by default per §8.5).
- True tile-space safe-tile highlighting needs scene projection (client
  spatial work); v1 renders advisories instead. Tracked in README-style
  limitation note here rather than shipping half of it.
- Countdown uses prediction data (honest ETAs) rather than guessing timings.
- Quiet hours: criticals bypass suppression entirely; window fixed at 4 ticks.

### Deviations from docs

- Roadmap's `CoachConfigV2` skipped: single config group extended instead
  (same call made in Sprint 17).
- "All 9 overlay types render correctly" — 8 exist (debug overlay counts as
  the 9th); true safe-TILE rendering deferred pending spatial features.

---

## Sprint 17 — Settings Overhaul + Profile Groundwork (2026-08-26)

**Objective:** Full settings gate: per-category callout toggles, per-boss
suppression, and named profile save/apply logic. Phase 4 begins.

### Done

- `CoachConfig` additions:
  - Category toggles: Critical / Warning / Info / Transition callouts
    (all default on)
  - "Disabled Bosses" — comma-separated bossId suppression list
  - Hidden `profilesJson` storage item
- `CalloutFilter` — pure, dependency-free decision function (master enable →
  category toggle → per-boss suppress); `CalloutGate` wraps it as the live
  BiPredicate fed to CoachingEngine. Config changes apply instantly because
  the gate reads live config at every activation.
- `CoachingEngine` filter upgraded to `BiPredicate<bossId, callout>`.
- **`ProfileManager`** — named settings profiles persisted in the hidden
  config item via RuneLite's ConfigManager: `saveProfile` / `applyProfile` /
  `deleteProfile` / `listProfiles`. Learning/Practice/Performance-style
  presets are just profiles with different toggles.
- Tests: CalloutFilterTest ×5 (categories, per-boss csv case-insensitivity,
  master kill switch), ProfileManagerTest ×3 (persist/apply roundtrip,
  unknown profile, delete).

### Verified

- Tests: **147/147 pass** (+8).

### Decisions

- Kept ONE `CoachConfig` group instead of roadmap's `CoachConfigV2` — same
  RuneLite config group can grow; a second class would split settings state.
- Per-boss toggles stored as CSV string rather than JSON object: trivially
  editable in RuneLite's raw config view and matches how packs name bosses.
- Profiles persist through ConfigManager (RuneLite-native) rather than custom
  files; export/import UI is Sprint 22's scope.
- The settings gate caught a real bug during development: the filter initially
  passed the boss id where the suppression CSV belonged — unit tests flagged
  it immediately (rule 7 earning its keep).

### Deviations from docs

- Roadmap's Swing-based profile UI deferred to Sprint 22 (Profile Management);
  this sprint ships the complete logic layer, testable headless.

---

## Sprint 16 — CoX + Community Pack Template (2026-08-26)

**Objective:** CoX guidance pack, the community authoring template
demonstrating every trigger type, and the pack author guide. Phase 3 done.

### Done

- `encounter-packs/cox.pack/` + `cox_1.0.0.zip` (154 KB): guidance-focused v1
  for Vanguards, Tekton, Vasa Nistirio, Great Olm (start callouts + Olm final-
  phase HP warning at 25%). NPC ids flagged for verification (README).
- **`encounter-packs/template.pack/`** — community starting point: a single
  loadable pack demonstrating ALL 12 usable trigger types (animation,
  projectile, graphic, npc_spawn, npc_despawn, hp, tick_timer, player_state,
  location, shout, wave_cleared, composite) plus conditions gating and a
  wave-style phase exit. TODO markers where authors substitute real ids.
- **`docs/examples/ENCODING.md`** — pack author guide: workflow (copy → edit →
  TTS audio → zip → drop in folder → verify), validation rules they'll hit,
  audio pipeline pattern, publishing checklist (rule 8 enforcement).
- CoxPackTest: guards the real zip AND asserts the template demonstrates every
  trigger type and still loads/validates.

### Verified

- Tests: **139/139 pass** (+3 CoxPackTest).

### Decisions

- Template callouts omit `audioFile` deliberately: packs without referenced
  audio are valid, so the template stays self-contained; the guide shows how
  to add audio when authors want it.
- Roadmap's room list ("Tekton, Olm, Vanguard, Vet'ion") was wrong again —
  Vet'ion is a wilderness boss. Shipped Vanguards/Tekton/Vasa/Olm instead.
  Deep CoX mechanics (Olm attack-style prayers, Tekton anvil timing, Vasa
  portals) need verified animation ids → same live-capture path as ToB.

### Deviations from docs

- Roadmap's `docs/examples/ENCODING.md` location kept; content covers the
  workflow + validation rules rather than re-listing schema fields (the schema
  json is the field reference).

---

## Sprint 15 — Tombs of Amascut (2026-08-26)

**Objective:** ToA pack covering all four path bosses + the Wardens, using
mechanics that are honestly detectable (HP thresholds are ToA's friend).

### Done

- `encounter-packs/toa.pack/` + packaged `toa_1.0.0.zip` (254 KB):
  - **Zebak**: special-queue warnings at 85/70/55/40% (wiki-verified special
    queue points), enrage warning at 25%
  - **Akkha**: shadow-phase warnings at 80/60/40/20% (wiki-verified invuln
    thresholds)
  - **Ba-Ba**: knockback warnings at 66%/33% ("Mind the Gap!" danger)
  - **Kephri**: encounter-start guidance only (v1)
  - **Wardens**: start guidance + phase-3 enrage at <5% (heals 20%)
- 10 TTS voice lines; ToaPackTest guards the real zip (boss coverage +
  wiki-matched threshold assertions).

### Verified

- Tests: **136/136 pass** (+4 ToaPackTest).

### Decisions

- HP-threshold-first design: ToA's biggest mechanics are HP-driven per wiki,
  so v1 delivers verified-value warnings without animation-id guesswork.
- Invocation-aware callouts explicitly deferred to Sprint 17 (needs per-raid
  config plumbing) — roadmap listed it as an acceptance item; noted there.
- NPC ids flagged for live verification (README checklist): wrong ids fail
  silent (no callouts), so they're the top checklist item.

### Deviations from docs

- Roadmap says "Akkhan" — correct name is Akkha.
- Roadmap wanted "at least 3 bosses"; shipped 5.

---

## Backlog addition — rest-of-ToB rooms (from user review of Sprint 14)

The Sprint 14 roadmap mechanic list ("crab rave, green/red orbs, lightning,
web bombs") maps to the OTHER ToB rooms: Nylocas (crabs), Xarpus, Verzik
(lightning/webs), Maiden, Bloat. Deferred as a future pack expansion — all
these rooms share Sotetseg's problem: no reliably-sourced animation/projectile
ids for their telegraphs. Structure will follow once ids are captured live
(same README-checklist pattern).

---

## Sprint 14 — Theatre of Blood: Sotetseg (2026-08-26)

**Objective:** First ToB encounter pack. Deliberately scoped to what can be
detected honestly.

### Done

- `encounter-packs/sotetseg.pack/` + packaged `tob_sotetseg_1.0.0.zip`:
  - Encounter start guidance (Pray Magic, spread for the ball) on spawn of
    either scaling variant (npcIds [8337, 8388] — ids confirmed via RuneLite
    ToB damage-multiplier data)
  - Maze warnings at HP < 67% / < 34% (`hp` triggers, wiki-verified 66.6%/33.3%
    shadow-realm thresholds), critical priority with TTS "Follow the red path!"
- Engine improvement: `HpTriggerEvaluator` now accepts multi-NPC id sets
  (registry maps `npcIds`); needed to cover Sotetseg's party-size variants.
- 2 TTS voice lines; TobSotetsegPackTest guards the real zip.

### Verified

- Tests: **132/132 pass** (+2 TobSotetsegPackTest).

### Decisions

- **Scope honesty over roadmap checkbox-ticking**: the roadmap's mechanic list
  ("crab rave, green orbs, red orbs, lightning, web bombs") doesn't correspond
  to Sotetseg at all — those are other ToB rooms' mechanics. Actual Sotetseg
  mechanics per wiki: splitting projectiles, big red ball every 10 attacks,
  shadow-realm mazes at 66.6%/33.3%.
- Shipped only HP-threshold + spawn triggers because Sotetseg's animation and
  projectile ids have no reliable public source; naive pray-callouts would
  mislead given his prayer-disabling split projectiles. Attack/ball triggers
  are README checklist items pending verified ids or a chat-message string.
- Roadmap called Sotetseg the "first boss"; he's the fourth room. Pack follows
  reality, not the doc.

### Manual testing (pending, user)

1. Load zip; confirm `[LOADED]` in debug overlay at Sotetseg.
2. Verify maze callouts fire near the actual maze teleports.
3. Grab the highlighted ball-notification message text in-game so we can add
   a shout trigger for the 188-damage ball.

---

## Sprint 13 — Inferno Implementation (2026-08-26)

**Objective:** The beast: all 69 waves + basic Zuk, wave-transition detection,
per-attack prayer callouts. Roadmap estimated 300 minutes; it was right.

### Done

- **Engine extension — `wave_cleared` trigger type**: stateful evaluator
  tracking the alive subset of a configured NPC id set; fires once when every
  tracked NPC has spawned and then died; re-arms for repeated compositions
  (mager revives and blob splits correctly re-block the clear).
- **Multi-NPC triggers**: `npcIds` list on npc_spawn/npc_despawn (any-id match)
  for wave entry composites.
- **`generate_inferno_pack.py`**: canonical wave table (OSRS Wiki) → full pack:
  - 69 phases: waves 1-68 chained by `wave_cleared` exits (blob-split and
    Jad-healer ids included in relevant waves' clear sets), Zuk terminal
  - attack-callout mechanics injected per-wave by composition: Meleer →
    "Pray Melee!", Ranger → "Pray Ranged!", Mager → "Pray Magic!",
    Blob → switch-prayer warning (all critical, -1 tick audio), Jad →
    per-style "PRAY MAGIC/RANGED/MELEE!" at priority 99, healer spawn alerts
  - 135 mechanics total from ~15 shared definitions
- **9 TTS voice lines** via the edge-tts→ffmpeg pipeline; packaged as
  `inferno_1.0.0.zip` (150 KB).
- Pack README with rule-8 verification checklist (NPC id ranges 7690-7706,
  attack animation table with sources, revive/split semantics).

### Verified

- Tests: **130/130 pass** (+12: WaveClearedEvaluatorTest ×4 incl. revive and
  re-arm semantics, InfernoPackTest ×4 against the real zip, registry/schema
  updates covered transitively).

### Decisions

- Wave-clear = **all spawned wave NPCs dead**, including blob splits and Jad
  healers — matches real gating; mager revives naturally block clears because
  revived mobs re-spawn under tracked ids.
- Attack callouts reuse calloutIds across waves ("pray_ranged" everywhere):
  dedupes TTS assets and lets the callout-level cooldown suppress flick spam.
- Validator scope fix found by this pack: mechanicId uniqueness is now scoped
  per phase / shared-list instead of whole-boss — generated packs legitimately
  reuse attack mechanics across phases, and runtime only evaluates active ones.
- Zuk v1: attack tick warning + healer/Jad spawn alerts only. His single
  attack animation covers both prayer styles — real per-style calls need
  projectile triggers (noted in README).

### Deviations from docs

- Roadmap's "waves generated programmatically with rules" escape hatch used —
  hand-writing 69 phases would be insane.
- Roadmap wanted `arena` concept for Zuk safe tiles; deferred with Zuk v1 scope.

---

## Sprint 12 — Nex Implementation (2026-08-26)

**Objective:** First real encounter pack: full five-phase Nex with special
attack detection, prayer guidance, and real TTS audio. The fun one.

### Done

- **Engine extension — `shout` trigger type**: every Nex special is announced
  by a chat shout, so the plugin now subscribes to `ChatMessage` and gained
  `ShoutTriggerEvaluator` (case-insensitive substring match, optional sender
  filter). Registered in the registry + schema v1.0 updated.
- **`encounter-packs/nex.pack/`**: complete encounter definition:
  - 5 phases: smoke → shadow → blood → ice → zaros, chained by HP-threshold
    exits (80/60/40/20%); Zaros terminal
  - 13 mechanics: phase-entry prayer guidance (critical, -2 tick audio offset)
    for all five phases; specials via shouts (Choke, Smoke Dash, Shadow Smash,
    Embrace Darkness, Siphon, Sacrifice, Containment, Ice Prison); Wrath on
    death via `npc_despawn`
- **Real TTS audio** (`generate_nex_audio.py`, reusable pattern): edge-tts
  `en-US-GuyNeural` +15% rate → ffmpeg → mono 44.1kHz Vorbis `.ogg`; all
  14 callouts generated; packaged as `encounter-packs/nex_1.0.0.zip` (333 KB,
  15 entries).
- Pack README with the rule-8 human verification checklist (shout strings,
  chat-type delivery, HP thresholds, wrath timing) — required before any
  community release.
- `NexPackTest`: regression guard that loads the REAL zip from the repo and
  asserts structure (5 phases ordered, HP exit chain, terminal Zaros), shout
  coverage for all 12 announced mechanics, and audio on every callout.

### Verified

- Tests: **122/122 pass** (+4: NexPackTest ×3, ShoutTriggerEvaluatorTest ×4,
  minus assumption overlap).

### Decisions

- **Shout-based special detection over animation/projectile IDs**: wiki
  research confirmed all Nex specials are telegraphed by fixed shout strings;
  animation IDs were NOT reliably sourced from public data, so guessing them
  would violate rule 8's spirit. Shouts also survive game updates less often.
- Roadmap said phases "Smoke, Ranged, Mage, Melee, Zaros" — wrong; actual
  phases per wiki are Smoke/Shadow/Blood/Ice/Zaros. Pack follows reality.
- Prayer callouts use -2 tick offsets ("2 ticks before impact" per MVP spec);
  reaction specials (dash/smash/darkness) use 0/-1 since they're already
  telegraphed at trigger time.
- `.ogg` shipped per rule 11 even though playback needs Sprint 27's decoder;
  visuals fire regardless until then.
- Drag (Smoke) not implemented: no telegraph exists to detect.

### Manual testing (pending, user — the big one)

1. Copy `encounter-packs/nex_1.0.0.zip` into `.runelite/coach/encounters/`.
2. Debug overlay should show it `[LOADED]`; enter a Nex fight (or watch a
   stream of one) and verify shout callouts fire on specials + phase prayers
   appear at each transition.
3. Report exact shout strings if matching fails — one-line pack fix.

---

## Sprint 11 — Boss Loader + Pack Management (2026-08-26)

**Objective:** Pack lifecycle management: statuses, boss-conflict detection,
dependency reporting, pack visibility in the debug overlay.

### Done

- `PackManager` — owns the directory scan and per-file outcomes:
  - `PackStatus` per zip: LOADED / REJECTED (invalid) / CONFLICT, with
    human-readable `describe()` lines
  - **boss conflict detection**: two packs claiming the same bossId →
    alphabetically-first file wins, later ones marked CONFLICT and skipped;
    duplicate packIds treated the same way
  - **dependency reporting**: new optional `metadata.dependencies` (packIds);
    unsatisfied deps are warnings — the pack still loads
  - deterministic processing order (alphabetical filenames)
- `EncounterEngine.loadPacks` now delegates to PackManager; exposes
  `getPackStatuses()` + `getPackSummaryLines()`; rejections/conflicts/warnings
  logged on every reload.
- `DebugOverlay` gained a context-lines section: shows live pack statuses
  (`a_golem.pack.zip -> golempack@1.0.0 [LOADED]`, ...) above the event log.
- Schema v1.0 + validator updated for `metadata.dependencies`.
- CoachPlugin already reloaded packs on config change (Sprint 4) — now the
  reload also surfaces conflicts/warnings in logs.

### Verified

- Tests: **115/115 pass** (+7 PackManagerTest: distinct bosses, boss conflict
  first-wins, duplicate packId, satisfied/missing dependency, rejected status,
  overlay summary lines).

### Decisions

- Conflict policy is **first-pack-wins by filename order** — deterministic and
  easy to reason about ("rename your file to win"); reported loudly either way.
- Missing dependencies warn instead of rejecting: a pack missing an optional
  companion still provides value for the bosses it does define.
- Audio pre-loading was already implemented at reload time (Sprint 9); no
  change needed this sprint.

### Deviations from docs

- Roadmap wanted separate `PackLoader.java`; its responsibilities (zip
  extraction + validation) already lived in `EncounterLoader` since Sprint 4,
  so only the missing lifecycle piece (`PackManager`) was created.

---

## Sprint 10 — JSON Schema Formalization (2026-08-26)

**Objective:** Finalize schema v1.0, add the migration framework, audio
reference checking, and a consolidated validation suite. Phase 3 begins.

### Done

- `schemas/encounter_schema_v1.json` finalized: every field documented,
  conditions/recovery/composite/region fields included, matches what the
  plugin actually enforces.
- `schemas/migration_v0_to_v1.json` + `SchemaMigrations`: **data-driven
  migration framework** — declarative steps (rename/copy/set/remove) loaded
  from the resource, applied sequentially until current version. Legacy packs
  missing schemaVersion can be detected by layout (`detect.path`). Unknown
  versions rejected with "no migration path" message.
- Loader pipeline formalized: raw JSON → migrate → Gson parse → rule
  validation → **audio file existence check** (rule 8: referenced callout
  audio must exist in the pack zip's audio/).
- **Bug found & fixed**: SchemaValidator kept its error list as instance state
  across validate() calls — violations from one pack leaked into the next
  pack's error messages when loading multiple zips. Now reset per invocation.
- Committed zip fixtures under `src/test/resources/test_packs/`:
  valid_pack.zip (with real silent WAV), invalid_missing_fields.zip,
  invalid_bad_trigger_type.zip, legacy_v09_pack.zip (migrates on load).
- `SchemaValidationTest` — consolidated suite: fixture-based + rule-by-rule
  edge cases (future versions, dup callouts, composite children/logic,
  negative cooldown, priority range, condition validation, stripped audio).

### Verified

- Tests: **108/108 pass** (+15 net: SchemaValidationTest ×14, minus overlap).

### Decisions

- Migrations are **declarative JSON**, not code — pack authors and the AI
  pipeline can read exactly what changed between versions; new ops are trivial
  to add. Reaching the target version is implicit in the chain (no explicit
  set-schemaVersion step needed).
- Audio existence is enforced at load time per rule 8; string-only parsing
  (tests) skips it since there's no zip context.

### Deviations from docs

- Roadmap's `tests/java/...` + `tests/resources/...` layout adapted to Gradle
  standard `src/test/java` + `src/test/resources`.
- Roadmap's `migration_v0_to_v1.json` implied code-side migrations; implemented
  as a data file + small engine instead (see decision above).

---

## Sprint 9 — Prediction + Audio/Visual Dispatch (2026-08-26)

**Objective:** Close the coaching loop: predicted mechanics displayed, callouts
rendered as overlays, audio played from pack files. Phase 2 complete.

### Done

- `coaching/PredictionEngine` — predicts tick_timer-driven mechanics within a
  10-tick horizon using session phase clocks; event-driven mechanics are
  deliberately NOT guessed.
- `overlay/OverlayManager` — active visual store (pack-defined or default 3-tick
  duration, max 5, oldest dropped) + prediction snapshot.
- `overlay/CoachOverlay` — TOP_CENTER panel: active callouts color-coded by
  category (critical=red, warning=orange, transition=cyan, info=white) plus
  "next: mechanic (Nt)" prediction line.
- `audio/AudioEngine` — pre-loads `audio/*` entries from pack zips into memory;
  async playback on a daemon thread (never blocks ticks); master volume +
  mute config items, live-reactive; graceful no-op for missing files.
- `CoachPlugin` dispatch wiring: delivered callout → visual + audio together;
  per-tick prediction refresh; pack reload also re-loads audio caches.
- `EncounterEngine`: session snapshot + `getPackIdForBoss` for audio resolution.

### Verified

- Tests: **93/93 pass** (+12: PredictionEngineTest ×4, OverlayManagerTest ×4,
  AudioEngineTest ×4 with a synthesized in-code WAV fixture).

### Decisions

- **No Ogg decoder bundled** — verified against client-1.12.36.jar contents and
  POM. WAV/PCM playback ships now (fully testable headless); the .ogg decoder
  integration lands in Sprint 27 alongside the real TTS output, where actual
  .ogg fixtures exist to test against. Until then packs without audio are
  first-class: visuals fire, audio is a logged no-op (rule 5 preserved).
- Mute config gates **audio only**; visual-only/audio-only accessibility modes
  are Sprint 20 as planned.
- Predictions render only when honest (tick_timer-computable); no fake ETAs.

### Deviations from docs

- Guide §10 claims JOrbis/"OGG decoder bundled in RuneLite" — false per jar
  inspection. Documented here rather than adding an untested dependency blind.
- Roadmap wanted PredictionEngine inside CoachingEngine; it's wired at plugin
  level instead (equivalent flow, less coupling between engines).

---

## Sprint 8 — Coaching Engine (Priority + Queue) (2026-08-26)

**Objective:** The decision core: consume mechanic activations, schedule
callouts at tick offsets, suppress duplicates, deliver in priority order.

### Done

- `CalloutScheduler` — schedules at `activationTick + offset` (visual/audio);
  delivery moment = min(visualTick, audioTick) so negative audio offsets fire
  early ("2 ticks before impact" works).
- `CalloutQueue` — sorted pending list: dueTick asc, priority desc at equal
  ticks; `drainDue(tick)` extracts everything due.
- `CooldownManager` — per-calloutId suppression window (default 4 ticks).
- `PriorityResolver` — explicit definition priority (clamped 1–100), else
  category defaults critical(90) > warning(70) > info(50) > transition(40).
- `CoachStateManager` — latest PlayerState snapshot per tick.
- `CoachingEngine` — consumes `MechanicActivation`s from EncounterEngine,
  applies enable-filter + cooldown + schedule; delivers due callouts to
  listeners each tick. Plugin wires state updates and debug logging;
  overlay/audio listeners arrive Sprint 9.
- CoachPlugin lifecycle hardened: internal EventBus is now rebuilt on startUp
  instead of hand-unsubscribing each listener (no stale-listener leaks across
  plugin restarts); tick handlers guard against pre-startup events.

### Verified

- Tests: **81/81 pass** (+9: PriorityResolverTest ×3, CalloutQueueTest ×2,
  CoachingEngineTest ×4 incl. offsets, cooldown dupes, disabled filter,
  concurrent ordering).

### Decisions

- Delivery moment = min(visual, audio) tick: one queue entry per callout;
  AudioEngine (Sprint 9) refines its own sub-tick timing from audioOffset.
- Callout-level cooldown (default 4 ticks) is separate from mechanic-level
  cooldown (pack-defined) — shared callouts referenced by multiple mechanics
  still can't spam.
- Enable/disable is a predicate (`setEnabledFilter`) now; full RuneLite config
  toggles land in Sprint 17 as planned.

---

## Sprint 7 — Phase Machine + Conditions (2026-08-26)

**Objective:** Runtime encounter state: phase transitions, mechanic tracking
with cooldowns + condition gating, recovery on death/despawn.

### Done

- `ActiveEncounter` — runtime session per live boss: current phase,
  phase/global tick counters, per-mechanic cooldown map.
- `PhaseMachine` — entry via entryTrigger fires; exit triggers advance to the
  NEXT phase in list order (last phase is terminal).
- `MechanicManager` — mechanic trigger matching (exact or `#index` contexts)
  and cooldown enforcement (`cooldown` ticks between activations).
- `ConditionEvaluator` — gates activations: npc/player hp thresholds
  (live client), tick_mod on phaseTick; unknown types warn once + fail closed.
- `RecoveryHandler` — resets sessions on tracked-boss despawn or player HP 0.
- `EncounterEngine` upgraded: consumes trigger fires, manages sessions,
  emits `MechanicActivation`s to listeners (Sprint 8's Coaching Engine plugs in
  here). Also subscribed to the internal bus for tick counters + recovery.
- `ConditionDefinition` model + schema/validator support for `conditions`.
- `GameStateBridge.findNpc` static helper (deduped NPC lookup from Sprint 6's
  HpTriggerEvaluator).

### Verified

- Tests: **72/72 pass** (+15: PhaseMachineTest ×4, MechanicManagerTest ×3,
  RecoveryHandlerTest ×3, ConditionEvaluatorTest ×4, EncounterEngineFlowTest ×1
  covering the full flow: entry → gated activation → cooldown suppression →
  re-activation → phase transition → despawn reset → re-entry).

### Decisions

- Phases are **sequential** for now (exit → next in list). Explicit transition
  graphs (schema `transitions`) deferred until a boss actually needs them —
  Nex's phases are HP-driven and sequential.
- Conditions fail **closed** (unknown/unevaluable = false) so packs never fire
  callouts on unverified state.
- Sessions are keyed by npcId; multiple simultaneous bosses supported.
- `globalTick` only advances via TICK batches — fires carry their own tick but
  phase-relative conditions use the session clock.

---

## Sprint 6 — Trigger Engine (Advanced Triggers) (2026-08-26)

**Objective:** All remaining trigger types: npc_spawn/despawn, hp, tick_timer,
player_state, location, composite AND/OR. (Roadmap Sprint 6.)

### Done

- `NpcSpawnTriggerEvaluator` — spawn + despawn variants (boss entry detection).
- `TickTimerTriggerEvaluator` — `(tick − offset) % mod == 0`, offset optional.
- `HpTriggerEvaluator` — NPC health % via live client lookup
  (`getTopLevelWorldView().npcs()`), **edge-detected** so it fires once at the
  crossing, not every tick beyond; re-arms when the boss despawns.
- `PlayerStateTriggerEvaluator` — two modes: player animation
  (AnimationChanged where actor is the Player), or player HP below/above
  threshold (edge-detected on StatChanged HITPOINTS).
- `LocationTriggerEvaluator` — rectangular world region, fires on ENTRY only.
- `CompositeTriggerEvaluator` — AND/OR over children against the same event;
  interestedIn = union of children's interests.
- `ProjectileTriggerEvaluator` upgraded with optional `srcNpcId` — verified
  `Projectile.getSourceActor()` exists in the API, so no deferment needed.
- `EdgeDetector` helper shared by hp/player-hp/location.
- Schema v1 json + TriggerDefinition gained srcNpcId/tickOffset/region fields.

### Verified

- Tests: **57/57 pass** (+12: AdvancedTriggerEvaluatorTest ×9,
  CompositeTriggerTest ×3; existing tests updated for registry constructor).

### Decisions

- **HP triggers are evaluated per-tick against live client state** rather than
  an event, because RuneLite has no NPC HP event — this required giving
  TriggerRegistry a Client reference (nullable in tests). This is a deliberate
  softening of "triggers are stateless": edge state lives inside evaluators.
- Composite AND requires children observing the same event type (cross-event
  combos like "spawned AND hp<50%" are mechanic *conditions* → Sprint 7's
  ConditionEvaluator). Documented on the class.
- Location/hp evaluators need the client; registry skips them with a warning
  when no client is available (unit-test path).

### Deviations from docs

- `CustomRuleTriggerEvaluator` NOT created: custom rules are condition
  expressions, which belong to Sprint 7's ConditionEvaluator. The 'custom'
  type is schema-valid but logs "not supported yet" until then. Roadmap listed
  the file this sprint; deferring avoids dead code.

---

## Sprint 5 — Trigger Engine (Core Triggers) (2026-08-26)

**Objective:** Animation, Projectile and Graphic triggers evaluate against
batched game events; fires flow out for downstream engines. (Roadmap Sprint 5.)

### Done

- `trigger/TriggerEvaluator` — stateless matcher interface with an
  `interestedIn()` event-type filter so the engine skips irrelevant events.
- `AnimationTriggerEvaluator` — NPC + animation id match (null npcId = any NPC).
- `ProjectileTriggerEvaluator` — projectile id match via ProjectileMoved.
- `GraphicTriggerEvaluator` — matches GraphicChanged (actor spotanim) OR
  GraphicsObjectCreated (AoE/tile effect); optional npc filter.
- `TriggerRegistry` — type string → evaluator factory; unknown types or missing
  numeric fields → warning + skip, never crash (§8.4).
- `TriggerEngine` — rebuilt from loaded packs on load/reload; evaluates each
  tick batch; produces `TriggerFire` (tick, bossId, contextId, description);
  one fire per evaluator per event occurrence; fire listeners notified.
- Phase entry/exit trigger definitions are registered too (evaluated where an
  evaluator exists — e.g. npc_spawn waits for Sprint 6).
- `CoachPlugin`: TriggerEngine permanently subscribed to the internal bus
  (coaching must run even when debug mode is off); fires feed TriggerLogger
  only in debug mode.

### Verified

- Tests: **45/45 pass** (31 prior + CoreTriggerEvaluatorTest ×9 +
  TriggerEngineTest ×5: match/no-match/no-dup/rebuild/listeners).

### Decisions

- **'graphic' covers both manifestations**: many boss mechanics appear as
  tile-level GraphicsObjects rather than actor spotanims; one trigger type
  matching both keeps packs simple. Filterable by npcId for the actor case.
- Mechanic context ids get a `#index` suffix when a mechanic defines multiple
  triggers (`shadow_smash#0`) — disambiguates fires for Sprint 7+.
- Projectile source-NPC matching deferred to Sprint 6 (needs live client NPC
  positions to compare against projectile origin tiles).

### Deviations from docs

- Implementation guide's `TriggerEvaluator.evaluate(event, EncounterState)`
  signature trimmed to `matches(GameEvent)` until EncounterState exists
  (Sprint 7); guide's example also casts payloads that don't exist as written.

---

## Sprint 4 — Encounter Engine (JSON Loader) (2026-08-25)

**Objective:** Load encounter packs from the user's pack directory with
schema validation; invalid packs rejected at load time. (Roadmap Sprint 4.)

### Done

- `encounter/model/` — schema v1 POJOs populated by Gson: `EncounterPack`,
  `PackMetadata`, `BossDefinition`, `PhaseDefinition`, `MechanicDefinition`,
  `CalloutDefinition`, `TriggerDefinition`, `VisualDefinition`.
- `SchemaValidator` — plugin-side load gate implementing architecture §10 rules:
  required fields, unique boss/phase/mechanic/callout ids, known trigger types
  (incl. composite AND/OR children), callout categories, priority 1–100,
  tick offsets −5..10, ≥1 phase per boss, ≥1 trigger per mechanic.
- `EncounterLoader` — parses + validates; reads `encounter.json` out of `.zip`
  packs; all failures become actionable `PackLoadException` messages.
- `EncounterEngine` — scans `<packDir>/*.zip`, loads valid / rejects invalid
  (logged, never fatal), supports hot reload; lookup by live NPC id.
- `CoachConfig`: "Encounter Pack Directory" item (default
  `.runelite/coach/encounters/`); changing it reloads packs without restart.
- `resources/schemas/encounter_schema_v1.json` — authoring contract for the
  future AI pipeline.
- Tests: pack loading/rejection/reload via real zip fixtures in temp dirs.

### Verified

- `gradlew.bat --no-daemon build` → BUILD SUCCESSFUL
- Tests: **31/31 pass** (15 prior + EncounterLoaderTest ×10 + EncounterEngineTest ×5).

### Decisions

- **No external JSON-Schema library** (rule: no deps beyond RuneLite). The
  validator is hand-rolled against §10's rule list; full JSON-Schema checking
  stays in the AI pipeline tool where dependencies are unconstrained.
- Validation collects *all* violations per pack (not fail-fast) so pack authors
  see every problem in one log line.
- Trigger models store raw fields (npcId/animationId/...) — evaluation comes in
  Sprint 5; nothing evaluates yet.

### Deviations from docs

- Roadmap listed model classes as `Encounter.java`/`Phase.java` etc. under a
  runtime split from JSON classes; deferred that split until Sprint 7 adds
  mutable state — for now one set of immutable DTOs serves both roles.

### Manual testing (pending, user — optional)

1. Create `.runelite/coach/encounters/`, drop in a valid test pack zip →
   log shows "loaded 1 encounter pack(s)".
2. Drop a corrupt zip → "rejected encounter pack: ..." in log, plugin fine.

---

## Sprint 3 — Logging System + Debug Overlay (2026-08-25)

**Objective:** Debug logging (ring buffer + file) and an in-game debug overlay
showing recent events with tick numbers. (Roadmap Sprint 3.)

### Done

- `logging/LogBuffer` — central sink: 100-entry ring buffer + optional file writer.
- `logging/FileLogWriter` — lazy append-only writer to
  `.runelite/coach/logs/coach-debug.log`; never throws into the caller.
- `logging/EventLogger` — formats every event in each tick batch:
  `t<tick> <TYPE> <payload summary>` (actor/anim ids, projectile ids,
  npc spawn/despawn, stat levels, varbit id/value, container id).
- `logging/TriggerLogger`, `CalloutLogger` — stable APIs defined now;
  they start receiving real traffic in Sprints 5+/8+.
- `overlay/DebugOverlay` — top-left overlay rendering the last 30 entries.
- `CoachConfig`: added "Log To File" toggle alongside existing Debug Mode.
- `CoachPlugin`: debug enable/disable is now live-reactive via ConfigChanged —
  attaches/detaches the EventLogger listener, file writer, and overlay without
  plugin restart.

### Verified

- `gradlew.bat --no-daemon build` → BUILD SUCCESSFUL (deprecation warnings only)
- Tests: **15/15 pass** (7 from Sprint 2 + LogBufferTest ×4 + EventLoggerTest ×3).

### Decisions

- Introduced `LogBuffer` as a shared sink rather than three separate loggers
  writing to different places — one ring buffer feeds both the overlay and the
  file; TriggerLogger/CalloutLogger just log lines into it.
- Debug logging is fully gated behind Debug Mode: when off, no listener is
  attached to the internal bus at all (zero per-tick overhead for normal play).
- EventBus gained `unsubscribe(Listener)` so debugging can detach live.
- Overlay shows last 30 of 100 buffered entries (readability over completeness).

### Deviations from docs

- Roadmap expected separate EventLogger/TriggerLogger/CalloutLogger classes
  each owning their output; kept the class names but routed them through the
  shared LogBuffer instead (simpler, single source of truth).

### Manual testing (pending, user — see Sprint 2 note re: safe local testing)

1. Load JAR in RuneLite (sideboard plugins dir or dev runner).
2. Enable Debug Mode → overlay appears top-left showing live event lines.
3. Enable Log To File → check `.runelite/coach/logs/coach-debug.log`.
4. Toggle Debug Mode off → overlay disappears, logging stops, no restart needed.

---

## Sprint 2 — Event System (2026-08-25)

**Objective:** Subscribe to core RuneLite game events and establish the internal
tick-batched Event Bus. (Roadmap Sprint 2.)

### Done

- `events/EventType.java` — internal enum of 10 event types.
- `events/GameEvent.java` — immutable wrapper (type + tick + payload).
- `events/EventBus.java` — internal pub-sub with tick batching: non-tick events
  buffer between ticks; the TICK event flushes the whole batch to listeners.
- `events/GameStateBridge.java` — RuneLite → internal state translation.
- `model/PlayerState.java`, `model/BossState.java` — immutable snapshots
  (plain-int positions to keep API types out of the engine).
- `CoachPlugin` — `@Subscribe` handlers for all 10 events feeding the internal
  bus; debug-mode per-tick count logging; register/unregister on start/shutdown.
- Test deps added (JUnit 5.10, Mockito 4.11) + first unit tests.

### Verified

- `gradlew.bat --no-daemon build` → BUILD SUCCESSFUL
- Tests: **8/8 pass** (`EventBusTest` ×5: batching, ordering, multi-listener,
  empty tick, flush; `GameStateBridgeTest` ×3: player/boss extraction).

### Decisions

- Adapted the roadmap's event list to the **real RuneLite 1.12.x API**:
  - `ProjectileSpawned` does not exist → using `ProjectileMoved` (fires on
    spawn as the projectile's first movement; standard detection idiom).
  - `NpcHpChanged` does not exist → NPC health is polled via
    `getHealthRatio()/getHealthScale()` in `BossState`; dropped as an event.
  - `StatsChanged` is singular: `StatChanged` → mapped to PLAYER_STATS_CHANGED.
  - Added `GraphicsObjectCreated` (key for future boss-mechanic triggers) to
    keep the count at 10 core event types.

### Deviations from docs

- Implementation guide's sample `EventBus.subscribe(...)` API doesn't match
  RuneLite's actual bus methods (`register(Object)` / `unregister(Object)`),
  confirmed via javap against client-1.12.36.jar.
- Guide's `GameEvent.timestamp` field omitted (tick number suffices; avoids
  clock dependence in tests).

### Manual testing (pending, user)

1. Build JAR, load into RuneLite, enable plugin + Debug Mode config toggle.
2. Fight any mob → log lines like `[coach] tick 1234: 5 event(s):
   ANIMATION_CHANGED=2 PROJECTILE_MOVED=1 ...` appear once per tick with events.

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
