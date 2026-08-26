# Nex Encounter Pack

Five-phase Nex coaching pack: Smoke → Shadow → Blood → Ice → Zaros.

## Detection model

| Signal | Trigger type | Reliability |
|--------|-------------|-------------|
| Phase transitions | `hp` thresholds at 80/60/40/20% | High — HP-driven by design |
| Special attacks | `shout` — Nex announces every special via chat | High — shouts are the game's own telegraph |
| Zaros Wrath | `npc_despawn` on death | Medium — despawn timing vs. wrath window needs live confirmation |

## Human verification checklist (rule 8 — required before community release)

- [ ] Shout strings match live client text exactly (matched case-insensitively
      as substrings; verify punctuation differences don't break matching):
  - "Let the virus flow through you!"
  - "There is... NO ESCAPE!"
  - "Darken my shadow!" / "Fear the shadow!" / "Embrace darkness!"
  - "Flood my lungs with blood!" / "A siphon will solve this!" / "I demand a blood sacrifice!"
  - "Infuse me with the power of ice!" / "Contain this!" / "Die now, in a prison of ice!"
  - "NOW, THE POWER OF ZAROS!"
- [ ] Shout chat messages actually reach RuneLite's ChatMessage stream with a
      message type our handler receives (expected: GAME-type messages).
- [ ] HP thresholds align with mage-kill gating in real fights (thresholds are
      approximate to phase boundaries; mages gate progression in groups).
- [ ] Wrath callout timing acceptable relative to actual despawn/wrath window.
- [ ] Audio plays correctly through the plugin once Ogg decoding lands (Sprint 27).

## Audio

14 callouts generated with edge-tts (`en-US-GuyNeural`, +15% rate), converted
to mono 44.1kHz Vorbis `.ogg` via ffmpeg — see `../generate_nex_audio.py`.
Regenerate any time callout text changes.

Note: until Sprint 27's decoder integration the plugin will not play `.ogg`
files; visuals fire regardless (rule 5 preserved).

## Known limitations

- Drag (Smoke) has no telegraph — not detectable yet.
- Embrace Darkness end / Ice Prison shatter timers need tick-timer mechanics +
  condition support (post-v1.1).
