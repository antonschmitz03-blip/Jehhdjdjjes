# Traveler's Backpack Addons

> This repo also contains a second, independent mod: [Vanilla+ Tweaks](vanilla-tweaks/README.md).

Fabric addon for [Traveler's Backpack](https://github.com/Tiviacz1337/Travelers-Backpack) (Minecraft 26.3, Traveler's Backpack 11.4.1).

## Upgrades

### Tool Swapper Upgrade

When you start breaking a block while holding a tool that is unsuitable for it, the upgrade swaps it
for the first suitable tool in your worn backpack (tool slots first, then storage). The tool you were
holding goes into the slot the new one came from.

- Only tools are swapped: pickaxes, axes, shovels, hoes and shears (item tag `tbaddons:swappable_tools`,
  can be changed with a datapack). Swords and other weapons are never touched, and nothing happens
  when you hold a non-tool item or nothing at all.
- "Suitable" means: can harvest the block (for blocks that need a specific tool, e.g. ores), otherwise
  an effective tool for it (e.g. a shovel for dirt). Mining speed and enchantments are ignored, so a
  silk touch iron pickaxe is never swapped for a diamond pickaxe on stone.
- The backpack must be worn. The upgrade has the usual on/off toggle in the backpack screen.
- Disabled in creative mode.

Recipe (shaped):

```
Iron Pickaxe | Blank Upgrade | Iron Axe
Redstone     | Iron Shovel   | Redstone
```

### Stack Upgrades

Multiply the stack size of every stackable item in the backpack's main storage (not the tool slots):

| Upgrade | Multiplier | 64-stack items | Recipe |
|---|---|---|---|
| Iron Stack Upgrade | x2 | 128 | Blank Upgrade surrounded by 8 Iron Blocks |
| Gold Stack Upgrade | x4 | 256 | Blank Upgrade surrounded by 8 Gold Blocks |
| Diamond Stack Upgrade | x8 | 512 | Blank Upgrade with 4 Diamond Blocks around it (plus shape) |

- Unstackable items (tools, armor, ...) stay at 1. 16-stack items become 32 / 64 / 128.
- If several are installed, the highest one counts. Works while the backpack is worn, held or placed.
- Taking items out always gives at most one normal stack (64) at a time, so the player inventory never
  gets oversized stacks. Number-key swaps move one normal stack; swapping a different item onto an
  oversized stack is blocked.
- The backpack's sort button merges stacks up to the upgraded size (e.g. 128 with the Iron Stack Upgrade).
- Removing the upgrade keeps oversized stacks as they are (nothing is deleted); they just can't grow.

**Important:** Stacks above 99 are saved through mixins in this addon. If you remove the addon from a world
while backpacks still contain stacks above 99, Minecraft will drop those stacks when loading. Empty the
oversized slots (or remove the stack upgrade and take items out) before uninstalling.

### Soulbound Upgrade

You keep the backpack you are wearing when you die: it is neither dropped nor placed as a block at the
spot where you died, and you are wearing it again after respawning. The upgrade has the usual on/off toggle.

- Works with Traveler's Backpack's own backpack slot and with the Trinkets back slot (the default when
  Trinkets is installed). The Accessories back slot is not supported yet.
- Grave mods are bypassed for a soulbound backpack, since it never leaves you.

Recipe (shaped):

```
Ender Pearl | Soul Sand/Soil   | Ender Pearl
Soul Sand   | Blank Upgrade    | Soul Sand
Ender Pearl | Totem of Undying | Ender Pearl
```

### Reverting backpack variants

Put any special backpack (spider, bat, dragon, iron, ...) alone into a crafting grid to turn it back
into a standard Traveler's Backpack. Contents, upgrades, tier, tanks and the sleeping bag are kept;
the items used to craft the variant are **not** returned. The list of variants is the item tag
`tbaddons:revertible_backpacks`. This uses Traveler's Backpack's own `backpack_shaped` recipe type,
which copies all backpack data onto the result.

## Backpack next to chests

While you wear a backpack, opening a chest, double chest, barrel, ender chest or shulker box shows the
backpack's storage as a panel to the right of the container. No upgrade is needed.

- Click and drag items between the chest, your inventory and the backpack as usual.
- Shift-click in the backpack panel moves the item into the chest (or your inventory if the chest is full).
  Shift-click in the chest or your inventory works as in vanilla.
- Stack Upgrades apply to the panel slots as well; backpacks and blacklisted items can't be put in.

## Tom's Simple Storage terminals

Only active when [Tom's Simple Storage](https://github.com/tom5454/Toms-Storage) (2.12.1) is installed;
the addon works without it. (This used to be the separate mod Backpack Storage Link; remove its jar.)

Every Tom's terminal (Storage Terminal, Crafting Terminal, Wireless and Advanced Wireless Terminal) also
lists the items in your backpacks while you use it:

- **Which backpacks:** the one you wear (back slot or Trinkets slot) and every backpack item in your
  inventory, hotbar and offhand. Only the main storage counts, not the tool slots. Backpacks inside other
  containers are not included.
- **Display:** backpack items are merged into the normal list. The tooltip shows how many of them are in
  your backpacks.
- **Taking items:** the network is used first; only what it can't supply comes from your backpacks.
- **Putting items in:** always goes into the network, never into a backpack.
- **Crafting Terminal:** crafting, grid refill and recipe filling (recipe book, JEI) can use backpack items.
- **Toggle:** a backpack button below the terminal's other buttons turns it on or off. The setting is per
  player and saved; it starts on.
- **Multiplayer:** each player only sees and uses their own backpacks.

Tom's Simple Storage has no API for this, so the mod hooks into its terminal menu and may need an update when
Tom's Simple Storage changes.

## Building

Requires JDK 25.

```
./gradlew build
```

The jars end up in `build/libs/` (backpack addon) and `vanilla-tweaks/build/libs/` (tweaks). GitHub Actions builds it on every push as well (artifact
`travelers-backpack-addons`). Install it next to Traveler's Backpack, Fabric API and Forge Config API Port.

`libs/` contains the Traveler's Backpack jar (LGPL-3.0, by Tiviacz1337), the Trinkets Updated jar (MIT) and the
Tom's Simple Storage jar,
used only for compiling and the CI smoke test.
The upgrade texture is derived from Traveler's Backpack's blank upgrade texture.
