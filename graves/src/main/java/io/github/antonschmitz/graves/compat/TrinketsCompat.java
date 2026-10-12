package io.github.antonschmitz.graves.compat;

import eu.pb4.trinkets.api.TrinketAttachment;
import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.TrinketsApi;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Trinkets slots. Only touch the methods when {@link #LOADED} is true. */
public final class TrinketsCompat {
    public static final boolean LOADED = FabricLoader.getInstance().isModLoaded("trinkets");
    public static final String SLOT_PREFIX = "trinket:";

    private TrinketsCompat() {
    }

    public static void snapshot(ServerPlayer player, Map<String, ItemStack> snapshot) {
        TrinketsApi.getAttachment(player).forEach((TrinketSlotAccess slot, ItemStack stack) -> {
            if (!stack.isEmpty()) {
                snapshot.put(SLOT_PREFIX + slot.reference().getSerializedName(), stack.copy());
            }
        });
    }

    /** Puts the stack back into the given slot ({@code <slot>@<index>[?cosmetic]}) if that slot is empty. */
    public static boolean restore(ServerPlayer player, String reference, ItemStack stack) {
        int at = reference.lastIndexOf('@');
        if (at < 0) {
            return false;
        }
        boolean cosmetic = reference.endsWith("?cosmetic");
        String slotId = reference.substring(0, at);
        int index;
        try {
            index = Integer.parseInt(reference.substring(at + 1, cosmetic ? reference.length() - "?cosmetic".length() : reference.length()));
        } catch (NumberFormatException e) {
            return false;
        }
        TrinketAttachment attachment = TrinketsApi.getAttachment(player);
        TrinketSlotAccess slot = attachment.getSlotAccess(slotId, index, cosmetic);
        if (slot == null || !slot.isValid() || !slot.get().isEmpty()) {
            return false;
        }
        return slot.set(stack);
    }
}
