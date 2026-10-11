package io.github.antonschmitz.backpacklink.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.tom.storagemod.block.entity.StorageTerminalBlockEntity;
import com.tom.storagemod.inventory.StoredItemStack;
import com.tom.storagemod.inventory.TerminalItemStack;
import com.tom.storagemod.menu.StorageTerminalMenu;
import io.github.antonschmitz.backpacklink.BackpackContents;
import io.github.antonschmitz.backpacklink.BackpackLink;
import io.github.antonschmitz.backpacklink.PullContext;
import io.github.antonschmitz.backpacklink.TerminalMenuDuck;
import io.github.antonschmitz.backpacklink.network.BackpackViewPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Every Tom's terminal screen (storage, crafting, both wireless terminals) uses this menu. Each open menu
 * belongs to one player, so the backpack items are merged into what this menu syncs, not into the shared
 * block entity, and clicks handled by {@code receive} pull for this menu's player.
 */
@Mixin(StorageTerminalMenu.class)
abstract class StorageTerminalMenuMixin implements TerminalMenuDuck {
    @Shadow
    protected Inventory pinv;

    @Unique
    private Map<TerminalItemStack, Long> backpacklink$snapshot;
    @Unique
    private boolean backpacklink$sentEnabled;
    @Unique
    private int backpacklink$networkChange = Integer.MIN_VALUE;
    @Unique
    private int backpacklink$changeId;
    @Unique
    private Map<StoredItemStack, TerminalItemStack> backpacklink$merged;

    @Override
    public Player backpacklink$player() {
        return pinv.player;
    }

    @WrapOperation(method = "broadcastChanges", at = @At(value = "INVOKE",
            target = "Lcom/tom/storagemod/block/entity/StorageTerminalBlockEntity;getStacks()Ljava/util/Map;"))
    private Map<StoredItemStack, TerminalItemStack> backpacklink$addBackpackItems(StorageTerminalBlockEntity te,
            Operation<Map<StoredItemStack, TerminalItemStack>> original) {
        Map<StoredItemStack, TerminalItemStack> network = original.call(te);
        if (!(pinv.player instanceof ServerPlayer player)) {
            return network;
        }
        boolean enabled = BackpackLink.isEnabled(player);
        Map<TerminalItemStack, Long> snapshot = enabled ? BackpackContents.snapshot(player) : Map.of();
        boolean backpackChanged = !snapshot.equals(backpacklink$snapshot) || enabled != backpacklink$sentEnabled;
        if (backpackChanged) {
            backpacklink$snapshot = snapshot;
            backpacklink$sentEnabled = enabled;
            if (ServerPlayNetworking.canSend(player, BackpackViewPayload.TYPE)) {
                List<BackpackViewPayload.Entry> entries = new ArrayList<>(snapshot.size());
                snapshot.forEach((stack, count) -> entries.add(new BackpackViewPayload.Entry(stack.getStack(), count)));
                ServerPlayNetworking.send(player, new BackpackViewPayload(enabled, entries));
            }
        }
        int networkChange = te.getChangeCount();
        if (backpackChanged || networkChange != backpacklink$networkChange || backpacklink$merged == null) {
            backpacklink$networkChange = networkChange;
            backpacklink$merged = BackpackContents.merge(network, snapshot);
            backpacklink$changeId++;
        }
        return backpacklink$merged;
    }

    // Tom's sync only resends the list when this id changes, so it has to change with the backpacks too.
    @WrapOperation(method = "broadcastChanges", at = @At(value = "INVOKE",
            target = "Lcom/tom/storagemod/block/entity/StorageTerminalBlockEntity;getChangeCount()I"))
    private int backpacklink$changeId(StorageTerminalBlockEntity te, Operation<Integer> original) {
        return pinv.player instanceof ServerPlayer ? backpacklink$changeId : original.call(te);
    }

    @WrapMethod(method = "receive(Lnet/minecraft/world/level/storage/ValueInput;)V")
    private void backpacklink$receiveForPlayer(ValueInput message, Operation<Void> original) {
        PullContext.with(pinv.player, () -> original.call(message));
    }
}
