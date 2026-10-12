package io.github.antonschmitz.vplustweaks;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Happy ghasts can carry a double chest (54 slots) or an ender chest, similar to donkeys:
 * right-click with a chest or ender chest in hand to equip it, sneak + right-click to open it.
 * The chest (and a double chest's contents) drop when the ghast dies.
 */
public final class GhastChest {
    public static final int NONE = 0;
    public static final int CHEST = 1;
    public static final int ENDER_CHEST = 2;

    private static final int SLOTS = 54;

    private static final AttachmentType<List<ItemStack>> CONTENTS = AttachmentRegistry.createPersistent(
            Identifier.fromNamespaceAndPath(VanillaPlusTweaks.MOD_ID, "ghast_chest"),
            ItemStack.OPTIONAL_CODEC.listOf());

    /** Which chest the ghast carries; synced to clients so they can draw it on the ghast's back. */
    public static final AttachmentType<Integer> KIND = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(VanillaPlusTweaks.MOD_ID, "ghast_chest_kind"),
            builder -> builder.persistent(Codec.INT).syncWith(ByteBufCodecs.VAR_INT, AttachmentSyncPredicate.all()));

    private GhastChest() {}

    public static void register() {
        UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (!isHappyGhast(entity) || player.isSpectator()) {
                return InteractionResult.PASS;
            }
            int kind = entity.getAttachedOrElse(KIND, NONE);
            ItemStack held = player.getItemInHand(hand);

            if (kind == NONE) {
                int newKind = held.is(Items.CHEST) ? CHEST : held.is(Items.ENDER_CHEST) ? ENDER_CHEST : NONE;
                if (newKind == NONE || (entity instanceof LivingEntity living && living.isBaby())) {
                    return InteractionResult.PASS;
                }
                if (!level.isClientSide()) {
                    if (newKind == CHEST) {
                        List<ItemStack> empty = new ArrayList<>();
                        for (int i = 0; i < SLOTS; i++) {
                            empty.add(ItemStack.EMPTY);
                        }
                        entity.setAttached(CONTENTS, empty);
                    }
                    entity.setAttached(KIND, newKind);
                    if (!player.hasInfiniteMaterials()) {
                        held.shrink(1);
                    }
                }
                return InteractionResult.SUCCESS;
            }

            if (player.isSecondaryUseActive()) {
                if (!level.isClientSide()) {
                    open(player, entity, kind);
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!isHappyGhast(entity)) {
                return;
            }
            int kind = entity.getAttachedOrElse(KIND, NONE);
            if (kind == NONE) {
                return;
            }
            List<ItemStack> contents = entity.getAttached(CONTENTS);
            if (contents != null) {
                for (ItemStack stack : contents) {
                    if (!stack.isEmpty()) {
                        drop(entity, stack);
                    }
                }
            }
            drop(entity, new ItemStack(kind == ENDER_CHEST ? Items.ENDER_CHEST : Items.CHEST));
            entity.removeAttached(CONTENTS);
            entity.removeAttached(KIND);
        });
    }

    public static boolean isHappyGhast(Entity entity) {
        return EntityType.getKey(entity.getType()).getPath().equals("happy_ghast");
    }

    static void drop(Entity entity, ItemStack stack) {
        Containers.dropItemStack(entity.level(), entity.getX(), entity.getY(), entity.getZ(), stack);
    }

    private static void open(Player player, Entity ghast, int kind) {
        if (kind == ENDER_CHEST) {
            player.openMenu(new SimpleMenuProvider(
                    (id, inventory, p) -> ChestMenu.threeRows(id, inventory, p.getEnderChestInventory()),
                    Component.translatable("container.enderchest")));
            return;
        }
        List<ItemStack> contents = ghast.getAttached(CONTENTS);
        SimpleContainer container = new SimpleContainer(SLOTS) {
            @Override
            public void setChanged() {
                super.setChanged();
                List<ItemStack> saved = new ArrayList<>(SLOTS);
                for (int i = 0; i < SLOTS; i++) {
                    saved.add(getItem(i).copy());
                }
                ghast.setAttached(CONTENTS, saved);
            }
        };
        for (int i = 0; contents != null && i < SLOTS && i < contents.size(); i++) {
            container.setItem(i, contents.get(i).copy());
        }
        player.openMenu(new SimpleMenuProvider(
                (id, inventory, p) -> ChestMenu.sixRows(id, inventory, container),
                Component.literal("Happy Ghast")));
    }
}
