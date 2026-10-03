# Local visual generation

The source prompts for the local-only Alchemy art pass. Use the installed `game-asset` CLI described in `~/AI/AGENTS.md`.

Shared style:

`cohesive arcane alchemy fantasy mobile game, hand-painted semi-realistic illustration, dark sapphire and indigo shadows, antique brass and warm gold highlights, restrained luminous cyan and violet magic, crisp readable silhouette, premium collectible game art, rich materials, consistent soft rim light, no text, no letters, no watermark`

Use the draft FLUX.2 Klein 4B model first. It is the established default on this laptop; `--final` is slower and is not consistently better. Keep the full generated PNGs in `~/AI/GameAssets/output/`; copy only selected working sources into `assets/`.

Element icons are generated with `--sizes 256`; the `@256.png` exports are the repository sources. Backgrounds are generated at `768x1344`. Generate `bg_home` from text, choose one variant, then use `game-asset edit` from that chosen room for the other four rooms so their architecture remains coherent.

Two small deterministic post-processes are intentional because the model variants were less usable than generated geometry: `toggle_on.png` is the generated OFF switch flipped horizontally and brightened (`magick toggle_off.png -flop -modulate 135,120,100 toggle_on.png`), and the generated legendary frame uses the alpha mask of the generated base frame so its center remains truly transparent.
