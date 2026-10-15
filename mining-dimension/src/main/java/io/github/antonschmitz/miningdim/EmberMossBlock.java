package io.github.antonschmitz.miningdim;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Dark moss flecked with glowing orange specks that now and then lets an ember drift up. */
public class EmberMossBlock extends Block {
    public EmberMossBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(16) == 0 && level.getBlockState(pos.above()).isAir()) {
            level.addParticle(ParticleTypes.SMALL_FLAME, pos.getX() + random.nextDouble(), pos.getY() + 1.05, pos.getZ() + random.nextDouble(),
                    0.0, 0.02, 0.0);
        }
    }
}
