# Pack Author Guide (ENCODING)

How to write, package, test, and publish a Project Coach encounter pack.

**Start from the template:** `encounter-packs/template.pack/` — it contains a
working skeleton of every trigger type and this guide's companion README.

**Authoritative field list:** `plugin/src/main/resources/schemas/encounter_schema_v1.json`
(what the loader actually enforces). This guide summarizes that contract.

---

## 1. Pack structure

A pack is a `.zip` containing:

```
myboss_1.0.0.zip
├── encounter.json      # required, exactly this name
└── audio/              # optional; callout .wav (required for playback) files
    └── pray_melee.wav
```

Drop the zip into `<RuneLite dir>/coach/encounters/`
(default **Encounter Pack Directory** / `packDirectory`). The plugin scans
that directory on startup and on
every config change to that setting.

Invalid packs are **logged and skipped — never fatal**. Duplicate `packId`
or conflicting `bossId` → later file is `CONFLICT` (alphabetical order,
first pack wins). All validation errors for a pack are collected into one
log line. Statuses appear in the debug overlay as
`file.zip -> packId@version [LOADED|REJECTED|CONFLICT]`.

## 2. encounter.json at a glance

| Section | Required | Purpose |
|---------|----------|---------|
| `schemaVersion` | yes | Must be `"1.0"` (older known versions auto-migrate) |
| `metadata` | yes | `packId`, `name`, `version` (semver), `gameVersion`; optional `description`, `author`, `dependencies[]` |
| `bosses[]` | ≥1 | Each: `bossId`, `name`, `npcId`, `phases[]` (≥1), optional shared `mechanics[]`, `recovery` |
| phase | per boss | `phaseId`, `name`, `entryTrigger`; optional `exitTriggers[]`, `mechanics[]` |
| mechanic | | `mechanicId`, `name`, `triggers[]` (≥1), optional `callouts[]`, `conditions[]`, `cooldown` |
| callout | | `calloutId`, `text`, `category`; optional `audioFile`, `priority`, `audioOffset`, `visualOffset`, `visual{}` |
| trigger | | `type` + type-specific fields (see §3) |
| condition | | `type` + type-specific fields (see §3) |

### Notes the schema actually enforces

- `exitTriggers` and `callouts` on a mechanic are **optional** in the
  validator (last phase of a boss needs no exit; a mechanic may be
  trigger-only).
- There is **no** top-level shared `mechanics`/`triggers` map and **no**
  nested `visual.position` — those older doc sketches are not parsed.
- `visual` supports `type`, `color` (`#RRGGBB`), `opacity` (0–1),
  `durationTicks` (≥1).

## 3. Trigger types

Enum (unknown types rejected at load):

| `type` | Key fields |
|--------|------------|
| `animation` | `npcId?`, `animationId` |
| `projectile` | `projectId`, `srcNpcId?` |
| `graphic` | `graphicId`, `npcId?` |
| `npc_spawn` | `npcId` or `npcIds[]` |
| `npc_despawn` | `npcId` or `npcIds[]` |
| `hp` | `npcId` or `npcIds[]`, `hpThreshold`, `hpDirection` (`below`\|`above`, edge-detected) |
| `tick_timer` | `tickMod` (≥1), `tickOffset?` |
| `player_state` | `animationId` **or** `hpThreshold` (+ optional `hpDirection`) |
| `location` | `minX`/`maxX`/`minY`/`maxY` (inclusive) |
| `shout` | `containsText`, `senderName?` (case-insensitive substring) |
| `wave_cleared` | `npcIds[]` (all spawned NPCs must die; re-arms) |
| `composite` | `logic` (`AND`\|`OR`) + non-empty `children[]` |
| `custom` | reserved (ConditionEvaluator / Sprint 7) — **not registered yet** |

### Condition types

Schema enum: `npc_hp_below`, `npc_hp_above`, `player_hp_below`,
`player_hp_above`, `tick_mod`, `player_in_region`, `prayer_active`,
`prayer_inactive`, `inventory_contains`, `custom`.

**Runtime reality:** only `npc_hp_below/above`, `player_hp_below/above`, and
`tick_mod` are implemented. Others validate but evaluate **false with a
warning** (`ConditionEvaluator` default branch) — do not ship packs that
depend on the rest until implemented.

## 4. Validation rules you'll hit

- Unique ids: `packId` (per directory), `bossId`, `phaseId` (per boss),
  `mechanicId` (per list scope), `calloutId` (per mechanic)
- Tick offsets (`audioOffset`/`visualOffset`) must be −5…+10
- Callout categories: `critical | warning | info | transition`
- Priority 1–100; cooldown ≥ 0
- Every referenced `audioFile` must exist under `audio/` in the zip —
  missing files **reject the whole pack** at load
- Unknown trigger/condition types are rejected at load time

## 5. Audio (rule 11: pre-recorded TTS, generated offline)

Callout `text` is your TTS source. The repo's standard pipeline:

```python
# pattern from encounter-packs/generate_nex_audio.py
import edge_tts, subprocess
await edge_tts.Communicate(text, "en-US-GuyNeural", rate="+15%").save("x.mp3")
subprocess.run(["ffmpeg", "-i", "x.mp3", "-acodec", "pcm_s16le",
                "-ar", "44100", "-ac", "1", "x.wav"])
```

**Playback note:** ship **`.wav`** (PCM) callouts. Java Sound (and therefore
the plugin) has no Ogg decoder — `.ogg` references still validate at pack
load but **fail at playback** (visual callouts still fire). Shipped packs
are all `.wav` as of the beta audio fix.

## 6. Testing checklist before publishing

1. Zip loads: debug overlay shows `yourfile.zip -> packId@version [LOADED]`
2. Fight (or watch VODs of) the boss; verify every trigger fires
3. Verify ids against the live client (RuneLite dev tools / `!npc`)
4. Confirm callout timing feels right (offsets are ticks; 1 tick = 600ms)
5. Write a README verification checklist like the shipped packs'
6. Only then share — rule 8: unverified content is not community content

## 7. Versioning

- Pack `metadata.version`: semver, independent of plugin version
- Bump on any content change; changelog goes in your pack's README

## 8. Related docs

- Template: `encounter-packs/template.pack/`
- Shipped examples: `encounter-packs/nex.pack/`, `inferno.pack/`, etc.
- Knowledge pipeline (wiki → pack): `knowledge-pipeline/README.md`
- Schema JSON: `plugin/src/main/resources/schemas/encounter_schema_v1.json`
- User guide: [`docs/USER_GUIDE.md`](../USER_GUIDE.md)
