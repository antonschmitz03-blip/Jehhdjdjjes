package io.github.antonschmitz.diamondmachines.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.zurrtum.create.client.content.kinetics.saw.SawMovementRenderBehaviour;
import com.zurrtum.create.client.flywheel.lib.model.baked.PartialModel;
import com.zurrtum.create.content.contraptions.behaviour.MovementContext;
import io.github.antonschmitz.diamondmachines.client.DiamondPartials;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Saw blade on a moving contraption. */
@Mixin(SawMovementRenderBehaviour.class)
public abstract class SawMovementRenderBehaviourMixin {
    @ModifyExpressionValue(method = "getRenderState", at = {
            @At(value = "FIELD", target = "Lcom/zurrtum/create/client/AllPartialModels;SAW_BLADE_HORIZONTAL_ACTIVE:Lcom/zurrtum/create/client/flywheel/lib/model/baked/PartialModel;"),
            @At(value = "FIELD", target = "Lcom/zurrtum/create/client/AllPartialModels;SAW_BLADE_HORIZONTAL_INACTIVE:Lcom/zurrtum/create/client/flywheel/lib/model/baked/PartialModel;"),
            @At(value = "FIELD", target = "Lcom/zurrtum/create/client/AllPartialModels;SAW_BLADE_VERTICAL_ACTIVE:Lcom/zurrtum/create/client/flywheel/lib/model/baked/PartialModel;"),
            @At(value = "FIELD", target = "Lcom/zurrtum/create/client/AllPartialModels;SAW_BLADE_VERTICAL_INACTIVE:Lcom/zurrtum/create/client/flywheel/lib/model/baked/PartialModel;")
    })
    private PartialModel diamondmachines$diamondBlade(PartialModel original, @Local(argsOnly = true) MovementContext context) {
        return DiamondPartials.forState(original, context.state);
    }
}
