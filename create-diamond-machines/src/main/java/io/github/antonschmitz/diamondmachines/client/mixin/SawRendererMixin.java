package io.github.antonschmitz.diamondmachines.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.zurrtum.create.client.content.kinetics.saw.SawRenderer;
import com.zurrtum.create.client.flywheel.lib.model.baked.PartialModel;
import com.zurrtum.create.content.kinetics.saw.SawBlockEntity;
import io.github.antonschmitz.diamondmachines.client.DiamondPartials;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Fallback saw renderer: diamond blade, and keep the diamond blade's animation running. */
@Mixin(SawRenderer.class)
public abstract class SawRendererMixin {
    @ModifyExpressionValue(method = "extractRenderState(Lcom/zurrtum/create/content/kinetics/saw/SawBlockEntity;Lcom/zurrtum/create/client/content/kinetics/saw/SawRenderer$SawRenderState;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V", at = {
            @At(value = "FIELD", target = "Lcom/zurrtum/create/client/AllPartialModels;SAW_BLADE_HORIZONTAL_ACTIVE:Lcom/zurrtum/create/client/flywheel/lib/model/baked/PartialModel;"),
            @At(value = "FIELD", target = "Lcom/zurrtum/create/client/AllPartialModels;SAW_BLADE_HORIZONTAL_INACTIVE:Lcom/zurrtum/create/client/flywheel/lib/model/baked/PartialModel;"),
            @At(value = "FIELD", target = "Lcom/zurrtum/create/client/AllPartialModels;SAW_BLADE_HORIZONTAL_REVERSED:Lcom/zurrtum/create/client/flywheel/lib/model/baked/PartialModel;"),
            @At(value = "FIELD", target = "Lcom/zurrtum/create/client/AllPartialModels;SAW_BLADE_VERTICAL_ACTIVE:Lcom/zurrtum/create/client/flywheel/lib/model/baked/PartialModel;"),
            @At(value = "FIELD", target = "Lcom/zurrtum/create/client/AllPartialModels;SAW_BLADE_VERTICAL_INACTIVE:Lcom/zurrtum/create/client/flywheel/lib/model/baked/PartialModel;"),
            @At(value = "FIELD", target = "Lcom/zurrtum/create/client/AllPartialModels;SAW_BLADE_VERTICAL_REVERSED:Lcom/zurrtum/create/client/flywheel/lib/model/baked/PartialModel;")
    })
    private PartialModel diamondmachines$diamondBlade(PartialModel original, @Local(argsOnly = true) SawBlockEntity be) {
        return DiamondPartials.forState(original, be.getBlockState());
    }

    @ModifyExpressionValue(method = "extractRenderState(Lcom/zurrtum/create/content/kinetics/saw/SawBlockEntity;Lcom/zurrtum/create/client/content/kinetics/saw/SawRenderer$SawRenderState;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V", at = @At(value = "FIELD",
            target = "Lcom/zurrtum/create/client/content/kinetics/saw/SawRenderer;sprite:Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;"))
    private TextureAtlasSprite diamondmachines$diamondSprite(TextureAtlasSprite original, @Local(argsOnly = true) SawBlockEntity be) {
        return DiamondPartials.isDiamond(be.getBlockState()) ? DiamondPartials.sawSprite() : original;
    }
}
