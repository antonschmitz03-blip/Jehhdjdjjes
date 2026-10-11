# Simple Graves

Fabric grave mod for Minecraft 26.3. Works on its own; Trinkets, Traveler's Backpack and Xaero's Minimap
support is optional. Install on the server too when playing multiplayer (the client part only adds the
Xaero waypoint).

## What happens when you die

- Everything you would have dropped goes into a **grave** (a small headstone) instead, along with the XP
  vanilla would drop. Nothing is left lying around to despawn, burn or fall into the void.
- Whatever stays with you stays out of the grave: `keepInventory`, a backpack with an enabled **Soulbound
  Upgrade**, Curse of Vanishing items (destroyed as usual), Trinkets slots set to "keep".
- **Placement:** the nearest free spot (within 16 blocks) with a solid, non-lava block underneath. If there is
  none (e.g. a Nether lava ocean), the grave sits on the lava surface. After falling into the void it is placed
  at the bottom of the world above where you fell.
- You get a chat message with the coordinates. With **Xaero's Minimap** installed, a "Grave" waypoint is added
  and replaces Xaero's own deathpoint for that death (no grave, e.g. with keepInventory: the deathpoint stays).

## Getting your items back

- **Right-click your grave.** Every item goes back into the slot it came from (hotbar, armor, offhand,
  Trinkets slots, Traveler's Backpack's back slot). If a slot is taken, the item goes into your inventory;
  what doesn't fit drops at your feet. You also get the XP back. The grave and its waypoint disappear.
- Only the owner can open a grave. It can't be broken in survival, blown up, pushed by pistons or destroyed
  by the Wither or the Ender Dragon. Graves never expire.
- If a grave is removed some other way (creative mode, `/setblock`), it drops its contents instead.

Backpacks keep their contents, tier and upgrades, since Traveler's Backpack stores all of that on the item.
