# Vanilla Arsenal

New weapons that feel like they were always in the game. Install on the server and the client (the client needs
it for the item and its textures). Starts with the sling.

- **Sling** (shapeless: 2 string + leather): hold right-click to swing it, release to throw. A full draw takes one
  second. Ammo comes from the off hand first, then the inventory. Takes Unbreaking and Mending, repairs with leather.
  250 durability, one use per throw.
- Stones fly at 4.5 blocks per tick (a bow tops out at 3.0) and almost flat, and hit for 10 at full draw (a fully
  drawn bow does 6 to 9).
- **Ammo:** any stone block (stone, cobblestone, andesite, granite, diorite, tuff, blackstone, basalt, sandstone,
  end stone, nether bricks ... see the `vanillaarsenal:sling_ammo` tag) does 10. Deepslate and all its variants
  (`vanillaarsenal:sling_ammo_deepslate`) do 12. Gravel throws 3 pellets for 4 each. Snowballs do no damage but
  knock mobs back hard.
- Thrown stones are vanilla snowballs carrying the block item, so they need no client code and never break blocks.
