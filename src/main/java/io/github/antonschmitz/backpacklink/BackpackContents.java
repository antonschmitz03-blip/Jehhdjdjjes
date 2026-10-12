package io.github.antonschmitz.backpacklink;

import com.tiviacz.travelersbackpack.attachment.AttachmentUtils;
import com.tiviacz.travelersbackpack.init.ModDataComponents;
import com.tiviacz.travelersbackpack.inventory.BackpackWrapper;
import com.tiviacz.travelersbackpack.inventory.handler.ItemStackHandler;
import com.tiviacz.travelersbackpack.item.TravelersBackpackItem;
import com.tom.storagemod.inventory.StoredItemStack;
import com.tom.storagemod.inventory.TerminalItemStack;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * Reads and takes items from the main storage of the backpack a player wears and of every backpack item in
 * their inventory. Tool slots, upgrades and backpacks inside other containers are left alone.
 */
public final class BackpackContents {
    private BackpackContents() {
    }

    /** Item (count 1 template) to total count over all the player's backpacks. */
    public static Map<TerminalItemStack, Long> snapshot(Player player) {
        Map<TerminalItemStack, Long> counts = new HashMap<>();
        add(counts, AttachmentUtils.getWearingBackpack(player));
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            add(counts, inv.getItem(i));
        }
        return counts;
    }

    private static void add(Map<TerminalItemStack, Long> counts, ItemStack backpack) {
        if (!(backpack.getItem() instanceof TravelersBackpackItem)) {
            return;
        }
        ItemContainerContents contents = backpack.get(ModDataComponents.BACKPACK_CONTAINER);
        if (contents == null) {
            return;
        }
        contents.nonEmptyItemCopyStream().forEach(s -> counts.merge(new TerminalItemStack(s.copyWithCount(1), 1), (long) s.getCount(), Long::sum));
    }

    /** The terminal's network list with the backpack items added on top. Never changes {@code network}. */
    public static Map<StoredItemStack, TerminalItemStack> merge(Map<StoredItemStack, TerminalItemStack> network, Map<TerminalItemStack, Long> backpack) {
        if (backpack.isEmpty()) {
            return network;
        }
        Map<StoredItemStack, TerminalItemStack> merged = new HashMap<>(network);
        backpack.forEach((key, count) -> {
            TerminalItemStack stack = new TerminalItemStack(key.getStack(), count);
            merged.merge(stack, stack, TerminalItemStack::merge);
        });
        return merged;
    }

    /** Takes up to {@code max} items matching {@code template}, worn backpack first. Returns how many were taken. */
    public static long extract(ServerPlayer player, ItemStack template, long max) {
        long taken = 0;
        if (AttachmentUtils.isWearingBackpack(player)) {
            BackpackWrapper worn = AttachmentUtils.getBackpackWrapper(player, AttachmentUtils.STORAGE_ONLY.get());
            if (worn != null && worn.getStorage() != null) {
                taken += extract(worn.getStorage(), template, max);
                if (taken > 0) {
                    AttachmentUtils.synchronise(player);
                }
            }
        }
        Inventory inv = player.getInventory();
        boolean invChanged = false;
        for (int i = 0; i < inv.getContainerSize() && taken < max; i++) {
            ItemStack backpack = inv.getItem(i);
            if (!(backpack.getItem() instanceof TravelersBackpackItem) || !contains(backpack, template)) {
                continue;
            }
            BackpackWrapper wrapper = new BackpackWrapper(backpack, 1, player, player.level(), AttachmentUtils.STORAGE_ONLY.get());
            long n = extract(wrapper.getStorage(), template, max - taken);
            taken += n;
            invChanged |= n > 0;
        }
        if (invChanged) {
            inv.setChanged();
        }
        return taken;
    }

    private static boolean contains(ItemStack backpack, ItemStack template) {
        ItemContainerContents contents = backpack.get(ModDataComponents.BACKPACK_CONTAINER);
        return contents != null && contents.nonEmptyItemCopyStream().anyMatch(s -> ItemStack.isSameItemSameComponents(s, template));
    }

    private static long extract(ItemStackHandler storage, ItemStack template, long max) {
        long taken = 0;
        for (int slot = 0; slot < storage.getSlots() && taken < max; slot++) {
            while (taken < max && ItemStack.isSameItemSameComponents(storage.getStackInSlot(slot), template)) {
                ItemStack out = storage.extractItem(slot, (int) Math.min(max - taken, Integer.MAX_VALUE), false);
                if (out.isEmpty()) {
                    break;
                }
                taken += out.getCount();
            }
        }
        return taken;
    }
}
