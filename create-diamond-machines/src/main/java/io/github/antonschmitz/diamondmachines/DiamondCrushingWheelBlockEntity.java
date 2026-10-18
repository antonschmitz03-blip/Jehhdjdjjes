package io.github.antonschmitz.diamondmachines;

import com.zurrtum.create.content.kinetics.crusher.CrushingWheelBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Create's crushing wheel with our own block entity type, so it can render with diamond plates. */
public class DiamondCrushingWheelBlockEntity extends CrushingWheelBlockEntity {
    public DiamondCrushingWheelBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);
    }

    @Override
    public BlockEntityType<?> getType() {
        return DiamondMachines.CRUSHING_WHEEL_BE;
    }

    @Override
    public Holder<BlockEntityType<?>> typeHolder() {
        return DiamondMachines.CRUSHING_WHEEL_BE.builtInRegistryHolder();
    }

    @Override
    public boolean isValidBlockState(BlockState state) {
        return state.getBlock() instanceof DiamondCrushingWheelBlock;
    }
}
