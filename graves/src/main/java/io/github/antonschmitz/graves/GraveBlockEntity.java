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
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
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
    /** While the owner has the chest screen open, the shown items live here instead of in {@link #entries}. */
    private @Nullable SimpleContainer openContainer;
    /** Original slot of each item in {@link #openContainer}, by container index. */
    private List<String> openSlots = List.of();

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
        return List.copyOf(allEntries());
    }

    /** Stored items including the ones currently shown in an open chest screen, so a save never loses them. */
    private List<GraveEntry> allEntries() {
        if (openContainer == null) {
            return entries;
        }
        List<GraveEntry> all = new ArrayList<>();
        for (int i = 0; i < openContainer.getContainerSize(); i++) {
            ItemStack stack = openContainer.getItem(i);
            if (!stack.isEmpty()) {
                all.add(new GraveEntry(i < openSlots.size() ? openSlots.get(i) : "", stack.copy()));
            }
        }
        all.addAll(entries);
        return all;
    }

    public int xp() {
        return xp;
    }

    /**
     * Gives everything back: items into the slot they came from when it is empty, otherwise into the
     * inventory, otherwise dropped at the player's feet. Then removes the grave.
     */
    public void restoreTo(ServerPlayer player) {
        if (openContainer != null) {
            return;
        }
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
            DeathCapture.giveXp(player, xp);
        }
        entries.clear();
        xp = 0;
        setChanged();
        player.sendSystemMessage(Component.translatable("simplegraves.message.restored").withStyle(ChatFormatting.GRAY), true);
        // Contents are already handed out, so removing the block spills nothing.
        player.level().removeBlock(getBlockPos(), false);
    }

    /**
     * Opens a chest screen with the grave's items so the owner can take what they want. The XP is handed out
     * right away. When the screen is closed and the grave is empty, the grave goes away.
     */
    public void openMenu(ServerPlayer player) {
        if (openContainer != null) {
            return;
        }
        // The grave is only emptied into the menu once the player's screen actually opens.
        player.openMenu(new SimpleMenuProvider((containerId, inventory, p) -> fillMenu(containerId, inventory, player),
                Component.translatable("simplegraves.container.grave", ownerName)));
    }

    /** Moves up to 54 stacks into a chest container and gives the XP. Package-private for {@link SelfTest}. */
    GraveMenu fillMenu(int containerId, net.minecraft.world.entity.player.Inventory inventory, ServerPlayer player) {
        int size = 54;
        SimpleContainer container = new SimpleContainer(size);
        List<String> slots = new ArrayList<>();
        int shown = Math.min(size, entries.size());
        for (int i = 0; i < shown; i++) {
            container.setItem(i, entries.get(i).stack().copy());
            slots.add(entries.get(i).slot());
        }
        entries = new ArrayList<>(entries.subList(shown, entries.size()));
        openContainer = container;
        openSlots = slots;
        if (xp > 0) {
            DeathCapture.giveXp(player, xp);
            xp = 0;
        }
        setChanged();
        return new GraveMenu(containerId, inventory, container, this);
    }

    void closeMenu(net.minecraft.world.Container container) {
        if (container != openContainer) {
            return;
        }
        openContainer = null;
        List<GraveEntry> remaining = new ArrayList<>();
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty()) {
                remaining.add(new GraveEntry(i < openSlots.size() ? openSlots.get(i) : "", stack.copy()));
            }
        }
        container.clearContent();
        openSlots = List.of();
        remaining.addAll(entries);
        entries = remaining;
        setChanged();
        if (entries.isEmpty() && xp <= 0 && level != null && !isRemoved()) {
            level.removeBlock(getBlockPos(), false);
        }
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
            if (openContainer != null) {
                // Removed while the chest screen is open: spill what is shown too, and close the screen empty.
                SimpleContainer container = openContainer;
                openContainer = null;
                for (int i = 0; i < container.getContainerSize(); i++) {
                    if (!container.getItem(i).isEmpty()) {
                        entries.add(new GraveEntry("", container.getItem(i).copy()));
                    }
                }
                container.clearContent();
            }
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
        output.store("items", GraveEntry.CODEC.listOf(), allEntries());
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
