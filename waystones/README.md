# Simple Waystones

A stripped-down Waystones for Fabric 26.3. Server-side logic only; install it on the server and the client.

- **Waystone** (crafted: amethyst shard on top, ender pearl in the middle surrounded by stone bricks). Rename the
  item in an anvil before placing to name it; otherwise it is called "<biome> Waystone". Sneak + right-click with a
  named name tag renames a placed one.
- **Right-click** a waystone the first time to discover it. After that, right-click opens a list of every waystone
  you have discovered; click one to teleport next to it.
- **Cost:** free under 500 blocks (horizontal distance), then 1 level per 500 blocks, at most 3, plus 1 level for
  changing dimension. Free in creative. Adjustable in `config/simplewaystones.json`.
- **Warp Scroll** (shapeless: paper + ender pearl + amethyst shard): right-click to pick any discovered waystone
  and go there for free. Used up on a successful teleport.
- Only the owner (or a player in creative mode) can break a waystone. Explosions can't destroy it. A waystone
  moved by a piston counts as a new one and has to be discovered again.
- Data lives in `simplewaystones.json` in the world folder.
