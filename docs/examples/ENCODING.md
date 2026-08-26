# Pack Author Guide (ENCODING)

How to write, package, test, and publish a Project Coach encounter pack.

**Start from the template:** `encounter-packs/template.pack/` — it contains a
working example of every trigger type and this guide's companion README.

---

## 1. Pack structure

A pack is a `.zip` containing:

```
myboss_1.0.0.zip
├── encounter.json      # required, exactly this name
└── audio/              # optional; callout .ogg/.wav files live here
    └── pray_melee.ogg
```

Drop the zip into `<runelite>/coach/encounters/`. The plugin scans that
directory on startup and on every config change to the pack-directory setting.
Invalid packs are logged and skipped — they never break the plugin.

## 2. encounter.json at a glance

| Section | Required | Purpose |
|---------|----------|---------|
| `schemaVersion` | yes | Must be `"1.0"` (older known versions auto-migrate) |
| `metadata` | yes | `packId` (unique), `name`, `version`, `gameVersion`, optional `dependencies` (list of packIds), `description`, `author` |
| `bosses[]` | ≥1 | Each: `bossId`, `name`, `npcId`, `phases[]` (≥1), optional shared `mechanics[]`, `recovery` |
| phase | per boss | `phaseId`, `name`, `entryTrigger`, optional `exitTriggers[]`, `mechanics[]` |
| mechanic | | `mechanicId`, `name`, `triggers[]` (≥1), `callouts[]`, optional `conditions[]`, `cooldown` |
| callout | | `calloutId`, `text`, `category`, optional `audioFile`, `priority`, `audioOffset`, `visualOffset`, `visual{}` |
| trigger | | `type` + type-specific fields |

Full field documentation with descriptions:
`plugin/src/main/resources/schemas/encounter_schema_v1.json`

## 3. Validation rules you'll hit

- Unique ids: packId (per raid dir), bossId, phaseId (per boss),
  mechanicId (per phase / shared list), calloutId (per mechanic)
- Tick offsets (`audioOffset`/`visualOffset`) must be −5…+10
- Callout categories: `critical | warning | info | transition`
- Priority 1–100; cooldown ≥ 0
- Every referenced `audioFile` must exist under `audio/` in the zip
- Unknown trigger/condition types are rejected at load time

Error messages collect *all* violations per pack in one log line.

## 4. Audio (rule 11: pre-recorded TTS, generated offline)

Callout text is your TTS source. The repo's standard pipeline:

```python
# pattern from encounter-packs/generate_nex_audio.py
import edge_tts, subprocess
await edge_tts.Communicate(text, "en-US-GuyNeural", rate="+15%").save("x.mp3")
subprocess.run(["ffmpeg", "-i", "x.mp3", "-acodec", "libvorbis",
                "-ar", "44100", "-ac", "1", "x.ogg"])
```

Note: until Sprint 27's decoder integration the plugin plays `.wav` only;
`.ogg` files load and validate fine but playback is a graceful no-op.

## 5. Testing checklist before publishing

1. Zip loads: debug overlay shows `yourfile.zip -> packId@version [LOADED]`
2. Fight (or watch VODs of) the boss; verify every trigger fires
3. Verify ids against the live client (RuneLite dev tools / `!npc`)
4. Confirm callout timing feels right (offsets are ticks; 1 tick = 600ms)
5. Write a README verification checklist like the shipped packs'
6. Only then share — rule 8: unverified content is not community content

## 6. Versioning

- Pack `metadata.version`: semver, independent of plugin version
- Bump on any content change; changelog goes in your pack's README
