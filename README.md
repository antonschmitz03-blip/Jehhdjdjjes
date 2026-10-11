# Traveler's Backpack Addons

Fabric addon for [Traveler's Backpack](https://github.com/Tiviacz1337/Travelers-Backpack) (Minecraft 26.3, Traveler's Backpack 11.4.1).

## Upgrades

### Tool Swapper Upgrade

When you start breaking a block while holding a tool, the upgrade swaps it for the best matching tool
from your worn backpack (tool slots first, then storage). The tool you were holding goes into the slot
the new one came from.

- Only tools are swapped: pickaxes, axes, shovels, hoes and shears (item tag `tbaddons:swappable_tools`,
  can be changed with a datapack). Swords and other weapons are never touched, and nothing happens
  when you hold a non-tool item or nothing at all.
- "Best" means: able to harvest the block (if the block needs a specific tool), then fastest mining speed.
- Tools with 1 durability left are skipped.
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
| Diamond Stack Upgrade | x8 | 512 | Blank Upgrade surrounded by 8 Diamond Blocks |

- Unstackable items (tools, armor, ...) stay at 1. 16-stack items become 32 / 64 / 128.
- If several are installed, the highest one counts. Works while the backpack is worn, held or placed.
- Taking items out always gives at most one normal stack (64) at a time, so the player inventory never
  gets oversized stacks. Number-key swaps move one normal stack; swapping a different item onto an
  oversized stack is blocked.
- Removing the upgrade keeps oversized stacks as they are (nothing is deleted); they just can't grow.

**Important:** Stacks above 99 are saved through mixins in this addon. If you remove the addon from a world
while backpacks still contain stacks above 99, Minecraft will drop those stacks when loading. Empty the
oversized slots (or remove the stack upgrade and take items out) before uninstalling.

## Building

Requires JDK 25.

```
./gradlew build
```

The jar ends up in `build/libs/`. GitHub Actions builds it on every push as well (artifact
`travelers-backpack-addons`). Install it next to Traveler's Backpack, Fabric API and Forge Config API Port.

`libs/` contains the Traveler's Backpack jar (LGPL-3.0, by Tiviacz1337) used only for compiling.
The upgrade texture is derived from Traveler's Backpack's blank upgrade texture.
