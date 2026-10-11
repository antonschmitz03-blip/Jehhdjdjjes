# Mining Dimension

A Fabric mod for Minecraft 26.3 that adds a sealed cave dimension made for mining.

- **Getting there:** build a nether-portal-style frame out of **chiseled deepslate** (at least 4x5, corners optional)
  and light it with **flint and steel**. Portals work in the overworld and the mining dimension only.
- **Coordinates are 1:1** with the overworld. Walking through builds a matching portal on the other side if none
  exists within 64 blocks (in a small cleared room in the mining dimension, on the surface in the overworld).
- **Terrain:** y -64 to 191, bedrock floor and roof, stone above y 0 and deepslate below. Caves use vanilla's
  cheese, spaghetti and pillar caves (no noodle caves). No water, no lava, no aquifers, no carvers.
- **Ores:** every vanilla overworld ore (coal, iron, copper, gold, redstone, lapis, diamond) at the vanilla heights,
  with **3x the ore per vein** and **2x as many veins**, so about 6x the ore of the overworld. The mountain-only
  coal and iron layers are left out because the whole dimension is solid rock. Large copper and iron veins generate
  like in the overworld.
  Amethyst geodes and glow lichen are included.
- **Mobs:** vanilla cave spawning (zombies, skeletons, spiders, creepers, slimes, endermen, witches, bats).
- Beds set your spawn but can't be slept in.

Everything except the portal is data in `src/main/resources/data/miningdim/`, so it's easy to tune
(vein sizes in `worldgen/feature`; note that block count grows faster than the `size` value, counts and heights in `worldgen/placed_feature`).
