package io.github.antonschmitz.tbaddons;

import com.tiviacz.travelersbackpack.attachment.AttachmentUtils;
import com.tiviacz.travelersbackpack.inventory.BackpackWrapper;
import com.tiviacz.travelersbackpack.inventory.handler.ItemStackHandler;
import io.github.antonschmitz.tbaddons.upgrade.ToolSwapperUpgrade;
import java.util.Optional;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Swaps the held tool for a suitable one stored in the worn backpack when the player starts breaking a
 * block the held tool is unsuitable for. Runs on the server only; the client picks up the new hand item
 * through the normal inventory sync and restarts mining with it.
 */
public final class ToolSwapHandler {
    /** Items that may be swapped out of the hand and into it. Weapons (swords, maces, ...) are deliberately not in it. */
    public static final TagKey<Item> SWAPPABLE_TOOLS = TagKey.create(Registries.ITEM, TBAddons.id("swappable_tools"));

    private ToolSwapHandler() {
    }

    public static void register() {
        AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> {
            if (!level.isClientSide() && hand == InteractionHand.MAIN_HAND && player instanceof ServerPlayer serverPlayer) {
                trySwap(serverPlayer, level.getBlockState(pos));
            }
            return InteractionResult.PASS;
        });
    }

    private static void trySwap(ServerPlayer player, BlockState state) {
        if (player.isCreative() || player.isSpectator() || state.isAir()) {
            return;
        }

        ItemStack held = player.getMainHandItem();
        if (!isSwappableTool(held) || isSuitable(held, state)) {
            return;
        }

        BackpackWrapper wrapper = AttachmentUtils.getBackpackWrapper(player);
        if (wrapper == null) {
            return;
        }

        Optional<ToolSwapperUpgrade> upgrade = wrapper.getUpgradeManager().getUpgrade(ToolSwapperUpgrade.class);
        if (upgrade.isEmpty() || !upgrade.get().isEnabled(upgrade.get())) {
            return;
        }

        wrapper.loadAdditionally(0);
        wrapper.loadAdditionally(2);

        // First suitable tool wins, tool slots before the main storage.
        ItemStackHandler bestHandler = null;
        int bestSlot = -1;
        search:
        for (ItemStackHandler handler : new ItemStackHandler[]{wrapper.getTools(), wrapper.getStorage()}) {
            if (handler == null) {
                continue;
            }
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack candidate = handler.getStackInSlot(slot);
                if (isSwappableTool(candidate) && isSuitable(candidate, state) && handler.isItemValid(slot, held)) {
                    bestHandler = handler;
                    bestSlot = slot;
                    break search;
                }
            }
        }

        if (bestHandler == null) {
            return;
        }

        ItemStack newTool = bestHandler.getStackInSlot(bestSlot).copy();
        bestHandler.setStackInSlot(bestSlot, held.copy());
        player.setItemInHand(InteractionHand.MAIN_HAND, newTool);
    }

    /**
     * A tool is suitable if it can harvest the block (for blocks that need a specific tool) or if it is an
     * effective tool for it. Mining speed and enchantments are deliberately ignored, so e.g. a silk touch
     * iron pickaxe is never swapped for a diamond one.
     */
    private static boolean isSuitable(ItemStack stack, BlockState state) {
        if (state.requiresCorrectToolForDrops()) {
            return stack.isCorrectToolForDrops(state);
        }
        return stack.isCorrectToolForDrops(state) || stack.getDestroySpeed(state) > 1.0F;
    }

    private static boolean isSwappableTool(ItemStack stack) {
        return !stack.isEmpty() && stack.is(SWAPPABLE_TOOLS);
    }
}
