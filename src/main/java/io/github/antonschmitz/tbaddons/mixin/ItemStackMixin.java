package io.github.antonschmitz.tbaddons.mixin;

import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Same as {@link ItemStackTemplateMixin} for ItemStack.MAP_CODEC (used when a backpack handler is serialized directly). */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @ModifyArg(
            method = "lambda$static$1",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/ExtraCodecs;intRange(II)Lcom/mojang/serialization/Codec;"),
            index = 1
    )
    private static int tbaddons$raiseMaxCount(int max) {
        return Integer.MAX_VALUE;
    }
}
