package io.github.antonschmitz.diamondmachines.client;

import com.zurrtum.create.client.content.kinetics.base.SingleAxisRotatingVisual;
import com.zurrtum.create.client.flywheel.api.visualization.VisualizationContext;
import com.zurrtum.create.client.flywheel.lib.model.Models;
import com.zurrtum.create.content.kinetics.crusher.CrushingWheelBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Create's CrushingWheelVisual with diamond plates. */
public class DiamondCrushingWheelVisual extends SingleAxisRotatingVisual<CrushingWheelBlockEntity> {
    public DiamondCrushingWheelVisual(VisualizationContext context, CrushingWheelBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick, Models.chunkPartial(DiamondPartials.CRUSHING_WHEEL));
    }

    @Override
    public void setSectionCollector(SectionCollector sectionCollector) {
        switch (blockState.getValue(BlockStateProperties.AXIS)) {
            case X -> setSectionCollector(sectionCollector, 0, -1, -1, 0, 1, 1);
            case Y -> setSectionCollector(sectionCollector, -1, 0, -1, 1, 0, 1);
            case Z -> setSectionCollector(sectionCollector, -1, -1, 0, 1, 1, 0);
        }
    }
}
