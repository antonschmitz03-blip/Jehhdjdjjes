package io.github.antonschmitz.tbaddons.mixin;

import com.tiviacz.travelersbackpack.util.BackpackDeathHelper;
import io.github.antonschmitz.tbaddons.Soulbound;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Backpack worn through Traveler's Backpack's own slot (no Trinkets/Accessories): skip dropping or placing
 * a soulbound backpack. It then stays attached to the dead player and Traveler's Backpack re-equips it on
 * respawn, the same way it does with the keepInventory game rule.
 */
@Mixin(value = BackpackDeathHelper.class, remap = false)
public abstract class BackpackDeathHelperMixin {
    @Inject(method = "onPlayerDrops", at = @At("HEAD"), cancellable = true)
    private static void tbaddons$keepSoulbound(Level level, Player player, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (Soulbound.isSoulbound(stack)) {
            cir.setReturnValue(false);
        }
    }
}
