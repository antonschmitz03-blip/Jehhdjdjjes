package io.github.antonschmitz.vplustweaks.mixin;

import io.github.antonschmitz.vplustweaks.LargeTrees;
import io.github.antonschmitz.vplustweaks.TweaksConfig;
import net.minecraft.world.level.levelgen.feature.treedecorators.AlterGroundDecorator;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Large trees grown from saplings leave the ground under them alone (no podzol). World generation is unchanged. */
@Mixin(AlterGroundDecorator.class)
public abstract class AlterGroundDecoratorMixin {
    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void vplustweaks$keepGround(TreeDecorator.Context context, CallbackInfo ci) {
        if (LargeTrees.GROWING_FROM_SAPLING.get() && TweaksConfig.enabled(TweaksConfig.NO_PODZOL_FROM_SAPLINGS)) {
            ci.cancel();
        }
    }
}
