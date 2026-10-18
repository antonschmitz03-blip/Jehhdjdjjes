package io.github.antonschmitz.diamondmachines.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.zurrtum.create.client.content.kinetics.saw.SawVisual;
import com.zurrtum.create.client.flywheel.lib.model.baked.PartialModel;
import io.github.antonschmitz.diamondmachines.client.DiamondPartials;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Flywheel saw visual: diamond blade, and keep the diamond blade's animation running. */
@Mixin(SawVisual.class)
public abstract class SawVisualMixin {
    @ModifyExpressionValue(method = "updateSaw", at = {
            @At(value = "FIELD", target = "Lcom/zurrtum/create/client/AllPartialModels;SAW_BLADE_HORIZONTAL_ACTIVE:Lcom/zurrtum/create/client/flywheel/lib/model/baked/PartialModel;"),
            @At(value = "FIELD", target = "Lcom/zurrtum/create/client/AllPartialModels;SAW_BLADE_HORIZONTAL_INACTIVE:Lcom/zurrtum/create/client/flywheel/lib/model/baked/PartialModel;"),
            @At(value = "FIELD", target = "Lcom/zurrtum/create/client/AllPartialModels;SAW_BLADE_HORIZONTAL_REVERSED:Lcom/zurrtum/create/client/flywheel/lib/model/baked/PartialModel;"),
            @At(value = "FIELD", target = "Lcom/zurrtum/create/client/AllPartialModels;SAW_BLADE_VERTICAL_ACTIVE:Lcom/zurrtum/create/client/flywheel/lib/model/baked/PartialModel;"),
            @At(value = "FIELD", target = "Lcom/zurrtum/create/client/AllPartialModels;SAW_BLADE_VERTICAL_INACTIVE:Lcom/zurrtum/create/client/flywheel/lib/model/baked/PartialModel;"),
            @At(value = "FIELD", target = "Lcom/zurrtum/create/client/AllPartialModels;SAW_BLADE_VERTICAL_REVERSED:Lcom/zurrtum/create/client/flywheel/lib/model/baked/PartialModel;")
    })
    private PartialModel diamondmachines$diamondBlade(PartialModel original, @Local(argsOnly = true) BlockState state) {
        return DiamondPartials.forState(original, state);
    }

    @ModifyExpressionValue(method = "updateSaw", at = @At(value = "INVOKE", target = "Ljava/util/function/Supplier;get()Ljava/lang/Object;"))
    private Object diamondmachines$diamondSprite(Object original, @Local(argsOnly = true) BlockState state) {
        return DiamondPartials.isDiamond(state) ? DiamondPartials.sawSprite() : original;
    }
}
