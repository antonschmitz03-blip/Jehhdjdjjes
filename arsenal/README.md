# Vanilla Arsenal

New weapons that feel like they were always in the game. Install on the server and the client (the client needs
it for the item and its textures). Starts with the sling.

- **Sling** (shapeless: 2 string + leather): hold right-click to swing it, release to throw. A full draw takes one
  second. Ammo comes from the off hand first, then deepslate, then other stone, then gravel and snowballs from the inventory. Takes Unbreaking and Mending, repairs with leather.
  250 durability, one use per throw.
- Stones fly at 4.5 blocks per tick (a bow tops out at 3.0) and almost flat, and hit for 10 at full draw (a fully
  drawn bow does 6 to 9).
- **Ammo:** any stone block (stone, cobblestone, andesite, granite, diorite, tuff, blackstone, basalt, sandstone,
  end stone, nether bricks ... see the `vanillaarsenal:sling_ammo` tag) does 10. Deepslate and all its variants
  (`vanillaarsenal:sling_ammo_deepslate`) do 12. Gravel throws 3 pellets for 4 each. Snowballs do no damage but
  knock mobs back hard. TNT (off hand only) explodes on impact like lit TNT.
- Thrown stones are vanilla snowballs carrying the block item, so they need no client code and never break blocks.
- **Gale Bow** (3 breeze rods + 3 string, bow pattern): a normal bow (450 durability, all bow enchantments, repairs with
  breeze rods) with movement tricks while it is held:
  - **Long hop:** jump while sneaking to be thrown wherever you look, about 7 blocks when looking level (1 s cooldown).
  - **Air jump:** one extra jump per time in the air, about 3 blocks high, keeping your momentum.
  - **Focus:** while airborne, drawing the bow holds you in place and slows mobs and projectiles within 12 blocks to
    a crawl for up to 3 s. Arrows loosed meanwhile do +50% damage. Once per time in the air.
  While the bow is in your hand the jump key never opens an elytra, so the two don't fight.
  The movement runs on the server; the elytra rule is a small mixin on both sides.
