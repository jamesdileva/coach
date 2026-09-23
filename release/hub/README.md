# RuneLite Plugin Hub submission (maintainer)

Hub builds **from source** on this GitHub repo (`build=gradle` in
`runelite-plugin.properties`). You do **not** upload a jar to the hub — you
PR a two-line marker file.

Sprint 33 default: **prep only**. Fork/PR/merge is on you after beta smoke.

---

## 1. Preconditions

- [ ] Repo public: `https://github.com/jamesdileva/coach`
- [ ] `main` green: `gradlew --no-daemon check` + `buildRelease`
- [ ] Tag **`v1.0.0`** pushed (or the commit you want the hub to build)
- [ ] `runelite-plugin.properties` includes `build=gradle` and entry
      `plugins=com.coach.plugin.CoachPlugin` (already true on `main`)
- [ ] Optional: GitHub pre-release `v1.0.0-beta` with jar for local testers
      (hub still builds from the commit you pin)

Refresh the stub after you finalize the commit:

```powershell
# from plugin/
powershell -File ..\release\run.ps1
# or: .\gradlew.bat --no-daemon clean buildRelease
Get-Content build\release\hub-plugin.txt
```

Current stub shape (regenerate for your SHA):

```
repository=https://github.com/jamesdileva/coach.git
commit=<40-char SHA of the commit the hub should build>
```

Example on this tree at tag time:

```
repository=https://github.com/jamesdileva/coach.git
commit=675ec9315adade0063bacdd7fbeefd8c876147b4
```

---

## 2. Fork and branch

1. Open [runelite/plugin-hub](https://github.com/runelite/plugin-hub) → **Fork**
2. On your fork’s `master`:

```bash
git remote add upstream https://github.com/runelite/plugin-hub.git
git fetch upstream
git checkout -B coach upstream/master
```

3. Create **`plugins/coach`** (no extension) with:

```
repository=https://github.com/jamesdileva/coach.git
commit=YOUR_40_CHAR_SHA
```

4. Commit + push to **your** fork, branch `coach`.

---

## 3. Open the PR

1. On `runelite/plugin-hub` → **New pull request** → **Compare across forks**
2. head: `<you>/<repo>:coach` → base: `runelite:master`
3. Title/description: what Coach does (advisory PVM coaching, no input sim,
   data-only packs), link this repo + `docs/BETA_GUIDE.md`
4. Watch CI: **build** workflow + **RuneLite Plugin Hub Checks**
5. Fix review requests; update `commit=` if you push new commits to coach
   (hub builds that SHA, not `main` tip unless they match)

---

## 4. After merge (beta window)

- [ ] Confirm install path: RuneLite → Plugins → hub → **Coach**
- [ ] Point testers at [`docs/BETA_GUIDE.md`](../../docs/BETA_GUIDE.md)
- [ ] File issues with templates under `.github/ISSUE_TEMPLATE/`
- [ ] When updating the hub pointer: bump `commit=` only (same PR flow)

---

## 5. Optional GitHub pre-release (local jar path)

```powershell
# after buildRelease — attach plugin\build\release\coach-1.0.0.jar
gh release create v1.0.0-beta plugin\build\release\coach-1.0.0.zip `
  --prerelease --title "Coach 1.0.0-beta" --notes-file CHANGELOG.md
```

(Adjust to your `gh`/release process. Hub install does not require this.)

---

## 6. Suggested PR blurb

> **Coach** — real-time, tick-accurate OSRS boss coaching for RuneLite.
> Visual + audio callouts from data-only encounter packs (JSON schema 1.0).
> Advisory only: no input simulation, no network calls in the plugin.
> Packs load from the user’s `coach/encounters` directory (not bundled).
> Beta guide and issue templates: https://github.com/jamesdileva/coach

---

## Related

- Sprint 32 release pipeline: `plugin/build.gradle` → `buildRelease`
- Properties: `plugin/src/main/resources/runelite-plugin.properties`
- Hub upstream README: [runelite/plugin-hub](https://github.com/runelite/plugin-hub)
