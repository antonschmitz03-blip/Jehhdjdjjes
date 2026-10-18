package io.github.antonschmitz.diamondmachines.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.zurrtum.create.client.content.kinetics.drill.DrillMovementRenderBehaviour;
import com.zurrtum.create.client.flywheel.lib.model.baked.PartialModel;
import com.zurrtum.create.content.contraptions.behaviour.MovementContext;
import io.github.antonschmitz.diamondmachines.client.DiamondPartials;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Drill head on a moving contraption (fallback path without Flywheel). */
@Mixin(DrillMovementRenderBehaviour.class)
public abstract class DrillMovementRenderBehaviourMixin {
    @ModifyExpressionValue(method = "getRenderState", at = @At(value = "FIELD", target = "Lcom/zurrtum/create/client/AllPartialModels;DRILL_HEAD:Lcom/zurrtum/create/client/flywheel/lib/model/baked/PartialModel;"))
    private PartialModel diamondmachines$diamondHead(PartialModel original, @Local(argsOnly = true) MovementContext context) {
        return DiamondPartials.forState(original, context.state);
    }
}
