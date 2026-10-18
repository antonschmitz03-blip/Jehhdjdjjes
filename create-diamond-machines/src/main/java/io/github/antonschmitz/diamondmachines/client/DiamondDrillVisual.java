package io.github.antonschmitz.diamondmachines.client;

import com.zurrtum.create.client.content.kinetics.base.OrientedRotatingVisual;
import com.zurrtum.create.client.flywheel.api.visualization.VisualizationContext;
import com.zurrtum.create.client.flywheel.lib.model.Models;
import com.zurrtum.create.content.kinetics.drill.DrillBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Create's DrillVisual with the diamond head. */
public class DiamondDrillVisual extends OrientedRotatingVisual<DrillBlockEntity> {
    public DiamondDrillVisual(VisualizationContext context, DrillBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick, Direction.SOUTH,
                blockEntity.getBlockState().getValue(BlockStateProperties.FACING),
                Models.chunkPartial(DiamondPartials.DRILL_HEAD));
    }

    @Override
    public void setSectionCollector(SectionCollector sectionCollector) {
        switch (blockState.getValue(BlockStateProperties.FACING)) {
            case UP -> setSectionCollector(sectionCollector, 0, 0, 0, 0, 1, 0);
            case DOWN -> setSectionCollector(sectionCollector, 0, -1, 0, 0, 0, 0);
            case NORTH -> setSectionCollector(sectionCollector, 0, 0, -1, 0, 0, 0);
            case SOUTH -> setSectionCollector(sectionCollector, 0, 0, 0, 0, 0, 1);
            case WEST -> setSectionCollector(sectionCollector, -1, 0, 0, 0, 0, 0);
            case EAST -> setSectionCollector(sectionCollector, 0, 0, 0, 1, 0, 0);
        }
    }
}
