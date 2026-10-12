package io.github.antonschmitz.backpacklink.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.tom.storagemod.menu.CraftingTerminalMenu;
import io.github.antonschmitz.backpacklink.PullContext;
import io.github.antonschmitz.backpacklink.TerminalMenuDuck;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.storage.ValueInput;
import org.spongepowered.asm.mixin.Mixin;

/** Recipe filling (recipe book and JEI) may use backpack items. */
@Mixin(CraftingTerminalMenu.class)
abstract class CraftingTerminalMenuMixin {
    // The JEI/EMI "fill" message is handled after super.receive returns, so wrap the override too.
    @WrapMethod(method = "receive(Lnet/minecraft/world/level/storage/ValueInput;)V")
    private void backpacklink$receiveForPlayer(ValueInput message, Operation<Void> original) {
        PullContext.with(((TerminalMenuDuck) (Object) this).backpacklink$player(), () -> original.call(message));
    }

    @WrapMethod(method = "handlePlacement")
    private RecipeBookMenu.PostPlaceAction backpacklink$placeForPlayer(boolean useMaxItems, boolean creative, RecipeHolder<?> recipe,
            ServerLevel level, Inventory inventory, Operation<RecipeBookMenu.PostPlaceAction> original) {
        return PullContext.with(inventory.player, () -> original.call(useMaxItems, creative, recipe, level, inventory));
    }
}
