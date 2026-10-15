package io.github.antonschmitz.happyghastplus.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.antonschmitz.happyghastplus.GhastSearchlight;
import io.github.antonschmitz.happyghastplus.SearchlightTogglePayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;

/** The key (G by default; L is vanilla's advancements key) that switches the searchlight of the ghast you steer. */
public final class SearchlightKey {
    private static KeyMapping key;

    private SearchlightKey() {}

    public static void register() {
        key = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.happyghastplus.searchlight", InputConstants.KEY_G, KeyMapping.Category.GAMEPLAY));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (key.consumeClick()) {
                if (client.player != null && client.player.getVehicle() instanceof HappyGhast ghast
                        && ghast.getControllingPassenger() == client.player && GhastSearchlight.isMounted(ghast)) {
                    ClientPlayNetworking.send(SearchlightTogglePayload.INSTANCE);
                }
            }
        });
    }
}
