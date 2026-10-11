package io.github.antonschmitz.tbaddons.mixin;

import net.minecraft.world.item.ItemStackTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Container contents (and so the backpack storage) are saved as ItemStackTemplates, whose codec only
 * accepts counts up to 99. Lift that cap so stacks of up to 512 can be saved and loaded.
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
}
