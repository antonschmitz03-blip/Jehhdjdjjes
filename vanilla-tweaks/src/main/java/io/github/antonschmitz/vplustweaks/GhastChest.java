package io.github.antonschmitz.vplustweaks;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Happy ghasts can carry a double chest (54 slots), similar to donkeys:
 * right-click with a chest in hand to equip it, sneak + right-click to open it.
 * The contents drop (together with the chest) when the ghast dies.
 */
public final class GhastChest {
    private static final int SLOTS = 54;

    private static final AttachmentType<List<ItemStack>> CHEST = AttachmentRegistry.createPersistent(
            Identifier.fromNamespaceAndPath(VanillaPlusTweaks.MOD_ID, "ghast_chest"),
            ItemStack.OPTIONAL_CODEC.listOf());

    private GhastChest() {}

    public static void register() {
        UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (entity.getType() != EntityType.HAPPY_GHAST || player.isSpectator()) {
                return InteractionResult.PASS;
            }
            List<ItemStack> contents = entity.getAttached(CHEST);
            ItemStack held = player.getItemInHand(hand);

            if (contents == null) {
                if (!held.is(Items.CHEST)) {
                    return InteractionResult.PASS;
                }
                if (!level.isClientSide()) {
                    List<ItemStack> empty = new ArrayList<>();
                    for (int i = 0; i < SLOTS; i++) {
                        empty.add(ItemStack.EMPTY);
                    }
                    entity.setAttached(CHEST, empty);
                    if (!player.hasInfiniteMaterials()) {
                        held.shrink(1);
                    }
                }
                return InteractionResult.SUCCESS;
            }

            if (player.isSecondaryUseActive()) {
                if (!level.isClientSide()) {
                    open(player, entity, contents);
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity.getType() != EntityType.HAPPY_GHAST) {
                return;
            }
            List<ItemStack> contents = entity.getAttached(CHEST);
            if (contents == null) {
                return;
            }
            for (ItemStack stack : contents) {
                if (!stack.isEmpty()) {
                    Containers.dropItemStack(entity.level(), entity.getX(), entity.getY(), entity.getZ(), stack);
                }
            }
            Containers.dropItemStack(entity.level(), entity.getX(), entity.getY(), entity.getZ(), new ItemStack(Items.CHEST));
            entity.removeAttached(CHEST);
        });
    }

    private static void open(Player player, net.minecraft.world.entity.Entity ghast, List<ItemStack> contents) {
        SimpleContainer container = new SimpleContainer(SLOTS);
        for (int i = 0; i < SLOTS && i < contents.size(); i++) {
            container.setItem(i, contents.get(i).copy());
        }
        container.addListener(changed -> {
            List<ItemStack> saved = new ArrayList<>(SLOTS);
            for (int i = 0; i < SLOTS; i++) {
                saved.add(container.getItem(i).copy());
            }
            ghast.setAttached(CHEST, saved);
        });
        player.openMenu(new SimpleMenuProvider(
                (id, inventory, p) -> ChestMenu.sixRows(id, inventory, container),
                Component.literal("Happy Ghast")));
    }
}
