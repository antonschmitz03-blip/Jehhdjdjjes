package io.github.antonschmitz.miningdim;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * An abandoned dig site: a short stretch of old rail running into a cave-in, a lopsided timber support with a lantern,
 * a supply chest beside the track and a few cobwebs.
 */
public class DigSiteFeature implements Feature {
    public static final DigSiteFeature INSTANCE = new DigSiteFeature();
    public static final MapCodec<DigSiteFeature> CODEC = MapCodec.unit(INSTANCE);
    private static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE, MiningDimension.id("chests/dig_site"));

    @Override
    public MapCodec<DigSiteFeature> codec() {
        return CODEC;
    }

    private static boolean floorAt(WorldGenLevel level, BlockPos pos) {
        return level.getBlockState(pos).isAir() && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    private static void set(WorldGenLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, Block.UPDATE_CLIENTS);
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
        if (!floorAt(level, origin)) return false;
        Direction dir = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        Direction left = dir.getCounterClockWise();
        Direction right = dir.getClockWise();

        // The track: as long as the floor stays flat and open.
        int length = 0;
        while (length < 7 && floorAt(level, origin.relative(dir, length))) length++;
        if (length < 4) return false;
        RailShape shape = dir.getAxis() == Direction.Axis.X ? RailShape.EAST_WEST : RailShape.NORTH_SOUTH;
        for (int k = 0; k < length; k++) {
            if (k == 0 || random.nextFloat() < 0.8F) {
                set(level, origin.relative(dir, k), Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, shape));
            }
        }

        // The cave-in at the end of the track.
        BlockPos end = origin.relative(dir, length - 1);
        for (int j = 1; j <= 2; j++) {
            for (int side = -1; side <= 1; side++) {
                BlockPos base = end.relative(dir, j).relative(right, side);
                int height = (side == 0 ? 3 : 1 + random.nextInt(2)) - (j == 2 ? 1 : 0);
                for (int h = 0; h < height; h++) {
                    BlockPos pos = base.above(h);
                    if (!level.getBlockState(pos).isAir()) break;
                    if (!level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) break;
                    set(level, pos, (random.nextInt(3) == 0 ? Blocks.COBBLESTONE : Blocks.GRAVEL).defaultBlockState());
                }
            }
        }

        // A timber support with a lantern, a few blocks before the cave-in.
        BlockPos beam = origin.relative(dir, Math.max(1, length - 3));
        BlockPos leftPost = beam.relative(left);
        BlockPos rightPost = beam.relative(right);
        if (floorAt(level, leftPost) && floorAt(level, rightPost)
                && level.getBlockState(leftPost.above(2)).isAir() && level.getBlockState(rightPost.above(2)).isAir() && level.getBlockState(beam.above(2)).isAir()) {
            Block fence = Blocks.OAK_FENCE;
            set(level, leftPost, fence.defaultBlockState());
            set(level, leftPost.above(), fence.defaultBlockState());
            set(level, leftPost.above(2), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, false));
            set(level, rightPost, fence.defaultBlockState());
            set(level, rightPost.above(), fence.defaultBlockState());
            set(level, rightPost.above(2), Blocks.OAK_PLANKS.defaultBlockState());
            set(level, beam.above(2), Blocks.OAK_PLANKS.defaultBlockState());
        }

        // The supply chest next to the first rails, facing the track.
        for (Direction side : new Direction[] {left, right}) {
            BlockPos chest = origin.relative(side, 2);
            if (floorAt(level, chest)) {
                set(level, chest, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, side.getOpposite()));
                RandomizableContainer.setBlockEntityLootTable(level, random, chest, LOOT);
                break;
            }
        }

        // Cobwebs under the ceiling.
        for (int w = 0; w < 3; w++) {
            BlockPos column = origin.relative(dir, random.nextInt(length)).relative(left, random.nextInt(3) - 1);
            for (int up = 1; up < 7; up++) {
                BlockPos pos = column.above(up);
                if (!level.getBlockState(pos).isAir()) {
                    if (up > 2 && level.getBlockState(pos).isSolid()) {
                        set(level, pos.below(), Blocks.COBWEB.defaultBlockState());
                    }
                    break;
                }
            }
        }
        return true;
    }
}
