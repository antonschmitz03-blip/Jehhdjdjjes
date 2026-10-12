package io.github.antonschmitz.waystones.client;

import io.github.antonschmitz.waystones.SimpleWaystones;
import io.github.antonschmitz.waystones.network.WaystoneWaypointsPayload;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import org.jspecify.annotations.Nullable;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.minimap.waypoint.WaypointColor;
import xaero.hud.minimap.waypoint.WaypointPurpose;
import xaero.hud.minimap.waypoint.set.WaypointSet;
import xaero.hud.minimap.world.MinimapWorld;

/**
 * Waystone waypoints in Xaero's Minimap (World Map shows the same ones). One purple "WS" waypoint per discovered
 * waystone, named like the waystone. Xaero has no public waypoint API, so this uses the same internals as
 * Simple Graves; any failure is logged and the waystones work regardless. Only loaded with Xaero's Minimap.
 */
public final class XaeroWaypoints {
    /** Two letters, so a waypoint the player made themselves (one-letter default symbol) is never touched. */
    private static final String SYMBOL = "WS";
    private static @Nullable WaystoneWaypointsPayload pending;

    private XaeroWaypoints() {
    }

    static void queue(WaystoneWaypointsPayload payload) {
        pending = payload;
        applyPending();
    }

    static void clear() {
        pending = null;
    }

    static void applyPending() {
        WaystoneWaypointsPayload payload = pending;
        if (payload == null) {
            return;
        }
        try {
            MinimapWorld world = currentWorld(payload.dimension());
            if (world == null || world.getCurrentWaypointSet() == null) {
                return;
            }
            pending = null;
            if (sync(world, payload)) {
                save(world);
            }
        } catch (RuntimeException | LinkageError e) {
            pending = null;
            SimpleWaystones.LOGGER.warn("Could not update the Xaero waypoints for waystones", e);
        }
    }

    /** Adds, renames and removes waystone waypoints so they match the payload. Returns whether anything changed. */
    private static boolean sync(MinimapWorld world, WaystoneWaypointsPayload payload) {
        Map<BlockPos, String> wanted = new HashMap<>();
        for (WaystoneWaypointsPayload.Entry entry : payload.waystones()) {
            String name = entry.name().isBlank()
                    ? I18n.get("simplewaystones.default_name", I18n.get(entry.biome()))
                    : entry.name();
            wanted.put(entry.pos(), name);
        }
        boolean changed = false;
        for (WaypointSet set : world.getIterableWaypointSets()) {
            List<Waypoint> stale = new ArrayList<>();
            for (Waypoint waypoint : set.getWaypoints()) {
                if (!SYMBOL.equals(waypoint.getSymbol()) || waypoint.getPurpose() != WaypointPurpose.NORMAL) {
                    continue;
                }
                BlockPos pos = new BlockPos(waypoint.getX(), waypoint.getY(), waypoint.getZ());
                String name = wanted.remove(pos);
                if (name == null) {
                    stale.add(waypoint);
                } else if (!name.equals(waypoint.getName())) {
                    waypoint.setName(name);
                    changed = true;
                }
            }
            if (!stale.isEmpty()) {
                set.removeAll(stale);
                changed = true;
            }
        }
        for (Map.Entry<BlockPos, String> entry : wanted.entrySet()) {
            BlockPos pos = entry.getKey();
            world.getCurrentWaypointSet().add(new Waypoint(pos.getX(), pos.getY(), pos.getZ(), entry.getValue(), SYMBOL,
                    WaypointColor.PURPLE, WaypointPurpose.NORMAL), true);
            changed = true;
        }
        return changed;
    }

    /** The waypoint world Xaero uses for the dimension the player is in, if that is the payload's dimension. */
    private static @Nullable MinimapWorld currentWorld(String dimension) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !minecraft.level.dimension().identifier().toString().equals(dimension)) {
            return null;
        }
        MinimapSession session = BuiltInHudModules.MINIMAP.getCurrentSession();
        if (session == null) {
            return null;
        }
        MinimapWorld world = session.getWorldManager().getAutoWorld();
        return world != null ? world : session.getWorldManager().getCurrentWorld();
    }

    private static void save(MinimapWorld world) {
        MinimapSession session = BuiltInHudModules.MINIMAP.getCurrentSession();
        try {
            session.getWorldManagerIO().saveWorld(world);
        } catch (IOException e) {
            SimpleWaystones.LOGGER.warn("Could not save Xaero waypoints", e);
        }
    }
}
