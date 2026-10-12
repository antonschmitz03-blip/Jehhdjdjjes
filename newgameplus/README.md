# New Game Plus

Fabric mod for Minecraft 26.3 (singleplayer). Start a new world with your player from another world.

## How to use

1. On the **Create World** screen, click **Import Player...** (bottom left, or above the Create/Cancel buttons
   on small windows).
2. Pick the world your player should come from and tick what comes along: **Items** and **XP** (on by
   default), **Ender Chest** (off by default). The button then shows "Import: <world>".
3. Create the world. On first join your player has the imported items and XP.

The source world is only read, never changed.

## What comes along

- **Items**: inventory, hotbar slot, armor, offhand, and player data from mods: Trinkets slots and Fabric
  attachments (that's where Traveler's Backpack keeps the worn backpack and its contents).
- **XP**: level, progress and score.
- **Ender Chest** (optional): the ender chest contents.

Position, dimension, spawn point, health, hunger, effects, game mode, recipes, advancements and stats stay
behind; you start fresh at the new world's spawn.

## Notes

- The import is stored in the new world (`newgameplus_pending.dat`) and applied when the first player
  without saved data joins, then renamed to `newgameplus_applied.dat`.
- Worlds from older Minecraft versions are upgraded with the game's own data fixer first.
- Items from mods the new world doesn't have are dropped by the game when it loads them.
