# Theatre of Blood — Sotetseg Pack

Sotetseg v1: encounter-start guidance + shadow-realm maze warnings.

## Detection model

| Signal | Trigger type | Reliability |
|--------|-------------|-------------|
| Encounter start | `npc_spawn` ids 8337/8388 | High — ids confirmed via RuneLite ToB damage-multiplier data |
| Maze #1 / #2 | `hp` thresholds below 67% / 34% | High — wiki-verified thresholds (66.6% / 33.3%) |

## Human verification checklist (rule 8)

- [ ] Both NPC ids spawn in your raid size (8337 vs 8388 scaling variants —
      triggers cover both, but confirm neither id is entry-mode-only)
- [ ] HP-threshold maze warnings fire close enough to the actual maze
      teleport (thresholds are approximate to 67%/34%)
- [ ] Confirm the highlighted big-ball chat message text (2018 update added
      a "highlighted message" notifying of the ball) so a `shout` trigger can
      be added for it

## Known limitations (v1)

- No attack callouts: Sotetseg's projectile/melee animation ids are not
  reliably sourced yet. His attacks are also unusual (splitting projectiles
  that disable protection prayers), so naive pray-callouts would mislead.
- No big-red-ball trigger: needs the verified chat message string (shout
  trigger) or projectile id.
- Maze path tracking itself is tile-tracking territory — out of scope for a
  data pack (needs engine spatial features).
