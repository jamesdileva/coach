# Tombs of Amascut Pack

ToA v1: HP-driven mechanic warnings for all four paths + the Wardens.

| Boss | Covered mechanics | Trigger basis |
|------|-------------------|---------------|
| Zebak | Special-queue warnings at 85/70/55/40%, enrage at 25% | HP thresholds (wiki-verified) |
| Akkha | Shadow-phase warnings at 80/60/40/20% | HP thresholds (wiki-verified) |
| Ba-Ba | Knockback warnings at 66%/33% (Mind the Gap!) | HP thresholds (wiki-verified) |
| Kephri | Encounter-start guidance only (shield/swarm detection needs more data) | spawn |
| Wardens | Start guidance + phase-3 enrage at <5% (heals 20%) | HP threshold |

## Human verification checklist (rule 8)

- [ ] NPC ids: Zebak 11730, Akkha 11796, Ba-Ba 11778, Kephri 11719,
      Elidinis' Warden 11750, Tumeken's Warden 11791 — these are best-known
      values and NOT yet verified live. Wrong ids = silent no-callouts.
      Confirm via RuneLite dev tools (`!npc` / NPC examine) on a real raid.
- [ ] Warden ids: both wardens + their powered-up forms may use different
      ids per phase — extend npcIds lists if so.
- [ ] Akkha shadow thresholds: he goes invulnerable at exactly 80/60/40/20%
      but the warning should land slightly BEFORE the invuln to be useful;
      consider bumping thresholds a point or two after live testing.
- [ ] Zebak special warnings: specials are queued alternately (Roar/Waves);
      callouts are generic v1 — distinguishing them needs chat/gfx data.

## Known limitations

- Invocation-aware callouts ("Feeling Special?", path levels, Hard/Entry mode
  variants) need per-raid configuration — planned with Sprint 17 settings work.
- No attack-style prayer callouts for Akkha/Wardens yet: needs verified attack
  animation ids (Akkha's style tells are his overhead prayers, which change).
- Kephri shield/swarm phases and Ba-Ba boulder timing not covered in v1.
