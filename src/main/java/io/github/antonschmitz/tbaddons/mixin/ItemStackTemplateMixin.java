package io.github.antonschmitz.tbaddons.mixin;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Container contents (and so the backpack storage) are stored as ItemStackTemplates. Vanilla only accepts
 * counts up to 99 in their codec and turns any stack above its max stack size into EMPTY when creating
 * it, which would delete upgraded backpack stacks on save/load. Allow both.
 */
@Mixin(ItemStackTemplate.class)
public abstract class ItemStackTemplateMixin {
    @ModifyArg(
            method = "lambda$static$0",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/ExtraCodecs;intRange(II)Lcom/mojang/serialization/Codec;"),
            index = 1
    )
    private static int tbaddons$raiseMaxCount(int max) {
        return Integer.MAX_VALUE;
    }

    @Inject(method = "validate", at = @At("HEAD"), cancellable = true)
    private void tbaddons$allowOversizedStacks(ItemStack result, CallbackInfoReturnable<ItemStack> cir) {
        if (!result.isEmpty() && result.getCount() > result.getMaxStackSize()
                && ItemStack.validateStrict(result.copyWithCount(result.getMaxStackSize())).isSuccess()) {
            cir.setReturnValue(result);
        }
    }
}
