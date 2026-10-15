package io.github.antonschmitz.miningdim;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * Scatters one block state around the origin. Modes: {@code floor} (plants standing on the ground), {@code ceiling}
 * (hanging from the roof) and {@code replace_floor} (turns patches of the ground itself into the state).
 */
public record ScatterFeature(BlockState state, String mode, int tries, int radius) implements Feature {
    public static final MapCodec<ScatterFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BlockState.CODEC.fieldOf("state").forGetter(ScatterFeature::state),
            Codec.STRING.fieldOf("mode").forGetter(ScatterFeature::mode),
            Codec.INT.fieldOf("tries").forGetter(ScatterFeature::tries),
            Codec.INT.fieldOf("radius").forGetter(ScatterFeature::radius)).apply(i, ScatterFeature::new));

    @Override
    public MapCodec<ScatterFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
        boolean placed = false;
        if (this.mode.equals("replace_floor")) {
            for (int dx = -this.radius; dx <= this.radius; dx++) {
                for (int dz = -this.radius; dz <= this.radius; dz++) {
                    if (dx * dx + dz * dz > this.radius * this.radius + random.nextInt(3) || random.nextFloat() > 0.8F) continue;
                    BlockPos floor = findFloor(level, origin.offset(dx, 0, dz));
                    if (floor == null) continue;
                    BlockState below = level.getBlockState(floor.below());
                    if (below.is(BlockTags.BASE_STONE_OVERWORLD) || below.is(Blocks.BASALT) || below.is(Blocks.BLACKSTONE) || below.is(Blocks.SMOOTH_BASALT)) {
                        level.setBlock(floor.below(), this.state, Block.UPDATE_CLIENTS);
                        placed = true;
                    }
                }
            }
            return placed;
        }
        boolean ceiling = this.mode.equals("ceiling");
        for (int t = 0; t < this.tries; t++) {
            BlockPos spot = origin.offset(random.nextInt(this.radius * 2 + 1) - this.radius, random.nextInt(3) - 1, random.nextInt(this.radius * 2 + 1) - this.radius);
            BlockPos pos = ceiling ? findCeiling(level, spot) : findFloor(level, spot);
            if (pos == null || !this.state.canSurvive(level, pos)) continue;
            if (this.state.getBlock() instanceof DoublePlantBlock) {
                if (!level.getBlockState(pos.above()).isAir()) continue;
                DoublePlantBlock.placeAt(level, this.state, pos, Block.UPDATE_CLIENTS);
            } else {
                level.setBlock(pos, this.state, Block.UPDATE_CLIENTS);
            }
            placed = true;
        }
        return placed;
    }

    /** The first air block with solid ground below, searching a few blocks up and down from the spot. */
    private static BlockPos findFloor(WorldGenLevel level, BlockPos spot) {
        for (int dy : new int[] {0, -1, 1, -2, 2}) {
            BlockPos pos = spot.above(dy);
            if (level.getBlockState(pos).isAir() && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) return pos;
        }
        return null;
    }

    private static BlockPos findCeiling(WorldGenLevel level, BlockPos spot) {
        for (int dy : new int[] {0, -1, 1, -2, 2}) {
            BlockPos pos = spot.above(dy);
            if (level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isFaceSturdy(level, pos.above(), Direction.DOWN)) return pos;
        }
        return null;
    }
}
