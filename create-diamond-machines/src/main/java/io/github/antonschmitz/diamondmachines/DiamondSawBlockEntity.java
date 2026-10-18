package io.github.antonschmitz.diamondmachines;

import com.zurrtum.create.content.kinetics.saw.SawBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Create's saw with our own block entity type, faster block breaking and faster item processing. */
public class DiamondSawBlockEntity extends SawBlockEntity {
    public DiamondSawBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);
    }

    @Override
    public BlockEntityType<?> getType() {
        return DiamondMachines.SAW_BE;
    }

    @Override
    public Holder<BlockEntityType<?>> typeHolder() {
        return DiamondMachines.SAW_BE.builtInRegistryHolder();
    }

    @Override
    public boolean isValidBlockState(BlockState state) {
        return state.getBlock() instanceof DiamondSawBlock;
    }

    @Override
    protected float getBreakSpeed() {
        return super.getBreakSpeed() * DiamondMachines.BREAK_SPEED_MULTIPLIER;
    }

    @Override
    public void start(ItemStack inserted) {
        super.start(inserted);
        // start() sets how long the cut takes; the diamond blade needs half of that
        if (inventory.remainingTime > 0) {
            inventory.remainingTime /= DiamondMachines.SAW_PROCESSING_MULTIPLIER;
            inventory.recipeDuration /= DiamondMachines.SAW_PROCESSING_MULTIPLIER;
        }
    }
}
