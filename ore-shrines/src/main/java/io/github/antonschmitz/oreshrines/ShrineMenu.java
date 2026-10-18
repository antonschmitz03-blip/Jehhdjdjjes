package io.github.antonschmitz.oreshrines;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

/**
 * The shrine's overview, shown as a 3-row chest screen so the client needs nothing of its own. Top: the shrine's
 * state (effect, pyramid, range). Middle: one icon per effect with the offering it needs; clicking one offers that
 * item from your inventory. Bottom: what ore is worth to a Wisdom shrine and how much XP it holds.
 */
public class ShrineMenu extends ChestMenu {
    private static final int STATUS_SLOT = 4;
    private static final int[] EFFECT_SLOTS = {10, 11, 12, 14, 15, 16};
    private static final int HELP_SLOT = 13;
    private static final int XP_SLOT = 22;

    private final ServerPlayer player;
    private final ShrineBlockEntity shrine;
    private final SimpleContainer container;
    private int refreshTimer;

    private ShrineMenu(int containerId, Inventory inventory, SimpleContainer container, ServerPlayer player, ShrineBlockEntity shrine) {
        super(MenuType.GENERIC_9x3, containerId, inventory, container, 3);
        this.player = player;
        this.shrine = shrine;
        this.container = container;
        fill();
    }

    public static void open(ServerPlayer player, ShrineBlockEntity shrine) {
        shrine.scan(player.level());
        shrine.showRange(player);
        player.openMenu(new SimpleMenuProvider(
                (containerId, inventory, p) -> new ShrineMenu(containerId, inventory, new SimpleContainer(27), player, shrine),
                Component.translatable("block.oreshrines.shrine_core")));
    }

    private void fill() {
        container.clearContent();
        container.setItem(STATUS_SLOT, statusIcon());
        container.setItem(HELP_SLOT, helpIcon());
        for (int i = 0; i < ShrineEffect.SELECTABLE.length; i++) {
            container.setItem(EFFECT_SLOTS[i], effectIcon(ShrineEffect.SELECTABLE[i]));
        }
        container.setItem(XP_SLOT, xpIcon());
    }

    private ItemStack statusIcon() {
        ShrineEffect effect = shrine.effect();
        ItemStack stack = new ItemStack(OreShrines.SHRINE_CORE_ITEM);
        stack.set(DataComponents.CUSTOM_NAME, plain(Component.translatable("oreshrines.menu.status")).withStyle(ChatFormatting.WHITE));
        List<Component> lore = new ArrayList<>();
        lore.add(line(Component.translatable("oreshrines.menu.effect",
                Component.translatable(effect.nameKey()).withStyle(effect.formatting())), ChatFormatting.GRAY));
        if (shrine.layers() == 0) {
            lore.add(line(Component.translatable("oreshrines.menu.no_pyramid"), ChatFormatting.RED));
            lore.add(line(Component.translatable("oreshrines.menu.no_pyramid.hint"), ChatFormatting.DARK_GRAY));
        } else {
            int size = shrine.layers() * 2 + 1;
            lore.add(line(Component.translatable("oreshrines.menu.pyramid", shrine.layers(), size, size), ChatFormatting.GRAY));
            lore.add(line(Component.translatable("oreshrines.menu.blocks", shrine.ironBlocks(), shrine.goldBlocks(),
                    shrine.diamondBlocks()), ChatFormatting.DARK_GRAY));
            lore.add(line(Component.translatable("oreshrines.menu.range", shrine.radius()), ChatFormatting.GRAY));
            if (shrine.layers() < ShrineBlockEntity.MAX_LAYERS) {
                lore.add(line(Component.translatable("oreshrines.menu.next_layer"), ChatFormatting.DARK_GRAY));
            }
        }
        if (effect == ShrineEffect.NONE) {
            lore.add(line(Component.translatable("oreshrines.menu.pick_effect"), ChatFormatting.YELLOW));
        }
        stack.set(DataComponents.LORE, new ItemLore(lore));
        return stack;
    }

    private ItemStack helpIcon() {
        ItemStack stack = new ItemStack(Items.BOOK);
        stack.set(DataComponents.CUSTOM_NAME, plain(Component.translatable("oreshrines.menu.help")).withStyle(ChatFormatting.WHITE));
        List<Component> lore = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            lore.add(line(Component.translatable("oreshrines.menu.help." + i), ChatFormatting.GRAY));
        }
        stack.set(DataComponents.LORE, new ItemLore(lore));
        return stack;
    }

    private ItemStack effectIcon(ShrineEffect effect) {
        Item offering = effect.offering();
        ItemStack stack = new ItemStack(offering);
        boolean active = shrine.effect() == effect;
        stack.set(DataComponents.CUSTOM_NAME, plain(Component.translatable(effect.nameKey())).withStyle(effect.formatting(), ChatFormatting.BOLD));
        List<Component> lore = new ArrayList<>();
        lore.add(line(Component.translatable("oreshrines.menu.offering", Component.translatable(offering.getDescriptionId())),
                ChatFormatting.YELLOW));
        lore.add(line(Component.translatable(effect.descriptionKey()), ChatFormatting.GRAY));
        if (effect == ShrineEffect.WISDOM) {
            lore.add(line(Component.translatable("oreshrines.effect.wisdom.rate", Math.max(1, shrine.layers())), ChatFormatting.DARK_GRAY));
        }
        lore.add(Component.empty());
        if (active) {
            lore.add(line(Component.translatable("oreshrines.menu.active"), ChatFormatting.GREEN));
            stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        } else if (player.isCreative() || has(offering)) {
            lore.add(line(Component.translatable("oreshrines.menu.click_to_offer"), ChatFormatting.AQUA));
        } else {
            lore.add(line(Component.translatable("oreshrines.menu.missing_offering"), ChatFormatting.DARK_GRAY));
        }
        stack.set(DataComponents.LORE, new ItemLore(lore));
        return stack;
    }

    private ItemStack xpIcon() {
        ItemStack stack = new ItemStack(Items.EXPERIENCE_BOTTLE);
        stack.set(DataComponents.CUSTOM_NAME, plain(Component.translatable("oreshrines.menu.stored",
                shrine.storedXp(), ShrineBlockEntity.MAX_XP)).withStyle(ChatFormatting.AQUA));
        List<Component> lore = new ArrayList<>();
        lore.add(line(Component.translatable("oreshrines.menu.values"), ChatFormatting.GRAY));
        lore.add(line(Component.translatable("oreshrines.menu.value.iron"), ChatFormatting.WHITE));
        lore.add(line(Component.translatable("oreshrines.menu.value.gold"), ChatFormatting.GOLD));
        lore.add(line(Component.translatable("oreshrines.menu.value.diamond"), ChatFormatting.AQUA));
        lore.add(line(Component.translatable("oreshrines.menu.value.blocks"), ChatFormatting.DARK_GRAY));
        lore.add(line(Component.translatable("oreshrines.menu.feed_hint"), ChatFormatting.DARK_GRAY));
        stack.set(DataComponents.LORE, new ItemLore(lore));
        return stack;
    }

    private boolean has(Item item) {
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).is(item)) {
                return true;
            }
        }
        return false;
    }

    private boolean takeOne(Item item) {
        if (player.isCreative()) {
            return true;
        }
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(item)) {
                stack.shrink(1);
                inventory.setChanged();
                return true;
            }
        }
        return false;
    }

    private static MutableComponent plain(MutableComponent component) {
        return component.withStyle(style -> style.withItalic(false));
    }

    private static Component line(MutableComponent component, ChatFormatting color) {
        return plain(component).withStyle(color);
    }

    /** Every click is handled here; nothing in this screen can be picked up or moved. */
    @Override
    public void clicked(int slotIndex, int buttonNum, ContainerInput input, Player clicker) {
        if (input == ContainerInput.PICKUP) {
            for (int i = 0; i < EFFECT_SLOTS.length; i++) {
                if (EFFECT_SLOTS[i] == slotIndex) {
                    select(ShrineEffect.SELECTABLE[i]);
                }
            }
        }
        if (player.containerMenu == this) {
            // Undo whatever the client predicted (picked-up icon, moved items).
            sendAllDataToRemote();
        }
    }

    private void select(ShrineEffect effect) {
        if (shrine.isRemoved() || shrine.effect() == effect) {
            return;
        }
        if (!takeOne(effect.offering())) {
            player.sendSystemMessage(Component.translatable("oreshrines.message.need_offering",
                    Component.translatable(effect.offering().getDescriptionId())).withStyle(ChatFormatting.RED), true);
            return;
        }
        ShrineCoreBlock.applyOffering(player, shrine, effect);
        fill();
    }

    @Override
    public void broadcastChanges() {
        // Keep stored XP, range and "you have this offering" up to date while the screen is open.
        if (++refreshTimer >= 20) {
            refreshTimer = 0;
            fill();
        }
        super.broadcastChanges();
    }

    @Override
    public ItemStack quickMoveStack(Player clicker, int slotIndex) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player clicker) {
        BlockPos pos = shrine.getBlockPos();
        return !shrine.isRemoved() && clicker.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void removed(Player clicker) {
        super.removed(clicker);
        container.clearContent();
    }
}
