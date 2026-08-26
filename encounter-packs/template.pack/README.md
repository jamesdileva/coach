# Community Pack Template — Pack Author Guide

Copy `template.pack/` as a starting point, rename things, delete what you
don't use. The full field-by-field reference lives in
`plugin/src/main/resources/schemas/encounter_schema_v1.json` (every property
is documented there). This guide is the workflow.

## Workflow

1. Copy `template.pack/` → `myboss.pack/`, edit `encounter.json`.
2. Replace every `TODO` value: real NPC ids, animation ids, shout texts.
3. Generate audio for each callout that has an `audioFile`
   (see `generate_nex_audio.py` for the edge-tts + ffmpeg pattern).
4. Package:

   ```
   # from inside myboss.pack/
   zip -r ../myboss_1.0.0.zip encounter.json audio/
   ```

5. Drop the zip into `.runelite/coach/encounters/`. Enable the plugin's
   Debug Mode — the overlay shows `[LOADED]` / `[REJECTED: reason]` per pack.
6. Verify in-game, then tick off your README verification checklist before
   sharing (rule 8: AI-assisted and unverified content must not be published).

## Trigger types (one example each in this template)

| Type | Detects | Key fields |
|------|---------|-----------|
| `npc_spawn` / `npc_despawn` | NPC appears/disappears | `npcId` or `npcIds` (any-of list) |
| `hp` | NPC health crosses % threshold (edge-detected) | `npcId(s)`, `hpThreshold`, `hpDirection` |
| `animation` | NPC/player plays an animation id | `npcId(s)`, `animationId` |
| `projectile` | Projectile seen moving (fires on spawn) | `projectId`, optional `srcNpcId` |
| `graphic` | Spotanim on actor OR AoE graphics object | `graphicId`, optional `npcId` |
| `shout` | Chat message contains text | `containsText`, optional `senderName` |
| `tick_timer` | Phase-tick hits `(tick - offset) % mod == 0` | `tickMod`, `tickOffset` |
| `player_state` | Local player animation or HP threshold | `animationId` or `hpThreshold`+`hpDirection` |
| `location` | Player ENTERS rectangular region | `minX/maxX/minY/maxY` |
| `wave_cleared` | Every NPC of a set spawned then died | `npcIds` |
| `composite` | AND/OR of children against the same event | `logic`, `children` |
| `custom` | Reserved — not yet evaluated | — |

## Mechanics

A mechanic = triggers + callouts (+ conditions + cooldown).

- Any trigger firing activates the mechanic **unless** a `conditions` entry
  fails (all conditions must hold; unknown types fail closed with a warning).
- `cooldown`: ticks before the mechanic may activate again.
- Callouts deliver when `activationTick + min(visualOffset, audioOffset)` is
  reached. Negative offsets fire *before* the trigger moment ("Pray X" 2 ticks
  early = `"audioOffset": -2`).

## Phases

Phases chain in list order: the phase's `exitTriggers` advance to the next
phase; the last phase is terminal. Entry triggers matter only for starting
the session.

## Rules you're agreeing to

1. No game logic in packs — data only (the runtime enforces what it can).
2. Every callout should have visual AND audio (ship the `.ogg` or omit
   `audioFile`; missing referenced files are rejected at load time).
3. Human-verify your ids/texts against the live client before publishing.
