package io.github.antonschmitz.diamondmachines;

import com.zurrtum.create.AllBlocks;
import com.zurrtum.create.content.kinetics.crusher.CrushingWheelBlock;
import com.zurrtum.create.content.kinetics.crusher.CrushingWheelBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class DiamondCrushingWheelBlock extends CrushingWheelBlock implements DiamondVariant {
    public DiamondCrushingWheelBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Block baseBlock() {
        return AllBlocks.CRUSHING_WHEEL;
    }

    @Override
    public BlockEntityType<? extends CrushingWheelBlockEntity> getBlockEntityType() {
        return DiamondMachines.CRUSHING_WHEEL_BE;
    }

    /** True when the crushing wheel controller at {@code controllerPos} sits between two diamond wheels. */
    public static boolean isDiamondPair(BlockGetter level, BlockPos controllerPos) {
        int diamondWheels = 0;
        for (Direction direction : Direction.values()) {
            if (level.getBlockState(controllerPos.relative(direction)).getBlock() instanceof DiamondCrushingWheelBlock) {
                diamondWheels++;
            }
        }
        return diamondWheels >= 2;
    }
}
