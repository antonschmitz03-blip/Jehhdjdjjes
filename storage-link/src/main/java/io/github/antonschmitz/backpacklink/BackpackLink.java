package io.github.antonschmitz.backpacklink;

import com.mojang.serialization.Codec;
import io.github.antonschmitz.backpacklink.network.BackpackViewPayload;
import io.github.antonschmitz.backpacklink.network.ToggleBackpackPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BackpackLink implements ModInitializer {
    public static final String MOD_ID = "backpacklink";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /** Per-player setting of the terminal toggle button. Saved with the player and kept on death. */
    public static final AttachmentType<Boolean> ENABLED = AttachmentRegistry.create(id("enabled"),
            builder -> builder.persistent(Codec.BOOL).initializer(() -> Boolean.TRUE).copyOnDeath());

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.clientboundPlay().register(BackpackViewPayload.TYPE, BackpackViewPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ToggleBackpackPayload.TYPE, ToggleBackpackPayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ToggleBackpackPayload.TYPE,
                (payload, context) -> context.player().setAttached(ENABLED, payload.enabled()));

        if (Boolean.getBoolean("backpacklink.selfTest")) {
            ServerLifecycleEvents.SERVER_STARTED.register(SelfTest::run);
        }
    }

    public static boolean isEnabled(Player player) {
        return player.getAttachedOrElse(ENABLED, Boolean.TRUE);
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
