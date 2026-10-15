# Mining Dimension

A Fabric mod for Minecraft 26.3 that adds a sealed cave dimension made for mining.

- **Getting there:** build a nether-portal-style frame out of **chiseled deepslate** (at least 4x5, corners optional)
  and light it with **flint and steel**. Portals work in the overworld and the mining dimension only.
- **Coordinates are 1:1** with the overworld. Walking through builds a matching portal on the other side if none
  exists within 64 blocks (in a small cleared room in the mining dimension, on the surface in the overworld).
- **Terrain:** y -64 to 191, bedrock floor and roof, stone above y 0 and deepslate below. Caves use vanilla's
  cheese, spaghetti and pillar caves (no noodle caves). No lava, no aquifers, no carvers; the only water is in small clay pools.
- **Ores:** every vanilla overworld ore (coal, iron, copper, gold, redstone, lapis, diamond) at the vanilla heights,
  with vanilla vein sizes and about **1.5x as many veins** (diamonds only about 1.25x), so a bit more ore than the
  overworld. The mountain-only
  coal and iron layers are left out because the whole dimension is solid rock. Large copper and iron veins generate
  like in the overworld.
- **Biomes:** six small cave biomes (roughly 25-50 blocks across, and they change with depth too):
  - *Mining Caves*: plain stone caves, with the occasional clay pool.
  - *Lush Caverns*: moss, azaleas, spore blossoms and lots of glow berry vines, and clay pools with dripleaves.
  - *Dripstone Caverns*: dripstone clusters, large stalactites and stalagmites.
  - *Crystal Caverns*: amethyst geodes about 8x as often and more shroomlight.
  - *Basalt Caverns*: basalt, smooth basalt and blackstone blobs, basalt columns and pillars, a few magma blocks
    (they burn when you walk on them, but there's no lava) and white ash in the air. It covers about a third of the map.
  - *Meadow Caverns*: grass floors with flowers, tall grass and firefly bushes, glowing shroomlight set into the
    ground and hanging from the ceiling, fireflies in the air and meadow music. Cows, sheep, pigs, chickens and
    rabbits spawn here (the shroomlight keeps the grass bright enough for them).
- **Mineshafts:** vanilla abandoned mineshafts (normal near the bedrock floor, badlands-style anywhere), with the
  usual rails, chests and cave spider spawners.
- **Light:** rare shroomlight clusters hang from cave ceilings (more of them in Crystal Caverns), plus glow berry
  vines everywhere and about twice the vanilla glow lichen.
- **Atmosphere:** each biome has a few drifting particles (dust motes, spore specks, water drips, purple sparkles),
  a faint fog tint (fog distance is unchanged) and rare ambient sounds (distant rumbles, rustling leaves,
  drips, amethyst chimes) on top of the normal cave sounds.
- **Mobs:** vanilla cave spawning (zombies, skeletons, spiders, creepers, slimes, endermen, witches, bats),
  axolotls in the Lush Caverns clay pools, farm animals in the Meadow Caverns, and Pebblins everywhere.
- **Pebblins:** small, round stone critters with a crystal cluster on their back. Feed one a raw ore item
  (coal, raw iron, raw copper, raw gold, redstone dust, lapis, a diamond or an emerald) and its crystals turn that
  color: for a minute it waddles toward the nearest matching ore within 14 blocks, hops excitedly and points a
  sparkle trail and a chime at it. Mine it and it sniffs out the next one. If there's none nearby it puffs a sad
  little cloud. Adults now and then shed a flint or a few iron or gold nuggets. They breed with glow berries,
  follow you for ore or glow berries, never dig and never attack. Spawn egg in the Spawn Eggs tab.
- **Knockers:** mischievous mine goblins from Cornish folklore, with glowing eyes and a lamp on their miner's cap.
  They live in threes around dens: a little camp on the cave floor with a trodden dirt patch, an unlit campfire, straw beds, candles, cobwebs and their stash sack, which bulges fuller (up to overflowing) with every theft. You can hear a den before you see it: muffled giggles and knocks, and a full sack glints with gold. An empty den fills up with knockers again over time. A knocker
  sneaks up and pinches the most valuable stack in your hotbar (gems, ingots, raw ores, nuggets, ore blocks,
  enchanted books; never tools, weapons or armour), then runs off clutching it. Throw it any food and it hands
  the stack back; killed, it always drops it. Three food offerings to a den's knockers keep that den off your
  pockets for one in-game day, and while they like you they knock on the rock when monsters lurk nearby. They
  also pocket torches now and then (if mobGriefing is on), and hitting one makes its den-mates pelt you with
  pebbles. Breaking the sack spills its stash (the fuller, the richer: nuggets, raw ores, gold, lapis, the odd emerald) and makes its knockers furious.
  Spawn egg in the Spawn Eggs tab, den in Functional Blocks.
- Beds set your spawn but can't be slept in.

Everything except the portal is data in `src/main/resources/data/miningdim/`, so it's easy to tune
(vein sizes in `worldgen/feature`; note that block count grows faster than the `size` value, counts and heights in `worldgen/placed_feature`).
