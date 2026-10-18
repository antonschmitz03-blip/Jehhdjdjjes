package io.github.antonschmitz.waystones;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;

/**
 * Renaming reuses the vanilla anvil screen, so it works without anything on the client: the waystone sits in
 * the left slot, you type the new name, and clicking the result saves it. Free, and nothing can be taken out.
 */
public class WaystoneRenameMenu extends AnvilMenu {
    private static final int RESULT_SLOT = 2;

    private final ServerPlayer player;
    private final WaystoneData.Waystone waystone;
    private final ItemStack icon;
    private String name;

    private WaystoneRenameMenu(int containerId, Inventory inventory, ServerPlayer player, WaystoneData.Waystone waystone) {
        super(containerId, inventory, ContainerLevelAccess.NULL);
        this.player = player;
        this.waystone = waystone;
        this.name = waystone.displayName().getString();
        this.icon = new ItemStack(SimpleWaystones.WAYSTONE_ITEM);
        icon.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        inputSlots.setItem(0, icon.copy());
        createResult();
    }

    public static void open(ServerPlayer player, WaystoneData.Waystone waystone) {
        player.openMenu(new SimpleMenuProvider((containerId, inventory, p) -> create(containerId, inventory, player, waystone),
                Component.translatable("simplewaystones.menu.rename_title")));
    }

    /** Package-private for {@link SelfTest}. */
    static WaystoneRenameMenu create(int containerId, Inventory inventory, ServerPlayer player, WaystoneData.Waystone waystone) {
        return new WaystoneRenameMenu(containerId, inventory, player, waystone);
    }

    @Override
    public boolean setItemName(String newName) {
        name = newName == null ? "" : newName.strip();
        if (name.length() > 50) {
            name = name.substring(0, 50);
        }
        createResult();
        // The client predicts a vanilla rename (1 level); send the real state back so it shows no cost.
        sendAllDataToRemote();
        return true;
    }

    @Override
    public void createResult() {
        if (icon == null) {
            return;
        }
        ItemStack result = ItemStack.EMPTY;
        if (!name.isEmpty()) {
            result = icon.copy();
            result.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        }
        resultSlots.setItem(0, result);
    }

    @Override
    public void clicked(int slotIndex, int buttonNum, ContainerInput input, Player clicker) {
        if (slotIndex == RESULT_SLOT && !name.isEmpty()) {
            WaystoneData.get().rename(waystone, name);
            player.closeContainer();
            player.sendSystemMessage(Component.translatable("simplewaystones.message.renamed", waystone.displayName())
                    .withStyle(ChatFormatting.AQUA), true);
            return;
        }
        if (player.containerMenu == this) {
            sendAllDataToRemote();
        }
    }

    @Override
    public ItemStack quickMoveStack(Player clicker, int slotIndex) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player clicker) {
        BlockPos pos = waystone.pos();
        return clicker.level().getBlockState(pos).is(SimpleWaystones.WAYSTONE)
                && clicker.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void removed(Player clicker) {
        // The waystone in the left slot is only for show; don't hand it to the player.
        inputSlots.clearContent();
        resultSlots.clearContent();
        super.removed(clicker);
    }
}
