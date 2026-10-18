package io.github.antonschmitz.diamondmachines.client;

import com.zurrtum.create.client.AllBlockEntityBehaviours;
import com.zurrtum.create.client.AllBlockEntityRenders;
import com.zurrtum.create.client.content.kinetics.base.SingleAxisRotatingRenderer;
import com.zurrtum.create.client.content.kinetics.drill.DrillMovementRenderBehaviour;
import com.zurrtum.create.client.content.kinetics.drill.DrillRenderer;
import com.zurrtum.create.client.content.kinetics.saw.SawMovementRenderBehaviour;
import com.zurrtum.create.client.content.kinetics.saw.SawRenderer;
import com.zurrtum.create.client.content.kinetics.saw.SawVisual;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.audio.KineticAudioBehaviour;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.audio.SawAudioBehaviour;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.zurrtum.create.client.foundation.blockEntity.behaviour.tooltip.KineticTooltipBehaviour;
import io.github.antonschmitz.diamondmachines.DiamondMachines;
import net.fabricmc.api.ClientModInitializer;

public class DiamondMachinesClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        DiamondPartials.init();

        // Create's renderers and visuals; the mixins in client.mixin swap in the diamond parts for our blocks.
        AllBlockEntityRenders.visual(DiamondMachines.DRILL_BE, DrillRenderer::new, DiamondDrillVisual::new);
        AllBlockEntityRenders.normal(DiamondMachines.SAW_BE, SawRenderer::new, SawVisual::new);
        AllBlockEntityRenders.visual(DiamondMachines.CRUSHING_WHEEL_BE,
                SingleAxisRotatingRenderer.of(DiamondPartials.CRUSHING_WHEEL), DiamondCrushingWheelVisual::new);

        // Sounds, goggle tooltips and the saw's filter slot, which Create looks up by block entity type.
        AllBlockEntityBehaviours.add(DiamondMachines.DRILL_BE, KineticAudioBehaviour::new, KineticTooltipBehaviour::new);
        AllBlockEntityBehaviours.add(DiamondMachines.SAW_BE, FilteringBehaviour::saw, KineticAudioBehaviour::new,
                KineticTooltipBehaviour::new, SawAudioBehaviour::new);
        AllBlockEntityBehaviours.add(DiamondMachines.CRUSHING_WHEEL_BE, KineticAudioBehaviour::new,
                KineticTooltipBehaviour::new);

        DiamondMachines.DRILL_MOVEMENT.attachRender = new DrillMovementRenderBehaviour();
        DiamondMachines.SAW_MOVEMENT.attachRender = new SawMovementRenderBehaviour();
    }
}
