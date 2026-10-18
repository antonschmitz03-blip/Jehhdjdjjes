package io.github.antonschmitz.diamondmachines.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.zurrtum.create.client.content.kinetics.drill.DrillRenderer;
import com.zurrtum.create.client.flywheel.lib.model.baked.PartialModel;
import com.zurrtum.create.content.kinetics.drill.DrillBlockEntity;
import io.github.antonschmitz.diamondmachines.client.DiamondPartials;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Fallback renderer (used when Flywheel can't draw, e.g. with some shader packs): diamond head for our drill. */
@Mixin(DrillRenderer.class)
public abstract class DrillRendererMixin {
    @ModifyExpressionValue(
            method = "extractRenderState(Lcom/zurrtum/create/content/kinetics/drill/DrillBlockEntity;Lcom/zurrtum/create/client/content/kinetics/base/SingleKineticRenderState;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V",
            at = @At(value = "FIELD", target = "Lcom/zurrtum/create/client/AllPartialModels;DRILL_HEAD:Lcom/zurrtum/create/client/flywheel/lib/model/baked/PartialModel;"))
    private PartialModel diamondmachines$diamondHead(PartialModel original, @Local(argsOnly = true) DrillBlockEntity be) {
        return DiamondPartials.forState(original, be.getBlockState());
    }
}
