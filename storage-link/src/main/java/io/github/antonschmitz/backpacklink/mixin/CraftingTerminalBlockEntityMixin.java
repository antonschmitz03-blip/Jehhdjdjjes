package io.github.antonschmitz.backpacklink.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.tom.storagemod.block.entity.CraftingTerminalBlockEntity;
import io.github.antonschmitz.backpacklink.PullContext;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;

/** Refilling the grid after a craft may use backpack items. */
@Mixin(CraftingTerminalBlockEntity.class)
abstract class CraftingTerminalBlockEntityMixin {
    @WrapMethod(method = "craft(Lnet/minecraft/world/entity/player/Player;)V")
    private void backpacklink$craftForPlayer(Player player, Operation<Void> original) {
        PullContext.with(player, () -> original.call(player));
    }
}
