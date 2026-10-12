package io.github.antonschmitz.waystones.client;

import io.github.antonschmitz.waystones.network.WaystoneWaypointsPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;

public class SimpleWaystonesClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        if (!FabricLoader.getInstance().isModLoaded("xaerominimap")) {
            return;
        }
        ClientPlayNetworking.registerGlobalReceiver(WaystoneWaypointsPayload.TYPE, (payload, context) -> XaeroWaypoints.queue(payload));
        // Xaero's waypoint session may not exist yet right after joining, so updates wait until it does.
        ClientTickEvents.END_CLIENT_TICK.register(client -> XaeroWaypoints.applyPending());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> XaeroWaypoints.clear());
    }
}
