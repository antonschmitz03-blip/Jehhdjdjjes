package io.github.antonschmitz.backpacklink.mixin;

import com.tom.storagemod.block.entity.StorageTerminalBlockEntity;
import com.tom.storagemod.inventory.StoredItemStack;
import io.github.antonschmitz.backpacklink.BackpackContents;
import io.github.antonschmitz.backpacklink.BackpackLink;
import io.github.antonschmitz.backpacklink.PullContext;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** When the network can't supply the whole request, the rest comes from the player's backpacks. */
@Mixin(StorageTerminalBlockEntity.class)
abstract class StorageTerminalBlockEntityMixin {
    @Inject(method = "pullStack(Lcom/tom/storagemod/inventory/StoredItemStack;J)Lcom/tom/storagemod/inventory/StoredItemStack;",
            at = @At("RETURN"), cancellable = true)
    private void backpacklink$pullFromBackpack(StoredItemStack stack, long max, CallbackInfoReturnable<StoredItemStack> cir) {
        ServerPlayer player = PullContext.player();
        if (stack == null || player == null || !BackpackLink.isEnabled(player)) {
            return;
        }
        StoredItemStack fromNetwork = cir.getReturnValue();
        long got = fromNetwork == null ? 0 : fromNetwork.getQuantity();
        if (got >= max) {
            return;
        }
        long extra = BackpackContents.extract(player, stack.getStack(), max - got);
        if (extra > 0) {
            cir.setReturnValue(new StoredItemStack(stack.getStack().copyWithCount(1), got + extra));
        }
    }
}
