# Happy Ghast+

Fabric mod for Minecraft 26.3 with extras for the happy ghast. Works on its own; install it on the server too when
playing multiplayer, and on the client to see the chest and rocket engines. It started out as part of
[Vanilla+ Tweaks](../vanilla-tweaks/README.md) and works with or without it.

## Features

- **Chest:** right-click a happy ghast with a chest to equip a double chest (54 slots), or with an ender chest to
  carry your ender chest; sneak + right-click to open it. The chest shows on top of the ghast, towards its back.
  Contents and chest drop when the ghast dies.
- **Speed upgrade:** right-click a happy ghast with it to double its flying speed (permanent; drops on death).
  An upgraded ghast wears a firework rocket (a 3D take on the vanilla item) on each side, strapped to a leather pad on the harness, that trails
  small flames and smoke, with a glowing nozzle, while it flies forward.
- **Whistle:** calls every happy ghast you have ridden within 128 blocks to fly to you (3 s cooldown).
  Only ghasts in loaded chunks can hear it.
- **Rope ladder:** use the whistle while a happy ghast is within 8 blocks sideways and it lets a rope ladder down on
  its right-hand side, from its back to the ground (a farther ghast flies over and drops it on arrival). The ladder is
  drawn on the ghast, turns with it and has no blocks; hold jump to climb up, sneak to hold on. Riding pulls it up,
  dismounting lets it down again. The ghast holds still while it is down, and it is pulled up after a minute with
  nobody near it.
- **Protection:** players (and their arrows) can't hurt a harnessed happy ghast or the one they ride.
- **Searchlight:** right-click a harnessed happy ghast with it to mount a swivelling spotlight on top of the harness
  (sneak + shears takes it off). While riding, press G (rebindable: Toggle Ghast Searchlight) to switch it on. It
  turns to wherever you look and lights that spot up to 48 blocks away with a round pool of real light (invisible
  light blocks that follow the beam a few times a second and clean themselves up), plus a faint beam and a soft glow
  drawn on screen. It switches off when nobody steers the ghast and drops when the ghast dies.
- **Ghast nest:** a 6x6 straw nest (place it on the ground; it needs a free 6x6 area without the corners). A harnessed
  happy ghast that hovers slowly within 6 blocks above it and about 3 blocks of its middle settles in, and a few
  seconds later closes its eyes (the smile stays) and slowly heals. Riding it, hurting it or whistling wakes it up.
  Only the low straw floor is solid; you walk through the rim. Breaking any part removes the whole nest and drops it.
  A bucket of powder snow on the rim marks the front of the nest.

## Recipes

| Result | Recipe |
|---|---|
| Ghast speed upgrade | `IFI` / `SRS` / `IFI` (iron ingot, firework rocket, sugar, redstone block) |
| Ghast whistle | Iron ingot + snowball + string (shapeless) |
| Ghast searchlight | `IPI` / `IGI` / ` I ` (iron ingot, glass pane, glowstone) |
| Ghast nest | `HSH` / `SHS` / `HSH` (hay bale, stick) |

## Coming from Vanilla+ Tweaks 1.4.0

The old `vplustweaks:` whistle, speed upgrade and rope ladder ids are aliased to the new ones, and ghasts keep their
chests, owners and upgrades. Replace Vanilla+ Tweaks 1.4.0 with 1.5.0 when you add this mod: both 1.4.0 and this
mod would register the same things.
