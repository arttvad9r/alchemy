# Alchemy-From-Scratch

Offline Android element-mixing game (Kotlin/Compose, single module, no DI): 180 elements, 176 recipes, local progress only — no network, accounts, ads, purchases or analytics.

- Prefer the smallest sufficient change and existing Android/Compose platform capabilities; do not add frameworks or dependencies without a demonstrated need.
- Don't change the catalog (180 elements, 176 recipes), the overlap rule or depth-based rarity. Not planned: levels, XP, scores, streaks, daily quests, cloud, accounts.
- Before claiming an interaction fix complete, verify observable behavior on an emulator. Build success alone is not acceptance.
- For workspace mechanics, verify real spawn, drag, overlap resolution, invalid-pair no-op, boundary deletion, tab round-trips, and persistence where relevant.
- Run `./gradlew qualityCheck testDebugUnitTest assembleDebug` for deterministic verification and the focused Android instrumentation tests for affected UI flows (`docs/testing.md`).
- Agents install and test only on an emulator, never on the owner's personal phone even if ADB sees it; the owner checks sound and haptics on the phone.
- Each feature goes on its own `feat/<name>` branch from `master`; merge after verification and owner approval. No push, tags or releases without explicit permission; user-visible changes go to the «Не выпущено» section of `CHANGELOG.md`.
- Delegated agents must work only inside the isolated worktree supplied by Hermes, commit their changes there, and never `cd` back to the main checkout.

## Status
- Feature plan `docs/superpowers/plans/2026-09-29-feature-roadmap.md`: stages 1–9 merged into `master` (themes last, 2026-10-01). No public release yet.
- Themes: blue art has pre-tinted `*_ember`/`*_verdant` copies built by `tools/build_ui_assets.py` (`THEMED_ART`), picked via `themedArt()`.
- Studio intro: 2.8s flat portrait ARTT animation follows a plain black system starting window (no game branding); no manual skip, background pauses playback, reduced motion shows a still.
- Branding sources and rebuild instructions: `ART_ASSETS.md`, `tools/build_studio_intro.py`.
