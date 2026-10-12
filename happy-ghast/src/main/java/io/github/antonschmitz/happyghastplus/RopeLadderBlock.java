package io.github.antonschmitz.happyghastplus;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;

/** The rope ladder a happy ghast lets down. It hangs in the air, so it needs no block behind it. */
public class RopeLadderBlock extends LadderBlock {
    public RopeLadderBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return true;
    }
}
