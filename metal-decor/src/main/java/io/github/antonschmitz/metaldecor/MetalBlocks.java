package io.github.antonschmitz.metaldecor;

import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WaterloggedTransparentBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Public constructors for the vanilla block types whose own constructors are protected. */
public final class MetalBlocks {
    private MetalBlocks() {}

    public static class Grate extends WaterloggedTransparentBlock {
        public Grate(Properties properties) {
            super(properties);
        }
    }

    public static class Stairs extends StairBlock {
        public Stairs(BlockState baseState, Properties properties) {
            super(baseState, properties);
        }
    }

    public static class Bars extends IronBarsBlock {
        public Bars(Properties properties) {
            super(properties);
        }
    }
}
