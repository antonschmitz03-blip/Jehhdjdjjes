package io.github.antonschmitz.vplustweaks.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.antonschmitz.vplustweaks.TweaksConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;

/** An "Open elytra" key (unbound by default). Once it is bound, jumping in the air no longer opens the elytra. */
public final class ElytraKey implements ClientModInitializer {
    public static final KeyMapping OPEN_ELYTRA = new KeyMapping(
            "key.vplustweaks.open_elytra", InputConstants.UNKNOWN.getValue(), KeyMapping.Category.MOVEMENT);

    /** True when the key is in use, so the jump key should leave the elytra alone. */
    public static boolean replacesJump() {
        return TweaksConfig.enabled(TweaksConfig.OPEN_ELYTRA_KEY)
                && !OPEN_ELYTRA.isUnbound()
                && !TweaksConfig.enabled(TweaksConfig.ELYTRA_JUMP_ALSO_OPENS);
    }

    @Override
    public void onInitializeClient() {
        if (!TweaksConfig.enabled(TweaksConfig.OPEN_ELYTRA_KEY)) {
            return;
        }
        KeyMappingHelper.registerKeyMapping(OPEN_ELYTRA);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN_ELYTRA.consumeClick()) {
                LocalPlayer player = client.player;
                if (player != null && player.tryToStartFallFlying()) {
                    player.connection.send(new ServerboundPlayerCommandPacket(
                            player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
                }
            }
        });
    }
}
