package io.github.antonschmitz.miningdim;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/** A cluster of glowworm threads hanging from the ceiling around the origin (an air block just under the ceiling). */
public class GlowwormFeature implements Feature {
    public static final GlowwormFeature INSTANCE = new GlowwormFeature();
    public static final MapCodec<GlowwormFeature> CODEC = MapCodec.unit(INSTANCE);

    @Override
    public MapCodec<GlowwormFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
        boolean placed = false;
        int threads = 5 + random.nextInt(10);
        for (int i = 0; i < threads; i++) {
            BlockPos column = origin.offset(random.nextInt(7) - 3, 0, random.nextInt(7) - 3);
            // The ceiling may be a bit higher or lower than at the origin.
            BlockPos top = null;
            for (int dy : new int[] {0, -1, 1, -2, 2}) {
                if (hangable(level, column.above(dy))) {
                    top = column.above(dy);
                    break;
                }
            }
            if (top == null) continue;
            int length = 2 + random.nextInt(5);
            BlockPos pos = top;
            int placedLength = 0;
            while (placedLength < length && level.getBlockState(pos).isAir()) {
                pos = pos.below();
                placedLength++;
            }
            for (int k = 0; k < placedLength; k++) {
                level.setBlock(top.below(k), MiningDimension.GLOWWORM.defaultBlockState().setValue(GlowwormBlock.TIP, k == placedLength - 1), Block.UPDATE_CLIENTS);
            }
            placed |= placedLength > 0;
        }
        return placed;
    }

    private static boolean hangable(WorldGenLevel level, BlockPos pos) {
        return level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isFaceSturdy(level, pos.above(), Direction.DOWN);
    }
}
