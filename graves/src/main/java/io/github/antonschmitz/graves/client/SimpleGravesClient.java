package io.github.antonschmitz.graves.client;

import io.github.antonschmitz.graves.network.GraveWaypointPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;

public class SimpleGravesClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        if (!FabricLoader.getInstance().isModLoaded("xaerominimap")) {
            return;
        }
        ClientPlayNetworking.registerGlobalReceiver(GraveWaypointPayload.TYPE, (payload, context) -> {
            if (payload.added()) {
                XaeroWaypoints.addGrave(payload.pos(), payload.dimension());
            } else {
                XaeroWaypoints.removeGrave(payload.pos(), payload.dimension());
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> XaeroWaypoints.consumeGraveDeath());
    }
}
