# Project Coach — Player Guide

Real-time, tick-accurate boss coaching for Old School RuneScape via RuneLite:
visual overlays and audio callouts driven by data-only **encounter packs**.
Callouts are always advisory — the plugin never simulates input.

---

## 1. Install

1. Install [RuneLite](https://runelite.net/) (standard or stable).
2. Open RuneLite → **Settings → Plugins**.
3. Search for **Coach** and enable it when listed in the
   [Plugin Hub](https://runelite.net/plugin-hub).
   *Until the hub PR merges, install from a local JAR (`coach-<version>.jar`)
   via RuneLite’s external-plugin loader / developer tools.*
   Beta testers: prefer [`docs/BETA_GUIDE.md`](BETA_GUIDE.md) §2.
4. Confirm the plugin appears and the Coach overlay registers (no red error
   in the client console).

Plugin metadata (from `runelite-plugin.properties`):

| Key | Value |
|-----|--------|
| name | Coach |
| description | Real-time boss coaching with visual and audio callouts |
| tags | pvm, bossing, coaching, overlay, audio |

## 2. Encounter packs

Packs are `.zip` files dropped in:

```
<RuneLite dir>/coach/encounters/
```

Default **Encounter Pack Directory** config (`packDirectory`) is exactly that
path (configurable). Each zip must
contain `encounter.json` plus optional `audio/`.

- Directory is scanned on **plugin startup** and whenever **Encounter Pack
  Directory** config changes.
- Invalid packs are **skipped with a log warning** — they never break the
  plugin.
- Debug overlay pack lines look like:
  `nex_1.0.0.zip -> nex@1.0.0 [LOADED]`

To author or troubleshoot packs, see [`docs/examples/ENCODING.md`](examples/ENCODING.md).

Shipped example packs live in this repo under `encounter-packs/`
(`nex`, `inferno`, `toa`, `cox`, `sotetseg`, `template`).

## 3. Configuration

Open **Settings → Plugins → Coach**. Settings apply immediately (no restart).

### General

| Setting | Default | Effect |
|---------|---------|--------|
| Enabled | on | Master switch; off blocks all callouts and overlays |
| Debug Mode | off | Debug overlay + record all game events |
| Log To File | off | Write debug entries to `<RuneLite>/coach/logs/coach-debug.log` (requires Debug Mode) |
| Encounter Pack Directory | `<RuneLite>/coach/encounters` | Folder of pack zips; changing reloads packs |
| Mute All | off | Silence all audio (visuals still show) |
| Master Volume | 70 | Base audio volume 0–100 |
| Critical / Warning / Info Volume | 100 / 80 / 60 | Per-category multipliers 0–100 |

### Callout categories

| Setting | Default | Effect |
|---------|---------|--------|
| Critical / Warning / Info / Transition callouts | all on | Show + speak that category |
| Essential Only | off | Only **critical** callouts (silences the rest) |
| Disabled Bosses | *(empty)* | Comma-separated boss ids to mute, e.g. `nex, inferno` (case-insensitive) |

Uncategorised callouts are treated as **info**.

### Overlay

| Setting | Default | Effect |
|---------|---------|--------|
| Show Prayer Indicator / Prayer Indicator | on | Large flashing prayer guidance |
| Show Countdown / Countdowns | on | Countdown when a predicted mechanic is ≤5 ticks out |
| Show Timeline / Phase Timeline | on | Boss phase progress bar |
| Show Status / Status Indicator | on | HP percentage in the coach panel |
| Show Mini HUD / Mini HUD | on | Compact persistent encounter line |

### Accessibility

| Setting | Default | Effect |
|---------|---------|--------|
| Accessibility Mode | BOTH | `BOTH` / `AUDIO_ONLY` (hide visuals) / `VISUAL_ONLY` (no audio) |
| Essential Only | off | Critical-only (also under callouts) |
| High Contrast Palette | off | WCAG AA-friendly overlay colours |
| Text Scale (%) | 100 | Overlay text size 50–200% |

### Profiles & debug

| Setting | Default | Effect |
|---------|---------|--------|
| Profiles (internal) | seeded once | Named presets (Learning / Practice / Performance) via ProfileManager |
| Debug Tab | EVENTS | Which debug view: `EVENTS`, `TRIGGERS`, `STATE`, `TIMELINE`, `PROFILING` |

Disabling Debug Mode exports a debug bundle to
`<RuneLite>/coach/debug_logs` when debugging is turned off.

## 4. How a fight feels

1. Enter a zone with a loaded pack’s boss NPC.
2. Phase entry triggers start a session; mechanics arm on trigger fires.
3. Callouts fire on **exact ticks** (1 tick = 600 ms), with optional
   `audioOffset` / `visualOffset` (−5…+10 ticks).
4. Overlay pieces (prayer flash, countdown, timeline, mini HUD) update live.
5. Phase exit triggers (or the last phase) advance the encounter.

## 5. Troubleshooting

| Symptom | Likely cause | What to do |
|---------|--------------|------------|
| No callouts at all | Plugin or category off | Enable **Enabled**; check category toggles; clear **Essential Only** / **Disabled Bosses** |
| Pack line `REJECTED` | Schema/audio validation failed | Open client log; fix pack per `ENCODING.md` (all errors listed in one line) |
| Pack line `CONFLICT` | Duplicate packId/bossId | Remove or rename the losing zip (alphabetical first wins) |
| Visuals but no audio | Mute All, `VISUAL_ONLY`, missing/bad file, or `.ogg` only | Check Mute All + mode; prefer `.wav`; confirm file exists in pack `audio/` |
| Audio file missing at load | Referenced `audioFile` not in zip | Whole pack rejects — add the file or remove the reference |
| Wrong timing | Offsets or pack content | Adjust pack offsets in ticks; re-verify in fight (rule 6) |
| Overlay missing | Overlay registration failed or mode hides visuals | Check client log for overlay warning; ensure mode is not `AUDIO_ONLY` |
| Nothing loads | Pack directory empty/missing | Create `<RuneLite>/coach/encounters` and drop zips; or fix Encounter Pack Directory setting |
| Debug wants file logs | Debug off / path wrong | Enable **Debug Mode** then **Log To File** → `coach/logs/coach-debug.log` |

**Design guarantees:** invalid packs never crash the plugin; audio failures
never block visual coaching; all callouts are advisory.

## 6. Related docs

- Pack author guide: [`docs/examples/ENCODING.md`](examples/ENCODING.md)
- Developer setup: [`docs/DEVELOPER_SETUP.md`](DEVELOPER_SETUP.md)
- Architecture / rules: [`docs/master-architecture.md`](master-architecture.md)
- Project overview: [`README.md`](../README.md)
