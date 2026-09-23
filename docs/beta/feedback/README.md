# Beta feedback summaries

Human-written intake for the community beta (Sprint 33). One file per
summary batch (e.g. `2026-10-06_week1.md`), filled from GitHub Issues and
Discord after testers report.

**Do not** put raw issue dumps here without categorization.

## Categories

| Category | Meaning |
|----------|---------|
| `critical` | Crash, wrong-tick safety callout, pack unloadable, total callout failure |
| `pack` | Missing/wrong mechanic, bad NPC id, audio file, schema/validation |
| `ux` | Overlay clarity, settings discoverability, accessibility |
| `perf` | Tick budget, memory, audio latency |
| `docs` | Guide/changelog/API mismatches |
| `feature` | Requested enhancements (not bugs) |

## Files

| File | Purpose |
|------|---------|
| [`TEMPLATE.md`](TEMPLATE.md) | Copy for each summary |
| `YYYY-MM-DD_weekN.md` | Actual summaries (create when data exists) |

## Process

1. Collect GitHub Issues labeled `beta` / `bug` / `pack`
2. Note pack verification results (≥3 packs — see [`../../BETA_GUIDE.md`](../../BETA_GUIDE.md) §3)
3. Write a summary from `TEMPLATE.md`
4. Triage criticals → fix before Sprint 34 release notes
