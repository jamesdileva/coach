# Inferno Encounter Pack

All 69 waves + basic TzKal-Zuk, generated from the canonical wave table
(`../generate_inferno_pack.py`). Regenerate after editing the table:

```
python ../generate_inferno_pack.py
python ../generate_inferno_audio.py   # if callout text changed
```

## Detection model

| Signal | Trigger type | Notes |
|--------|-------------|-------|
| Wave cleared → next phase | `wave_cleared` (new) | Fires when every NPC of the wave's id set has spawned then died. Blob-split and Jad-healer ids are included in the relevant waves' sets so splits/healers must die too (matches real wave gating). |
| Attack prayer callouts | `animation` on mob attack anims | The core value: "Pray Ranged!" etc. at -1 tick audio offset |
| Zuk healers / mid-Zuk Jad | `npc_spawn` | Basic Zuk v1 |

## Human verification checklist (rule 8 — required before community release)

Animation + NPC ids are best-known community values, cross-checked across
OpenOSRS InfernoPlugin, InfernoTrainer assets, and boss-cooldown tick-counter
data — but NOT verified live by us:

- [ ] NPC id ranges: Jal-Nib 7690-7693, Jal-MejRah 7694, Jal-Ak 7695,
      Jal-ImKot 7696, Jal-Xil 7698, Jal-Zek 7699, JalTok-Jad 7700,
      Yt-HurKot 7701, Jal-MejJak 7702, blob splits 7703/7704/7705,
      TzKal-Zuk 7706
- [ ] Attack animations: Meleer 7597, Ranger ranged 7605 / melee 7604,
      Mager magic 7610 / melee 7612, Blob 7581/7583/7582,
      Jad magic 7592 / ranged 7593 / melee 7594, Zuk attack 7566
- [ ] Wave-clear detection works end to end (watch a few transitions in
      debug overlay; wrong Nibbler variant ids would stall waves)
- [ ] Mager revive spawns count toward wave clearing (they should — revived
      mobs re-spawn with the same ids)
- [ ] Callout cooldown (default 4 ticks) feels right during triple-Jad flicks

## Known limitations

- No safespot/tick-counter guidance (RuneLite-class feature set; post-v1)
- Zuk prayer styles not called out: his attack animation is identical for
  both styles, only the projectile differs — needs projectile triggers
- Bat drain callouts omitted (low threat, high noise)
