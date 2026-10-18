package io.github.antonschmitz.miningdim;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * Places a small herd of Pebblins on cave floors while the chunk generates. Cave biomes only create creatures with a
 * low per-chunk chance, so without this Pebblins were close to nonexistent.
 */
public class PebblinFeature implements Feature {
    public static final PebblinFeature INSTANCE = new PebblinFeature();
    public static final MapCodec<PebblinFeature> CODEC = MapCodec.unit(INSTANCE);

    @Override
    public MapCodec<PebblinFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
        int herd = 1 + random.nextInt(3);
        boolean placed = false;
        for (int i = 0; i < herd; i++) {
            BlockPos pos = origin.offset(random.nextInt(5) - 2, 0, random.nextInt(5) - 2);
            if (!level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()
                    || !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) {
                continue;
            }
            Pebblin pebblin = MiningDimension.PEBBLIN.create(level.getLevel(), EntitySpawnReason.CHUNK_GENERATION);
            if (pebblin == null) continue;
            pebblin.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            pebblin.setPersistenceRequired();
            level.addFreshEntity(pebblin);
            placed = true;
        }
        return placed;
    }
}
