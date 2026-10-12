# Simple Waystones

A stripped-down Waystones for Fabric 26.3. Server-side logic only; install it on the server and the client.

- **Waystone** (crafted: amethyst shard on top, ender pearl in the middle surrounded by stone bricks). A new one is
  called "<biome> Waystone" (or the item's anvil name). The owner renames it with the name tag in its teleport list:
  that opens an anvil-style screen, type the name and click the result. Free.
- **Right-click** a waystone the first time to discover it. After that, right-click opens a list of every waystone
  you have discovered; click one to teleport next to it.
- **Cost:** free under 500 blocks (horizontal distance), then 1 level per 500 blocks, at most 3, plus 1 level for
  changing dimension. Free in creative. Adjustable in `config/simplewaystones.json`.
- **Warp Scroll** (shapeless: 2 paper + ender pearl + amethyst shard makes 4): right-click to pick any discovered waystone
  and go there for free. Used up on a successful teleport.
- Only the owner (or a player in creative mode) can break a waystone. Explosions can't destroy it. A waystone
  moved by a piston counts as a new one and has to be discovered again.
- Data lives in `simplewaystones.json` in the world folder.
