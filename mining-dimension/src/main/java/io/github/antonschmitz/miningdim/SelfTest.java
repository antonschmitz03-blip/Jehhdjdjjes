package io.github.antonschmitz.miningdim;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.BlockUtil.FoundRectangle;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

/** CI smoke test: generates a few mining dimension chunks, logs what they contain and builds portals. */
final class SelfTest {
    private static final int CHUNKS = 6;
    private static final Set<String> DECOR = Set.of("shroomlight", "glow_lichen", "cave_vines", "cave_vines_plant", "moss_block",
            "azalea", "flowering_azalea", "spore_blossom", "dripstone_block", "pointed_dripstone", "amethyst_block", "amethyst_cluster");

    private SelfTest() {
    }

    static void run(MinecraftServer server) {
        ServerLevel mining = server.getLevel(MiningDimension.MINING);
        if (mining == null) {
            throw new IllegalStateException("miningdim self test: dimension missing");
        }

        Map<String, Integer> counts = new TreeMap<>();
        Map<String, Integer> decor = new TreeMap<>();
        int air = 0;
        int[] airByBand = new int[8];
        int fluids = 0;
        int total = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int cx = 0; cx < CHUNKS; cx++) {
            for (int cz = 0; cz < CHUNKS; cz++) {
                LevelChunk chunk = mining.getChunk(cx, cz);
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        for (int y = mining.getMinY(); y <= mining.getMaxY(); y++) {
                            BlockState state = chunk.getBlockState(pos.set(x, y, z));
                            total++;
                            if (state.isAir()) {
                                air++;
                                airByBand[(y - mining.getMinY()) / 32]++;
                            } else if (!state.getFluidState().isEmpty()) {
                                fluids++;
                            } else {
                                String name = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
                                if (name.endsWith("_ore")) {
                                    counts.merge(name, 1, Integer::sum);
                                } else if (DECOR.contains(name)) {
                                    decor.merge(name, 1, Integer::sum);
                                }
                            }
                        }
                    }
                }
            }
        }
        boolean bedrockFloor = mining.getBlockState(new BlockPos(0, mining.getMinY(), 0)).is(Blocks.BEDROCK);
        boolean bedrockRoof = mining.getBlockState(new BlockPos(0, mining.getMaxY(), 0)).is(Blocks.BEDROCK);
        MiningDimension.LOGGER.info("miningdim self test: {} chunks, y {}..{}, air {}%, fluid blocks {}, bedrock floor {}, roof {}",
                CHUNKS * CHUNKS, mining.getMinY(), mining.getMaxY(), Math.round(100.0 * air / total), fluids, bedrockFloor, bedrockRoof);
        StringBuilder bands = new StringBuilder();
        for (int band = 0; band < airByBand.length; band++) {
            bands.append(" y").append(mining.getMinY() + band * 32).append('=').append(Math.round(100.0 * airByBand[band] / (CHUNKS * CHUNKS * 256 * 32))).append('%');
        }
        MiningDimension.LOGGER.info("miningdim self test: air by height{}", bands);
        counts.replaceAll((name, count) -> count / (CHUNKS * CHUNKS));
        MiningDimension.LOGGER.info("miningdim self test: ore blocks per chunk {}", counts);
        MiningDimension.LOGGER.info("miningdim self test: decoration blocks in {} chunks {}", CHUNKS * CHUNKS, decor);
        if (fluids > 0 || !bedrockFloor || !bedrockRoof || counts.isEmpty()) {
            throw new IllegalStateException("miningdim self test: unexpected terrain");
        }

        // Biomes should be small: sample a 512x512 area every 16 blocks at three depths without generating it.
        Climate.Sampler sampler = mining.getChunkSource().randomState().sampler();
        BiomeSource biomes = mining.getChunkSource().getGenerator().getBiomeSource();
        Map<String, Integer> biomeCounts = new TreeMap<>();
        int changes = 0;
        for (int y : new int[] {-40, 40, 120}) {
            String previous = null;
            for (int x = 0; x < 512; x += 16) {
                for (int z = 0; z < 512; z += 16) {
                    String biome = biomes.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z), sampler)
                            .unwrapKey().map(key -> key.identifier().getPath()).orElse("?");
                    biomeCounts.merge(biome, 1, Integer::sum);
                    if (z > 0 && !biome.equals(previous)) {
                        changes++;
                    }
                    previous = biome;
                }
            }
        }
        // Each sample line is 512 blocks long; the average run between biome changes shows how big biomes are.
        MiningDimension.LOGGER.info("miningdim self test: biome samples {}, a new biome every ~{} blocks", biomeCounts,
                changes == 0 ? "inf" : String.valueOf(3 * 32 * 512 / changes));
        if (biomeCounts.size() < 4) {
            throw new IllegalStateException("miningdim self test: expected all four biomes in a 512 block area");
        }

        // An exit portal built inside solid rock must be a complete portal.
        FoundRectangle exit = PortalLinker.createPortal(mining, new BlockPos(200, 40, 200), Axis.X);
        if (!PortalShape.find(mining, exit.minCorner, Axis.X).isComplete()) {
            throw new IllegalStateException("miningdim self test: exit portal incomplete");
        }

        // A hand-built frame in the overworld lights like a nether portal would.
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        BlockPos base = new BlockPos(300, 100, 300);
        for (int w = -1; w <= 2; w++) {
            for (int h = -1; h <= 3; h++) {
                boolean frame = w == -1 || w == 2 || h == -1 || h == 3;
                overworld.setBlockAndUpdate(base.east(w).above(h), frame ? Blocks.CHISELED_DEEPSLATE.defaultBlockState() : Blocks.AIR.defaultBlockState());
            }
        }
        PortalShape.findEmpty(overworld, base, Axis.X).orElseThrow(() -> new IllegalStateException("miningdim self test: frame not found"))
                .createPortalBlocks(overworld);
        if (!overworld.getBlockState(base.above()).is(MiningDimension.PORTAL)) {
            throw new IllegalStateException("miningdim self test: portal not lit");
        }
        overworld.setBlockAndUpdate(base.east(-1).above(1), Blocks.AIR.defaultBlockState());
        if (overworld.getBlockState(base.above()).is(MiningDimension.PORTAL)) {
            throw new IllegalStateException("miningdim self test: portal survived a broken frame");
        }

        MiningDimension.LOGGER.info("miningdim self test passed");
    }
}
