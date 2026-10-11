# Mining Dimension

A Fabric mod for Minecraft 26.3 that adds a sealed cave dimension made for mining.

- **Getting there:** build a nether-portal-style frame out of **chiseled deepslate** (at least 4x5, corners optional)
  and light it with **flint and steel**. Portals work in the overworld and the mining dimension only.
- **Coordinates are 1:1** with the overworld. Walking through builds a matching portal on the other side if none
  exists within 64 blocks (in a small cleared room in the mining dimension, on the surface in the overworld).
- **Terrain:** y -64 to 191, bedrock floor and roof, stone above y 0 and deepslate below. Caves use vanilla's
  cheese, spaghetti and pillar caves (no noodle caves). No lava, no aquifers, no carvers; the only water is in small clay pools.
- **Ores:** every vanilla overworld ore (coal, iron, copper, gold, redstone, lapis, diamond) at the vanilla heights,
  with **3x the ore per vein** and **2x as many veins**, so about 6x the ore of the overworld. The mountain-only
  coal and iron layers are left out because the whole dimension is solid rock. Large copper and iron veins generate
  like in the overworld.
- **Biomes:** five small cave biomes (roughly 25-50 blocks across, and they change with depth too):
  - *Mining Caves*: plain stone caves, with the occasional clay pool.
  - *Lush Caverns*: moss, azaleas, spore blossoms and lots of glow berry vines, and clay pools with dripleaves.
  - *Dripstone Caverns*: dripstone clusters, large stalactites and stalagmites.
  - *Crystal Caverns*: amethyst geodes about 8x as often and more shroomlight.
  - *Basalt Caverns*: basalt, smooth basalt and blackstone blobs, basalt columns and pillars, a few magma blocks
    (they burn when you walk on them, but there's no lava) and white ash in the air. It covers about a third of the map.
- **Mineshafts:** vanilla abandoned mineshafts (normal near the bedrock floor, badlands-style anywhere), with the
  usual rails, chests and cave spider spawners.
- **Light:** rare shroomlight clusters hang from cave ceilings (more of them in Crystal Caverns), plus glow berry
  vines everywhere and about twice the vanilla glow lichen.
- **Atmosphere:** each biome has a few drifting particles (dust motes, spore specks, water drips, purple sparkles),
  a faint fog tint (fog distance is unchanged) and rare ambient sounds (distant rumbles, rustling leaves,
  drips, amethyst chimes) on top of the normal cave sounds.
- **Mobs:** vanilla cave spawning (zombies, skeletons, spiders, creepers, slimes, endermen, witches, bats).
- Beds set your spawn but can't be slept in.

Everything except the portal is data in `src/main/resources/data/miningdim/`, so it's easy to tune
(vein sizes in `worldgen/feature`; note that block count grows faster than the `size` value, counts and heights in `worldgen/placed_feature`).
