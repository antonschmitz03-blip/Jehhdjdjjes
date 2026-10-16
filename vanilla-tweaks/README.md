# Vanilla+ Tweaks

Small Fabric tweaks for Minecraft 26.3. Works on its own; Enchanting Infuser support is optional.
Install on the server too when playing multiplayer.

## Features

- **Double doors:** opening or closing one door also toggles the matching door next to it
  (same facing, opposite hinge). Sneak-click to use a single door. Iron doors are not affected.
- **No crop trampling:** landing on farmland no longer turns it into dirt (fall damage is unchanged).
- **No "Too Expensive!" in the anvil:** the 40-level cap is gone; you still pay the full level cost.
- **Cheaper enchanting:**
  - Enchanting table: the level *requirement* is halved (e.g. 30 → 15). Enchantments are as strong as
    in vanilla, and the levels/lapis spent (1–3) are unchanged.
  - Enchanting Infuser: its level cost is cut to a quarter (rounded up, so a cost of 1 stays 1). Refunds for
    removing enchantments are unchanged.
- **Any enchantments combine:** Sharpness with Smite, Fortune with Silk Touch, all Protections and so on, on the
  anvil, at the enchanting table and in the Enchanting Infuser. Only Riptide with Loyalty or Channeling stays
  blocked, because a Riptide trident is never thrown.
- **Pets are safe from you:** you (and your arrows) can't damage your own tamed wolves, cats, horses and
  other pets.
- **Best horses:** every horse spawns and loads with maximum health (30), speed and jump strength.
- **Fast ladders:** right-click a ladder with a ladder to add one at the bottom of the column (or the top
  while sneaking), no matter how long it is.
- **Floating ladders:** ladders can be placed anywhere, with no block behind them.

The happy ghast features (chest, speed upgrade, whistle, rope ladder, protection) now live in their own mod, [Happy Ghast+](../happy-ghast/README.md). The two work alone or together.

## Config

`config/vplustweaks.json` is created on first launch with one on/off switch per feature, all on by default
(`double_doors`, `no_crop_trampling`, `no_anvil_too_expensive_limit`, `cheaper_enchanting_table`,
`cheaper_enchanting_infuser`, `free_enchantment_combining`, `safe_tamed_pets`, `max_stat_horses`,
`fast_ladder_placement`, `floating_ladders`, and `recipe_*` switches for the extra recipes). Set one to `false`
and restart the game to turn that feature off. New switches are added to the file when the mod gets new
features. In multiplayer, set them on the server and on the clients.

## Recipes

| Result | Recipe |
|---|---|
| Leather | Smelt / smoke / campfire-cook rotten flesh |
| Saddle | `LLL` / `LIL` (leather, iron ingot) |
| Gunpowder (x2) | Coal or charcoal + sugar + flint (shapeless) |
| Trident | `CHC` / ` I ` / ` I ` (prismarine crystal, heart of the sea, iron ingot) |
| Name tag | Paper + string + iron nugget (shapeless) |
| Iron / gold / diamond horse armor | `X X` / `XWX` / `X X` (material, any wool) |
| Chainmail armor | Armor shapes made of iron chains |
| 16 sticks | 2 logs stacked vertically |
| Plain glass, glass panes, terracotta, white wool, white carpet | 8 coloured blocks around a water bucket (bucket is returned) |
| Candle / white bed | Any coloured candle or bed + water bucket (shapeless, bucket returned) |
