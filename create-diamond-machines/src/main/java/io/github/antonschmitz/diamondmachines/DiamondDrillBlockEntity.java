package io.github.antonschmitz.diamondmachines;

import com.zurrtum.create.content.kinetics.drill.DrillBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Create's drill, with our own block entity type (for our renderer) and a faster break speed.
 * DrillBlockEntity's constructor hard-codes Create's type, so the type is swapped by overriding the getters.
 */
public class DiamondDrillBlockEntity extends DrillBlockEntity {
    public DiamondDrillBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);
    }

    @Override
    public BlockEntityType<?> getType() {
        return DiamondMachines.DRILL_BE;
    }

    @Override
    public Holder<BlockEntityType<?>> typeHolder() {
        return DiamondMachines.DRILL_BE.builtInRegistryHolder();
    }

    @Override
    public boolean isValidBlockState(BlockState state) {
        return state.getBlock() instanceof DiamondDrillBlock;
    }

    @Override
    protected float getBreakSpeed() {
        return super.getBreakSpeed() * DiamondMachines.BREAK_SPEED_MULTIPLIER;
    }
}
