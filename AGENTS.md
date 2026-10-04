# Alchemy-From-Scratch

Offline Android element-mixing game (Kotlin/Compose, single module, no DI): 180 elements, 176 curated + 230 alternative recipes (406), local progress only — no network, accounts, ads, purchases or analytics.

- Prefer the smallest sufficient change and existing Android/Compose platform capabilities; do not add frameworks or dependencies without a demonstrated need.
- Don't change the 180 elements, the 176 curated recipes, the overlap rule or depth-based rarity. Alternative recipes may be added if they keep every element's rarity (checked at startup and in tests; `docs/adr/0001-alternative-recipes.md`). Not planned: levels, XP, scores, streaks, daily quests, cloud, accounts.
- Before claiming an interaction fix complete, verify observable behavior on an emulator. Build success alone is not acceptance.
- For workspace mechanics, verify real spawn, drag, overlap resolution, invalid-pair no-op, boundary deletion, tab round-trips, and persistence where relevant.
- Run `./gradlew qualityCheck testDebugUnitTest assembleDebug` for deterministic verification and the focused Android instrumentation tests for affected UI flows (`docs/testing.md`).
- The workspace and the first-run tip step are saved next to the progress (`ProgressStore.saveWorkspace`, `saveTipStep`) and survive restarts; instrumentation tests that launch the game must start from a known state — `SkipOnboardingRule` does that and restores the device's progress afterwards.
- Save imports are bounded to 256 KiB and run off the main thread; exporting writes the requested snapshot even when progress is reset. Keep transfer results mutually exclusive.
- Agents install and test only on an emulator, never on the owner's personal phone even if ADB sees it; the owner checks sound and haptics on the phone.
- Official Android skills (`android/skills`, installed and updated by the `android` CLI — don't hand-edit): `android-cli`, `android-profiler`, `r8-analyzer` in `.claude/skills/` (Claude Code) and `.agents/skills/` (Codex). Update both with `android skills update --all` before a release. Never start, stop or create emulators through `android emulator`; lease one with `android-emu` and pass `--device=<serial>` to every other `android` command. `r8-analyzer` points to `scripts/*.py` that upstream doesn't ship: run `./gradlew :app:analyzeReleaseR8Config` and read `app/build/reports/r8/r8-config-analyzer-release.html` instead.
- Each feature goes on its own `feat/<name>` branch from `master`; merge after verification and owner approval. No push, tags or releases without explicit permission; user-visible changes go to the «Не выпущено» section of `CHANGELOG.md`.
- Delegated agents must work only inside the isolated worktree supplied by Hermes, commit their changes there, and never `cd` back to the main checkout.

## Status
- Feature plan `docs/superpowers/plans/2026-09-29-feature-roadmap.md`: stages 1–9 merged into `master` (themes last, 2026-10-01).
- RuStore: 1.1.1 (versionCode 3) submitted and in moderation as the first publication (2026-10-02). 1.2.0 (versionCode 4) is released on GitHub (tag `v1.2.0`, signed with the current key; 1.1.0 on GitHub used an older key, so it cannot be updated in place) and goes to RuStore as the first update once 1.1.1 is approved; its notes are the 1.2.0 section of `CHANGELOG.md`. 1.2.1 (versionCode 5, single style, in-game language) is released on GitHub (tag `v1.2.1`, 2026-10-04).
- One visual style (Aether); themes and the in-app reduced-motion toggle were removed (`docs/adr/0002-single-style.md`). Motion follows the system animation scale; language is chosen in-game via `LocaleManager` (API 33+).
- Studio intro: 2.5s ARTT Studio video supplied by the owner (used as is) follows a plain black system starting window (no game branding); no manual skip, background pauses playback, reduced motion shows a still. It dips to black, then lifts onto the game, which is laid out underneath from 0.7s (touch and semantics blocked; music waits for the end).
- Branding notes and poster command: `ART_ASSETS.md`.
- Release target: RuStore (signed APK). Listing, 512 px icon, 9:16 screenshots in `docs/rustore/`, privacy policy `docs/privacy.md`. Release key: ~/keys/alchemy-release.jks (props in ~/.gradle/gradle.properties). Element texts are looked up by name — keep `res/raw/keep.xml` in sync or the shrunk release crashes.
