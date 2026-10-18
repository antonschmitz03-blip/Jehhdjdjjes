package io.github.antonschmitz.diamondmachines;

import com.zurrtum.create.AllBlocks;
import com.zurrtum.create.content.kinetics.saw.SawBlock;
import com.zurrtum.create.content.kinetics.saw.SawBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class DiamondSawBlock extends SawBlock implements DiamondVariant {
    public DiamondSawBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Block baseBlock() {
        return AllBlocks.MECHANICAL_SAW;
    }

    @Override
    public BlockEntityType<? extends SawBlockEntity> getBlockEntityType() {
        return DiamondMachines.SAW_BE;
    }
}
