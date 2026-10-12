package io.github.antonschmitz.graves.compat;

import com.tiviacz.travelersbackpack.TravelersBackpack;
import com.tiviacz.travelersbackpack.attachment.AttachmentUtils;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Traveler's Backpack's own worn slot, used when there is no Trinkets/Accessories integration (with it, the
 * backpack sits in a Trinkets slot and {@link TrinketsCompat} handles it). Only touch the methods when
 * {@link #LOADED} is true.
 */
public final class TravelersBackpackCompat {
    public static final boolean LOADED = FabricLoader.getInstance().isModLoaded("travelersbackpack");
    public static final String SLOT = "backpack";

    private TravelersBackpackCompat() {
    }

    public static void snapshot(ServerPlayer player, Map<String, ItemStack> snapshot) {
        if (!TravelersBackpack.enableIntegration() && AttachmentUtils.isWearingBackpack(player)) {
            snapshot.put(SLOT, AttachmentUtils.getWearingBackpack(player).copy());
        }
    }

    public static boolean restore(ServerPlayer player, ItemStack stack) {
        if (TravelersBackpack.enableIntegration() || AttachmentUtils.isWearingBackpack(player)) {
            return false;
        }
        AttachmentUtils.equipBackpack(player, stack);
        return true;
    }
}
