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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
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

    /** Synced to clients so they can draw the chest on the ghast's back. */
    public static final AttachmentType<Boolean> HAS_CHEST = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(VanillaPlusTweaks.MOD_ID, "ghast_has_chest"),
            builder -> builder.persistent(Codec.BOOL).syncWith(ByteBufCodecs.BOOL, AttachmentSyncPredicate.all()));

    private GhastChest() {}

    public static void register() {
        UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (!isHappyGhast(entity) || player.isSpectator()) {
                return InteractionResult.PASS;
            }
            boolean hasChest = entity.getAttachedOrElse(HAS_CHEST, false);
            ItemStack held = player.getItemInHand(hand);

            if (!hasChest) {
                if (!held.is(Items.CHEST) || (entity instanceof LivingEntity living && living.isBaby())) {
                    return InteractionResult.PASS;
                }
                if (!level.isClientSide()) {
                    List<ItemStack> empty = new ArrayList<>();
                    for (int i = 0; i < SLOTS; i++) {
                        empty.add(ItemStack.EMPTY);
                    }
                    entity.setAttached(CHEST, empty);
                    entity.setAttached(HAS_CHEST, true);
                    if (!player.hasInfiniteMaterials()) {
                        held.shrink(1);
                    }
                }
                return InteractionResult.SUCCESS;
            }

            if (player.isSecondaryUseActive()) {
                if (!level.isClientSide()) {
                    List<ItemStack> contents = entity.getAttached(CHEST);
                    open(player, entity, contents != null ? contents : List.of());
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!isHappyGhast(entity)) {
                return;
            }
            List<ItemStack> contents = entity.getAttached(CHEST);
            entity.removeAttached(HAS_CHEST);
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

    public static boolean isHappyGhast(net.minecraft.world.entity.Entity entity) {
        return EntityType.getKey(entity.getType()).getPath().equals("happy_ghast");
    }

    private static void open(Player player, net.minecraft.world.entity.Entity ghast, List<ItemStack> contents) {
        SimpleContainer container = new SimpleContainer(SLOTS) {
            @Override
            public void setChanged() {
                super.setChanged();
                List<ItemStack> saved = new ArrayList<>(SLOTS);
                for (int i = 0; i < SLOTS; i++) {
                    saved.add(getItem(i).copy());
                }
                ghast.setAttached(CHEST, saved);
            }
        };
        for (int i = 0; i < SLOTS && i < contents.size(); i++) {
            container.setItem(i, contents.get(i).copy());
        }
        player.openMenu(new SimpleMenuProvider(
                (id, inventory, p) -> ChestMenu.sixRows(id, inventory, container),
                Component.literal("Happy Ghast")));
    }
}
