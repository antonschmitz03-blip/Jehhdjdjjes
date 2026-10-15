package io.github.antonschmitz.miningdim;

import com.mojang.serialization.MapCodec;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SeaPickleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * A still pool sunk into a flat stretch of cave floor: ragged shore with clay and gravel, one to three blocks deep.
 * Cells are only flooded where the surrounding rock holds the water, so a lake never spills.
 */
public class UndergroundLakeFeature implements Feature {
    public static final UndergroundLakeFeature INSTANCE = new UndergroundLakeFeature();
    public static final MapCodec<UndergroundLakeFeature> CODEC = MapCodec.unit(INSTANCE);
    private static final int[][] NEIGHBOURS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    @Override
    public MapCodec<UndergroundLakeFeature> codec() {
        return CODEC;
    }

    private static boolean holds(WorldGenLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return !state.isAir() && state.getFluidState().isEmpty() && state.canOcclude();
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
        int floorY = origin.getY() - 1;
        int rx = 3 + random.nextInt(5);
        int rz = 3 + random.nextInt(5);

        // 1. Flat floor cells inside a ragged ellipse, with room above them.
        Set<BlockPos> surface = new HashSet<>();
        for (int dx = -rx; dx <= rx; dx++) {
            for (int dz = -rz; dz <= rz; dz++) {
                double dist = (double) dx * dx / (rx * rx) + (double) dz * dz / (rz * rz);
                if (dist > 0.75 + random.nextDouble() * 0.3) continue;
                BlockPos floor = new BlockPos(origin.getX() + dx, floorY, origin.getZ() + dz);
                if (holds(level, floor) && level.getBlockState(floor.above()).isAir() && level.getBlockState(floor.above(2)).isAir()) {
                    surface.add(floor);
                }
            }
        }
        // 2. Drop cells whose neighbours at the surface level are open air (the water would run off).
        surface.removeIf(cell -> {
            for (int[] n : NEIGHBOURS) {
                BlockPos side = cell.offset(n[0], 0, n[1]);
                if (!surface.contains(side) && !holds(level, side)) return true;
            }
            return false;
        });
        if (surface.size() < 8) return false;

        // 3. Carve one to three blocks down where the rock below can hold water.
        Set<BlockPos> water = new HashSet<>();
        for (BlockPos cell : surface) {
            double dx = (cell.getX() - origin.getX()) / (double) rx;
            double dz = (cell.getZ() - origin.getZ()) / (double) rz;
            int depth = 1 + (int) (2.0 * (1.0 - Math.min(1.0, dx * dx + dz * dz)) + random.nextDouble() * 0.6);
            int usable = 0;
            while (usable < depth && holds(level, cell.below(usable)) && holds(level, cell.below(usable + 1))) usable++;
            for (int k = 0; k < usable; k++) water.add(cell.below(k));
        }
        // Water below the surface must also be walled in on every side.
        water.removeIf(pos -> {
            for (int[] n : NEIGHBOURS) {
                BlockPos side = pos.offset(n[0], 0, n[1]);
                if (!water.contains(side) && !holds(level, side)) return true;
            }
            return false;
        });
        if (water.size() < 8) return false;

        BlockState waterState = Blocks.WATER.defaultBlockState();
        for (BlockPos pos : water) {
            level.setBlock(pos, waterState, Block.UPDATE_CLIENTS);
        }
        // Sea pickles on the lake bed (they only glow underwater).
        for (BlockPos pos : water) {
            if (!water.contains(pos.below()) && random.nextFloat() < 0.12F) {
                level.setBlock(pos, Blocks.SEA_PICKLE.defaultBlockState().setValue(SeaPickleBlock.PICKLES, 1 + random.nextInt(4)).setValue(SeaPickleBlock.WATERLOGGED, true),
                        Block.UPDATE_CLIENTS);
            }
        }
        // 4. Clay and gravel on the shore.
        for (BlockPos cell : surface) {
            if (water.contains(cell)) continue;
            boolean shore = false;
            for (int[] n : NEIGHBOURS) {
                if (water.contains(cell.offset(n[0], 0, n[1]))) shore = true;
            }
            if (shore && random.nextFloat() < 0.6F) {
                level.setBlock(cell, (random.nextBoolean() ? Blocks.CLAY : Blocks.GRAVEL).defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        return true;
    }
}
