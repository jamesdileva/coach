# Nex Encounter Pack

Five-phase Nex coaching pack: Smoke â†’ Shadow â†’ Blood â†’ Ice â†’ Zaros.

## Detection model

| Signal | Trigger type | Reliability |
|--------|-------------|-------------|
| Phase transitions | `hp` thresholds at 80/60/40/20% | High â€” HP-driven by design |
| Special attacks | `shout` â€” Nex announces every special via chat | High â€” shouts are the game's own telegraph |
| Zaros Wrath | `npc_despawn` on death | Medium â€” despawn timing vs. wrath window needs live confirmation |

## Human verification checklist (rule 8 â€” required before community release)

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
- [ ] Audio plays correctly through the plugin (PCM `.wav`).

## Audio

14 callouts generated with edge-tts (`en-US-GuyNeural`, +15% rate), converted
to mono 44.1kHz PCM `.wav` via ffmpeg — see `../generate_nex_audio.py`.
Regenerate any time callout text changes.

Note: Java Sound has no Ogg decoder — packs must ship `.wav` or audio will
not play (visuals still fire).

## Known limitations

- Drag (Smoke) has no telegraph â€” not detectable yet.
- Embrace Darkness end / Ice Prison shatter timers need tick-timer mechanics +
  condition support (post-v1.1).
