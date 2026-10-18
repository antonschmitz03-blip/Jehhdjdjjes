package io.github.antonschmitz.waystones;

import io.github.antonschmitz.waystones.network.WaystoneWaypointsPayload;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Keeps the client's map waypoints (Xaero's Minimap / World Map) in line with the waystones a player has
 * discovered. Only clients with the mod installed get anything; the client ignores it without Xaero's.
 */
final class WaypointSync {
    /** Dimension each online player was last synced for, to resync after joining, respawning or a portal. */
    private static final Map<UUID, String> lastDimension = new HashMap<>();

    private WaypointSync() {
    }

    static void tick(MinecraftServer server) {
        Map<UUID, String> seen = new HashMap<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            String dimension = WaystoneData.dimensionId(player.level());
            seen.put(player.getUUID(), dimension);
            if (!dimension.equals(lastDimension.get(player.getUUID()))) {
                send(player);
            }
        }
        lastDimension.clear();
        lastDimension.putAll(seen);
    }

    static void clear() {
        lastDimension.clear();
    }

    /** Resends the list to every online player who knows any of these waystones. */
    static void sendToDiscoverers(MinecraftServer server, List<UUID> players) {
        for (UUID uuid : players) {
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player != null) {
                send(player);
            }
        }
    }

    static void send(ServerPlayer player) {
        if (!ServerPlayNetworking.canSend(player, WaystoneWaypointsPayload.TYPE)) {
            return;
        }
        String dimension = WaystoneData.dimensionId(player.level());
        List<WaystoneWaypointsPayload.Entry> entries = new ArrayList<>();
        for (WaystoneData.Waystone waystone : WaystoneData.get().discovered(player.getUUID())) {
            if (waystone.dimension().equals(dimension)) {
                entries.add(new WaystoneWaypointsPayload.Entry(waystone.pos(), waystone.name != null ? waystone.name : "", waystone.biome));
            }
        }
        ServerPlayNetworking.send(player, new WaystoneWaypointsPayload(dimension, entries));
    }
}
