# Alchemy-From-Scratch

- This repository is the only source for the new Alchemy implementation. Do not use or modify `/home/artt/Projects/Alchemy` as implementation input.
- Keep the project primitive-first until the user explicitly approves the mechanics and asks to move to final assets.
- Prefer the smallest sufficient change and existing Android/Compose platform capabilities; do not add frameworks or dependencies without a demonstrated need.
- Before claiming an interaction fix complete, verify observable behavior on an emulator/device. Build success alone is not acceptance.
- For workspace mechanics, verify real spawn, drag, overlap resolution, invalid-pair no-op, boundary deletion, tab round-trips, and persistence where relevant.
- Run `./gradlew qualityCheck testDebugUnitTest assembleDebug` for deterministic verification and the focused Android instrumentation tests for affected UI flows.
- Delegated agents must work only inside the isolated worktree supplied by Hermes, commit their changes there, and never `cd` back to the main checkout.
