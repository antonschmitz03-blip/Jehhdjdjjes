package io.github.antonschmitz.tbaddons.mixin.compat;

import eu.pb4.trinkets.api.TrinketDropRule;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.TrinketsApi;
import io.github.antonschmitz.tbaddons.Soulbound;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Backpack worn in the Trinkets back slot (Traveler's Backpack's default when Trinkets is installed): keep a
 * soulbound backpack in its slot on death. Trinkets copies the slots to the respawned player.
 */
@Mixin(value = TrinketsApi.class, remap = false)
public abstract class TrinketsApiMixin {
    @Inject(method = "getDropRule", at = @At("HEAD"), cancellable = true)
    private static void tbaddons$keepSoulbound(ItemStack stack, TrinketSlotAccess slot, LivingEntity entity, boolean keepInventory,
                                               CallbackInfoReturnable<TrinketDropRule> cir) {
        if (entity instanceof Player && Soulbound.isSoulbound(stack)) {
            cir.setReturnValue(TrinketDropRule.KEEP);
        }
    }
}
