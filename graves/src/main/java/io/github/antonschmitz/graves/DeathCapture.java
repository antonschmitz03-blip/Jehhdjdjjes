package io.github.antonschmitz.graves;

import io.github.antonschmitz.graves.compat.TravelersBackpackCompat;
import io.github.antonschmitz.graves.compat.TrinketsCompat;
import io.github.antonschmitz.graves.network.GraveWaypointPayload;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Collects everything a dying player drops. Instead of hooking each drop source (vanilla inventory, Trinkets,
 * Traveler's Backpack, ...), it intercepts item entities and XP orbs that get added to the world while the
 * player's death is being processed. Anything another mod keeps on the player (keepInventory, a soulbound
 * backpack, ...) is never dropped, so it never ends up in the grave either.
 */
public final class DeathCapture {
    private static @Nullable DeathCapture current;
    /** Set while the grave spills its contents, so those drops are not captured again. */
    private static boolean suspended;

    private final ServerPlayer player;
    private final ServerLevel level;
    private final BlockPos deathPos;
    /** Slot key to the stack that was in it when the player died (before anything was dropped). */
    private final Map<String, ItemStack> snapshot = new LinkedHashMap<>();
    private final List<ItemStack> items = new ArrayList<>();
    private int xp;

    private DeathCapture(ServerPlayer player) {
        this.player = player;
        this.level = player.level();
        this.deathPos = player.blockPosition();
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()) {
                snapshot.put("inv:" + i, stack.copy());
            }
        }
        if (TrinketsCompat.LOADED) {
            TrinketsCompat.snapshot(player, snapshot);
        }
        if (TravelersBackpackCompat.LOADED) {
            TravelersBackpackCompat.snapshot(player, snapshot);
        }
    }

    public static void begin(ServerPlayer player) {
        finishAny();
        current = new DeathCapture(player);
    }

    /** Returns true if the entity was taken into the grave and must not be added to the world. */
    public static boolean capture(ServerLevel level, Entity entity) {
        DeathCapture capture = current;
        if (capture == null || suspended || capture.level != level) {
            return false;
        }
        if (entity instanceof ItemEntity itemEntity) {
            ItemStack stack = itemEntity.getItem();
            if (!stack.isEmpty()) {
                capture.items.add(stack.copy());
            }
            return true;
        }
        if (entity instanceof ExperienceOrb orb) {
            capture.xp += orb.getValue();
            return true;
        }
        return false;
    }

    /** Returns true if the XP was taken into the grave. */
    public static boolean captureXp(ServerLevel level, int amount) {
        DeathCapture capture = current;
        if (capture == null || suspended || capture.level != level) {
            return false;
        }
        capture.xp += amount;
        return true;
    }

    public static void finish(ServerPlayer player) {
        if (current != null && current.player == player) {
            finishAny();
        }
    }

    public static void finishAny() {
        DeathCapture capture = current;
        if (capture == null) {
            return;
        }
        current = null;
        try {
            capture.placeGrave();
        } catch (RuntimeException e) {
            SimpleGraves.LOGGER.error("Failed to create grave for {}, dropping items instead", capture.player.getPlainTextName(), e);
            spill(capture.level, capture.deathPos, capture.toEntries(), capture.xp);
        }
    }

    private List<GraveEntry> toEntries() {
        Set<String> used = new HashSet<>();
        List<GraveEntry> entries = new ArrayList<>();
        for (ItemStack stack : items) {
            String slot = "";
            for (Map.Entry<String, ItemStack> entry : snapshot.entrySet()) {
                if (!used.contains(entry.getKey()) && ItemStack.matches(entry.getValue(), stack)) {
                    slot = entry.getKey();
                    used.add(slot);
                    break;
                }
            }
            entries.add(new GraveEntry(slot, stack));
        }
        return entries;
    }

    private void placeGrave() {
        if (items.isEmpty() && xp <= 0) {
            return;
        }
        List<GraveEntry> entries = toEntries();
        BlockPos pos = GravePlacement.find(level, deathPos);
        if (pos == null) {
            SimpleGraves.LOGGER.warn("No room for a grave for {} near {}, dropping items instead", player.getPlainTextName(), deathPos);
            spill(level, deathPos, entries, xp);
            return;
        }
        BlockState state = SimpleGraves.GRAVE.defaultBlockState().setValue(GraveBlock.FACING, player.getDirection().getOpposite());
        level.setBlockAndUpdate(pos, state);
        if (!(level.getBlockEntity(pos) instanceof GraveBlockEntity grave)) {
            throw new IllegalStateException("Grave block entity missing at " + pos);
        }
        grave.fill(player.getUUID(), player.getPlainTextName(), entries, xp);
        SimpleGraves.LOGGER.info("Grave for {} at {} in {} ({} stacks, {} xp)", player.getPlainTextName(), pos,
                level.dimension().identifier(), entries.size(), xp);

        player.sendSystemMessage(Component.translatable("simplegraves.message.placed",
                pos.getX(), pos.getY(), pos.getZ(), level.dimension().identifier().toString()).withStyle(ChatFormatting.GRAY));
        if (ServerPlayNetworking.canSend(player, GraveWaypointPayload.TYPE)) {
            ServerPlayNetworking.send(player, new GraveWaypointPayload(true, pos, level.dimension().identifier().toString()));
        }
    }

    /** Drops the items and XP as if there were no grave. */
    public static void spill(ServerLevel level, BlockPos pos, List<GraveEntry> entries, int xp) {
        suspended = true;
        try {
            for (GraveEntry entry : entries) {
                ItemEntity itemEntity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, entry.stack().copy());
                itemEntity.setDefaultPickUpDelay();
                level.addFreshEntity(itemEntity);
            }
            if (xp > 0) {
                ExperienceOrb.award(level, net.minecraft.world.phys.Vec3.atCenterOf(pos), xp);
            }
        } finally {
            suspended = false;
        }
    }
}
