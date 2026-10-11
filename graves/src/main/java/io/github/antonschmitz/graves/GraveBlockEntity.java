package io.github.antonschmitz.graves;

import io.github.antonschmitz.graves.compat.TravelersBackpackCompat;
import io.github.antonschmitz.graves.compat.TrinketsCompat;
import io.github.antonschmitz.graves.network.GraveWaypointPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class GraveBlockEntity extends BlockEntity {
    private @Nullable UUID owner;
    private String ownerName = "?";
    private List<GraveEntry> entries = new ArrayList<>();
    private int xp;

    public GraveBlockEntity(BlockPos pos, BlockState state) {
        super(SimpleGraves.GRAVE_BLOCK_ENTITY, pos, state);
    }

    void fill(UUID owner, String ownerName, List<GraveEntry> entries, int xp) {
        this.owner = owner;
        this.ownerName = ownerName;
        this.entries = new ArrayList<>(entries);
        this.xp = xp;
        setChanged();
    }

    public boolean isOwner(ServerPlayer player) {
        return player.getUUID().equals(owner);
    }

    public String ownerName() {
        return ownerName;
    }

    public List<GraveEntry> entries() {
        return List.copyOf(entries);
    }

    public int xp() {
        return xp;
    }

    /**
     * Gives everything back: items into the slot they came from when it is empty, otherwise into the
     * inventory, otherwise dropped at the player's feet. Then removes the grave.
     */
    public void restoreTo(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        List<ItemStack> leftovers = new ArrayList<>();
        for (GraveEntry entry : entries) {
            ItemStack stack = entry.stack().copy();
            if (!restoreToSlot(player, inventory, entry.slot(), stack)) {
                leftovers.add(stack);
            }
        }
        for (ItemStack stack : leftovers) {
            inventory.add(stack);
            if (!stack.isEmpty()) {
                ItemEntity itemEntity = new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), stack);
                itemEntity.setNoPickUpDelay();
                player.level().addFreshEntity(itemEntity);
            }
        }
        if (xp > 0) {
            player.giveExperiencePoints(xp);
        }
        entries.clear();
        xp = 0;
        setChanged();
        player.sendSystemMessage(Component.translatable("simplegraves.message.restored").withStyle(ChatFormatting.GRAY), true);
        // Contents are already handed out, so removing the block spills nothing.
        player.level().removeBlock(getBlockPos(), false);
    }

    private static boolean restoreToSlot(ServerPlayer player, Inventory inventory, String slot, ItemStack stack) {
        if (slot.startsWith("inv:")) {
            int index;
            try {
                index = Integer.parseInt(slot.substring(4));
            } catch (NumberFormatException e) {
                return false;
            }
            if (index >= 0 && index < inventory.getContainerSize() && inventory.getItem(index).isEmpty()) {
                inventory.setItem(index, stack);
                return true;
            }
            return false;
        }
        if (slot.startsWith(TrinketsCompat.SLOT_PREFIX)) {
            return TrinketsCompat.LOADED && TrinketsCompat.restore(player, slot.substring(TrinketsCompat.SLOT_PREFIX.length()), stack);
        }
        if (slot.equals(TravelersBackpackCompat.SLOT)) {
            return TravelersBackpackCompat.LOADED && TravelersBackpackCompat.restore(player, stack);
        }
        return false;
    }

    /** Called when the block goes away by any other means (creative, commands): spill instead of losing it. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level instanceof ServerLevel serverLevel) {
            if (!entries.isEmpty() || xp > 0) {
                DeathCapture.spill(serverLevel, pos, entries, xp);
                entries.clear();
                xp = 0;
            }
            if (owner != null && serverLevel.getServer().getPlayerList().getPlayer(owner) instanceof ServerPlayer player
                    && ServerPlayNetworking.canSend(player, GraveWaypointPayload.TYPE)) {
                ServerPlayNetworking.send(player, new GraveWaypointPayload(false, pos, serverLevel.dimension().identifier().toString()));
            }
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (owner != null) {
            output.store("owner", UUIDUtil.CODEC, owner);
        }
        output.putString("owner_name", ownerName);
        output.store("items", GraveEntry.CODEC.listOf(), entries);
        output.putInt("xp", xp);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
        ownerName = input.getStringOr("owner_name", "?");
        entries = new ArrayList<>(input.read("items", GraveEntry.CODEC.listOf()).orElse(List.of()));
        xp = input.getIntOr("xp", 0);
    }
}
