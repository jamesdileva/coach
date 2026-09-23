# Project Coach — Beta Guide

How to install the beta, test it, and report issues.

**Status:** Beta prep complete for **v1.0.0** (`v1.0.0` tag). Hub publish is
maintainer-driven (see [`release/hub/README.md`](../release/hub/README.md)).
Until the hub PR merges, install from a **local JAR** or a **GitHub
pre-release** if provided.

Callouts are always **advisory** — the plugin never simulates input.

---

## 1. Who should join

- OSRS PVMers who fight **Nex, Inferno, ToA, CoX, Sotetseg** (or want to)
- Comfortable installing an external RuneLite plugin and filing GitHub issues
- Can play **3+ sessions** over the beta window (~2 weeks) and verify packs

Recruitment channels (maintainer): RuneLite Discord, OSRS Discord, clan chats.

---

## 2. Install (pick one)

### A. Plugin Hub (when live)

1. RuneLite → **Settings → Plugins** → plug icon (hub) → search **Coach**
2. Install → enable → confirm no client console errors

Hub file lives in [runelite/plugin-hub](https://github.com/runelite/plugin-hub)
as `plugins/coach` (maintainer PR).

### B. Local JAR (sideload — works today)

RuneLite only loads local JARs from **`sideloaded-plugins`** when the client
is started with **`--developer-mode`**. Without that flag the jar is ignored.

1. **Build or obtain** `coach-1.0.0.jar`
   - From this repo: `powershell -File release\run.ps1`  
     (or from `plugin/`: `$env:JAVA_HOME = "$env:USERPROFILE\tools\jdk\jdk-11.0.32+9"; .\gradlew.bat --no-daemon jar` → `plugin\build\libs\coach-1.0.0.jar`)
   - Or download a release asset if the maintainer attaches one
2. **Copy the JAR** (not a folder) into:

   ```
   %USERPROFILE%\.runelite\sideloaded-plugins\coach-1.0.0.jar
   ```

   Create `sideloaded-plugins` if it does not exist. Do **not** put it in
   `.runelite\plugins\` (that folder is for Plugin Hub installs).
3. **Enable developer mode** (persistent):
   - Start Menu → **RuneLite (configure)**
   - Under **Client arguments**, add one line: `--developer-mode`
   - Optional but useful for beta: also add `--debug` and JVM arg `-ea`
   - Save / close the configure window
4. **Fully quit RuneLite** (client **and** launcher) and start it again —
   plugins only load at startup; a running client will not pick up a new jar.
5. **Settings → Plugins** → search **Coach** → enable it. Expect no red
   error in the client console / `client.log`.

**Verify sideload worked:** `client.log` should contain a line like
`Side-loading plugin coach-1.0.0.jar`. If that line is missing, developer
mode is off or the jar is in the wrong folder.

Hub install (option A) does **not** need `--developer-mode`.

### C. Encounter packs (required for coaching)

Packs are **not** inside the JAR. Copy zips into:

```
%USERPROFILE%\.runelite\coach\encounters\
```

(i.e. `<RuneLite dir>\coach\encounters\` where `<RuneLite dir>` is usually
`%USERPROFILE%\.runelite`.)

Beta pack sources / zips in this repo under `encounter-packs/`
(`nex_1.0.0.zip`, `inferno_1.0.0.zip`, `toa_1.0.0.zip`, `cox_1.0.0.zip`,
`tob_sotetseg_1.0.0.zip`). After restart, debug overlay should show
`… -> packId@version [LOADED]`.

---

## 3. What to test

### Settings smoke (5 min)

| Check | Expect |
|-------|--------|
| Enabled on/off | Callouts/overlays stop/start |
| Mute All | Silence; visuals remain |
| Category toggles | Critical/Warning/Info/Transition independent |
| Encounter Pack Directory change | Packs reload |
| Accessibility modes | `AUDIO_ONLY` / `VISUAL_ONLY` behave |
| Debug Mode + Log To File | `coach/logs/coach-debug.log` grows |
| Disable Debug Mode | Bundle under `coach/debug_logs/` (if data collected) |

### Pack verification (roadmap AC: ≥3 packs in real fights)

Use each pack’s README checklist (`encounter-packs/<pack>/README.md`).

**Minimum beta set (recommended):**

1. **Nex** — phase entry, shout triggers, prayer callouts, audio `.wav`
2. **Inferno** — wave clear / Jad / Zuk terminal phases
3. **ToA** or **CoX** or **Sotetseg** — multi-boss or maze mechanics

For each pack record:

- [ ] Pack `[LOADED]` (not REJECTED/CONFLICT)
- [ ] Phase timeline advances on entry/exit
- [ ] ≥3 mechanics fire on the **correct tick** (1 tick = 600 ms)
- [ ] Visual **and** audio for critical callouts (or document audio-only gap)
- [ ] Cooldown prevents spam
- [ ] Death/despawn resets encounter cleanly
- [ ] Boss disable setting mutes only that boss

### Known limitations (do not re-file unless worse)

- Ship **`.wav`** callouts (all bundled packs do). A third-party pack that
  is `.ogg`-only will validate but **not play** (no Ogg decoder).
- Some condition types validate but evaluate false (prayer/inventory/region/custom)
- Pack NPC/animation ids may need live verification (rule 8)

---

## 4. Report issues

**Primary channel:** GitHub Issues on
[`jamesdileva/coach`](https://github.com/jamesdileva/coach/issues)

| Type | Template |
|------|----------|
| Bug / crash / wrong tick | **Bug report** |
| Pack verification / missing mechanic | **Pack verification** |

Labels to use/maintain: `bug`, `pack`, `critical`, `beta`.

### Include in every bug report

1. Plugin version (`coach-1.0.0` / commit) and RuneLite version  
2. Boss + pack `packId@version` + phase/mechanic id if known  
3. Expected vs actual callout (text / tick)  
4. Overlay vs audio (which failed)  
5. Steps to reproduce; **debug bundle** if possible  
   (`coach/debug_logs/` after toggling Debug Mode off)  
6. Screenshot of debug overlay `TRIGGERS` / `TIMELINE` tab if timing-related  

**Critical** = crash, wrong-tick on a safety callout (e.g. prayer), pack won’t
load for everyone, or total audio+visual failure mid-fight.

Discord chat is fine for discussion; **file an issue** so it is tracked.

---

## 5. Feedback summary (maintainer)

Categorized notes go under [`docs/beta/feedback/`](beta/feedback/README.md)
after the beta window — common pain points, requested features, action items
for Sprint 34.

---

## 6. Related docs

- Player install/settings: [`USER_GUIDE.md`](USER_GUIDE.md)
- Pack authoring: [`examples/ENCODING.md`](examples/ENCODING.md)
- Hub PR steps (maintainer): [`../release/hub/README.md`](../release/hub/README.md)
- Changelog: [`../CHANGELOG.md`](../CHANGELOG.md)
- Worklog / status: [`worklog.md`](worklog.md)
