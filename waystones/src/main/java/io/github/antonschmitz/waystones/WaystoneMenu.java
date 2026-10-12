package io.github.antonschmitz.waystones;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import org.jspecify.annotations.Nullable;

/**
 * The list of discovered waystones, shown as a chest screen with one waystone item per entry, so the mod
 * needs no screen of its own. Clicking an entry teleports. The owner also gets a name tag in the last slot
 * that opens {@link WaystoneRenameMenu}. Too many waystones for one screen get pages with arrows in the
 * bottom row.
 */
public class WaystoneMenu extends ChestMenu {
    private static final int PAGE_SIZE = 45;
    private static final int PAGED_RENAME_SLOT = 49;

    private final ServerPlayer player;
    /** The waystone the menu was opened at, or null for a warp scroll. */
    private final WaystoneData.@Nullable Waystone from;
    private final @Nullable InteractionHand scrollHand;
    private final List<WaystoneData.Waystone> targets;
    private final SimpleContainer container;
    private final boolean canRename;
    private final boolean paged;
    private int page;

    private WaystoneMenu(int containerId, Inventory inventory, SimpleContainer container, int rows, ServerPlayer player,
                         WaystoneData.@Nullable Waystone from, @Nullable InteractionHand scrollHand, List<WaystoneData.Waystone> targets,
                         boolean canRename) {
        super(menuType(rows), containerId, inventory, container, rows);
        this.player = player;
        this.from = from;
        this.scrollHand = scrollHand;
        this.targets = targets;
        this.container = container;
        this.canRename = canRename;
        this.paged = targets.size() + (canRename ? 1 : 0) > 54;
        fill();
    }

    /** Owners (and creative players) can rename the waystone the list was opened at. */
    static boolean canRename(ServerPlayer player, WaystoneData.@Nullable Waystone waystone) {
        return waystone != null && (player.isCreative() || player.getUUID().equals(waystone.owner()));
    }

    /** Opens the list at a waystone ({@code from}) or for the warp scroll in {@code scrollHand}. */
    public static void open(ServerPlayer player, WaystoneData.@Nullable Waystone from, @Nullable InteractionHand scrollHand) {
        List<WaystoneData.Waystone> targets = targets(player, from);
        if (targets.isEmpty() && !canRename(player, from)) {
            player.sendSystemMessage(Component.translatable("simplewaystones.message.none_discovered").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        Component title = from != null ? from.displayName() : Component.translatable("item.simplewaystones.warp_scroll");
        player.openMenu(new SimpleMenuProvider((containerId, inventory, p) -> create(containerId, inventory, player, from, scrollHand),
                title));
    }

    /** Builds the menu directly. Package-private for {@link SelfTest}, since a fake player can't open screens. */
    static WaystoneMenu create(int containerId, Inventory inventory, ServerPlayer player, WaystoneData.@Nullable Waystone from,
                               @Nullable InteractionHand scrollHand) {
        List<WaystoneData.Waystone> targets = targets(player, from);
        boolean canRename = canRename(player, from);
        int rows = Math.max(1, Math.min(6, (targets.size() + (canRename ? 1 : 0) + 8) / 9));
        return new WaystoneMenu(containerId, inventory, new SimpleContainer(rows * 9), rows, player, from, scrollHand, targets, canRename);
    }

    private static List<WaystoneData.Waystone> targets(ServerPlayer player, WaystoneData.@Nullable Waystone from) {
        List<WaystoneData.Waystone> targets = new ArrayList<>(WaystoneData.get().discovered(player.getUUID()));
        if (from != null) {
            targets.removeIf(waystone -> waystone.key().equals(from.key()));
        }
        return targets;
    }

    private static MenuType<ChestMenu> menuType(int rows) {
        return switch (rows) {
            case 1 -> MenuType.GENERIC_9x1;
            case 2 -> MenuType.GENERIC_9x2;
            case 3 -> MenuType.GENERIC_9x3;
            case 4 -> MenuType.GENERIC_9x4;
            case 5 -> MenuType.GENERIC_9x5;
            default -> MenuType.GENERIC_9x6;
        };
    }

    private int firstIndex() {
        return paged ? page * PAGE_SIZE : 0;
    }

    private int shownCount() {
        return paged ? PAGE_SIZE : container.getContainerSize() - (canRename ? 1 : 0);
    }

    private int renameSlot() {
        return paged ? PAGED_RENAME_SLOT : container.getContainerSize() - 1;
    }

    private void fill() {
        container.clearContent();
        for (int slot = 0; slot < shownCount(); slot++) {
            int index = firstIndex() + slot;
            if (index < targets.size()) {
                container.setItem(slot, icon(targets.get(index)));
            }
        }
        if (canRename) {
            ItemStack tag = new ItemStack(Items.NAME_TAG);
            tag.set(DataComponents.CUSTOM_NAME, plain(Component.translatable("simplewaystones.menu.rename")).withStyle(ChatFormatting.YELLOW));
            container.setItem(renameSlot(), tag);
        }
        if (paged) {
            if (page > 0) {
                container.setItem(45, arrow("simplewaystones.menu.previous"));
            }
            if (firstIndex() + PAGE_SIZE < targets.size()) {
                container.setItem(53, arrow("simplewaystones.menu.next"));
            }
        }
    }

    private ItemStack icon(WaystoneData.Waystone waystone) {
        ItemStack stack = new ItemStack(SimpleWaystones.WAYSTONE_ITEM);
        stack.set(DataComponents.CUSTOM_NAME, plain(waystone.displayName().copy()).withStyle(ChatFormatting.AQUA));
        List<Component> lore = new ArrayList<>();
        BlockPos pos = waystone.pos();
        lore.add(plain(Component.translatable("simplewaystones.menu.position", pos.getX(), pos.getY(), pos.getZ()))
                .withStyle(ChatFormatting.GRAY));
        if (!waystone.dimension().equals(WaystoneData.dimensionId(player.level()))) {
            lore.add(plain(Component.translatable("simplewaystones.menu.dimension", dimensionName(waystone.dimension())))
                    .withStyle(ChatFormatting.DARK_PURPLE));
        }
        if (scrollHand != null) {
            lore.add(plain(Component.translatable("simplewaystones.menu.uses_scroll")).withStyle(ChatFormatting.GREEN));
        } else {
            int cost = WaystoneTeleport.cost(player, waystone);
            if (cost == 0) {
                lore.add(plain(Component.translatable("simplewaystones.menu.free")).withStyle(ChatFormatting.GREEN));
            } else {
                lore.add(plain(Component.translatable("simplewaystones.menu.cost", cost))
                        .withStyle(player.experienceLevel >= cost ? ChatFormatting.YELLOW : ChatFormatting.RED));
            }
        }
        stack.set(DataComponents.LORE, new ItemLore(lore));
        return stack;
    }

    private static Component dimensionName(String dimension) {
        return switch (dimension) {
            case "minecraft:overworld" -> Component.translatable("simplewaystones.dimension.overworld");
            case "minecraft:the_nether" -> Component.translatable("simplewaystones.dimension.the_nether");
            case "minecraft:the_end" -> Component.translatable("simplewaystones.dimension.the_end");
            default -> Component.literal(dimension);
        };
    }

    private static ItemStack arrow(String key) {
        ItemStack stack = new ItemStack(Items.ARROW);
        stack.set(DataComponents.CUSTOM_NAME, plain(Component.translatable(key)).withStyle(ChatFormatting.WHITE));
        return stack;
    }

    private static MutableComponent plain(MutableComponent component) {
        return component.withStyle(style -> style.withItalic(false));
    }

    /** Every click is handled here; nothing in this screen can be picked up or moved. */
    @Override
    public void clicked(int slotIndex, int buttonNum, ContainerInput input, Player clicker) {
        if (slotIndex >= 0 && slotIndex < container.getContainerSize() && input == ContainerInput.PICKUP) {
            select(slotIndex);
        }
        if (player.containerMenu == this) {
            // Undo whatever the client predicted (picked-up icon, moved items).
            sendAllDataToRemote();
        }
    }

    /** Package-private for {@link SelfTest}. */
    void select(int slot) {
        if (canRename && slot == renameSlot() && from != null) {
            player.closeContainer();
            WaystoneRenameMenu.open(player, from);
            return;
        }
        if (paged && slot == 45 && page > 0) {
            page--;
            fill();
            return;
        }
        if (paged && slot == 53 && firstIndex() + PAGE_SIZE < targets.size()) {
            page++;
            fill();
            return;
        }
        if (slot >= shownCount()) {
            return;
        }
        int index = firstIndex() + slot;
        if (index >= targets.size()) {
            return;
        }
        WaystoneData.Waystone target = targets.get(index);
        ItemStack scroll = scrollHand != null ? player.getItemInHand(scrollHand) : ItemStack.EMPTY;
        if (scrollHand != null && !scroll.is(SimpleWaystones.WARP_SCROLL)) {
            player.closeContainer();
            return;
        }
        player.closeContainer();
        if (WaystoneTeleport.teleport(player, target, scrollHand != null) && scrollHand != null) {
            scroll.consume(1, player);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player clicker, int slotIndex) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player clicker) {
        if (from == null) {
            return true;
        }
        BlockPos pos = from.pos();
        return clicker.level().getBlockState(pos).is(SimpleWaystones.WAYSTONE)
                && clicker.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void removed(Player clicker) {
        super.removed(clicker);
        // The icons are only for show; make sure none end up anywhere.
        container.clearContent();
    }
}
