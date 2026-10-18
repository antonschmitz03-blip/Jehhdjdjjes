package io.github.antonschmitz.vplustweaks.mixin;

import io.github.antonschmitz.vplustweaks.LargeTrees;
import net.minecraft.world.level.block.grower.TreeGrower;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Marks the time while a sapling grows into a tree, so {@link AlterGroundDecoratorMixin} can tell it from world generation. */
@Mixin(TreeGrower.class)
public abstract class TreeGrowerMixin {
    @Inject(method = "growTree", at = @At("HEAD"))
    private void vplustweaks$startSaplingGrowth(CallbackInfoReturnable<Boolean> cir) {
        LargeTrees.GROWING_FROM_SAPLING.set(true);
    }

    @Inject(method = "growTree", at = @At("RETURN"))
    private void vplustweaks$endSaplingGrowth(CallbackInfoReturnable<Boolean> cir) {
        LargeTrees.GROWING_FROM_SAPLING.set(false);
    }
}
