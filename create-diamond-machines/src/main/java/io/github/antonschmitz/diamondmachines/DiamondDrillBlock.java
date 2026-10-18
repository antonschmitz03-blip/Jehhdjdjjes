package io.github.antonschmitz.diamondmachines;

import com.zurrtum.create.AllBlocks;
import com.zurrtum.create.content.kinetics.drill.DrillBlock;
import com.zurrtum.create.content.kinetics.drill.DrillBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class DiamondDrillBlock extends DrillBlock implements DiamondVariant {
    public DiamondDrillBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Block baseBlock() {
        return AllBlocks.MECHANICAL_DRILL;
    }

    @Override
    public BlockEntityType<? extends DrillBlockEntity> getBlockEntityType() {
        return DiamondMachines.DRILL_BE;
    }
}
