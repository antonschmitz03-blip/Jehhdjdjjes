package io.github.antonschmitz.miningdim;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * A knocker camp: the stash sack on a trodden patch of dirt and gravel, an unlit campfire, straw beds (hay bales), a few candles, sometimes a stolen torch on the wall or a bent rail,
 * and cobwebs under the ceiling.
 */
public class KnockerDenFeature extends Feature<NoneFeatureConfiguration> {
    public KnockerDenFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    private static boolean floorAt(WorldGenLevel level, BlockPos pos) {
        return level.getBlockState(pos).isAir() && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    private static boolean isGround(BlockState state) {
        return state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.DIRT) || state.is(Blocks.GRAVEL);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();
        if (!floorAt(level, origin)) return false;

        // Trodden floor and the spots where props can stand.
        List<BlockPos> spots = new ArrayList<>();
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                if (dx * dx + dz * dz > 8 + random.nextInt(4)) continue;
                for (int dy = 1; dy >= -1; dy--) {
                    BlockPos spot = origin.offset(dx, dy, dz);
                    if (!floorAt(level, spot)) continue;
                    BlockPos ground = spot.below();
                    if (isGround(level.getBlockState(ground)) && random.nextFloat() < 0.75F) {
                        float r = random.nextFloat();
                        level.setBlock(ground, (r < 0.5F ? Blocks.COARSE_DIRT : r < 0.8F ? Blocks.GRAVEL : Blocks.PACKED_MUD).defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                    if (dx != 0 || dz != 0) spots.add(spot);
                    break;
                }
            }
        }

        level.setBlock(origin, MiningDimension.KNOCKER_DEN.defaultBlockState()
                .setValue(KnockerDenBlock.FILL, 1 + random.nextInt(3))
                .setValue(KnockerDenBlock.FACING, Direction.Plane.HORIZONTAL.getRandomDirection(random)), Block.UPDATE_CLIENTS);

        java.util.Collections.shuffle(spots, new java.util.Random(random.nextLong()));
        int i = 0;
        if (i < spots.size()) {
            level.setBlock(spots.get(i++), Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false), Block.UPDATE_CLIENTS);
        }
        int beds = 1 + random.nextInt(2);
        for (int b = 0; b < beds && i < spots.size(); b++) {
            level.setBlock(spots.get(i++), Blocks.HAY_BLOCK.defaultBlockState()
                    .setValue(RotatedPillarBlock.AXIS, random.nextBoolean() ? Direction.Axis.X : Direction.Axis.Z), Block.UPDATE_CLIENTS);
        }
        int candles = 1 + random.nextInt(2);
        for (int c = 0; c < candles && i < spots.size(); c++) {
            level.setBlock(spots.get(i++), Blocks.CANDLE.defaultBlockState()
                    .setValue(CandleBlock.CANDLES, 1 + random.nextInt(3)).setValue(CandleBlock.LIT, true), Block.UPDATE_CLIENTS);
        }
        if (random.nextBoolean() && i < spots.size()) {
            level.setBlock(spots.get(i++), Blocks.RAIL.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        // A stolen torch stuck on a nearby wall.
        if (random.nextBoolean()) {
            outer:
            for (int s = i; s < spots.size(); s++) {
                BlockPos spot = spots.get(s).above();
                if (!level.getBlockState(spot).isAir()) continue;
                for (Direction dir : Direction.Plane.HORIZONTAL) {
                    BlockPos wall = spot.relative(dir.getOpposite());
                    if (level.getBlockState(wall).isFaceSturdy(level, wall, dir)) {
                        level.setBlock(spot, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, dir), Block.UPDATE_CLIENTS);
                        break outer;
                    }
                }
            }
        }
        // Cobwebs hanging under the ceiling.
        for (int w = 0; w < 4; w++) {
            BlockPos column = origin.offset(random.nextInt(7) - 3, 1, random.nextInt(7) - 3);
            for (int up = 0; up < 7; up++) {
                BlockPos pos = column.above(up);
                if (!level.getBlockState(pos).isAir()) {
                    if (up > 1 && level.getBlockState(pos).isSolid()) {
                        level.setBlock(pos.below(), Blocks.COBWEB.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                    break;
                }
            }
        }
        return true;
    }
}
