package io.github.antonschmitz.graves.client;

import io.github.antonschmitz.graves.SimpleGraves;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.minimap.waypoint.WaypointColor;
import xaero.hud.minimap.waypoint.WaypointPurpose;
import xaero.hud.minimap.waypoint.set.WaypointSet;
import xaero.hud.minimap.world.MinimapWorld;

/**
 * Grave waypoints in Xaero's Minimap (and World Map, which shares them). Xaero has no public waypoint API, so
 * this uses the same internals as its own deathpoints; any failure is logged and the grave works regardless.
 * Only loaded when Xaero's Minimap is installed.
 */
public final class XaeroWaypoints {
    private static final String SYMBOL = "G";
    /** Set when a grave waypoint was added for the current death, so Xaero's own deathpoint is skipped. */
    private static boolean graveDeath;

    private XaeroWaypoints() {
    }

    public static boolean consumeGraveDeath() {
        boolean result = graveDeath;
        graveDeath = false;
        return result;
    }

    static void addGrave(BlockPos pos, String dimension) {
        try {
            MinimapWorld world = currentWorld(dimension);
            if (world == null || world.getCurrentWaypointSet() == null) {
                return;
            }
            Waypoint waypoint = new Waypoint(pos.getX(), pos.getY(), pos.getZ(), I18n.get("simplegraves.waypoint"), SYMBOL,
                    WaypointColor.DARK_GRAY, WaypointPurpose.NORMAL);
            world.getCurrentWaypointSet().add(waypoint, true);
            save(world);
            graveDeath = true;
        } catch (RuntimeException | LinkageError e) {
            SimpleGraves.LOGGER.warn("Could not add a Xaero waypoint for the grave at {}", pos, e);
        }
    }

    static void removeGrave(BlockPos pos, String dimension) {
        try {
            MinimapWorld world = currentWorld(dimension);
            if (world == null) {
                return;
            }
            boolean removed = false;
            for (WaypointSet set : world.getIterableWaypointSets()) {
                List<Waypoint> matches = new ArrayList<>();
                for (Waypoint waypoint : set.getWaypoints()) {
                    if (SYMBOL.equals(waypoint.getSymbol()) && waypoint.getPurpose() == WaypointPurpose.NORMAL
                            && waypoint.getX() == pos.getX() && waypoint.getY() == pos.getY() && waypoint.getZ() == pos.getZ()) {
                        matches.add(waypoint);
                    }
                }
                if (!matches.isEmpty()) {
                    set.removeAll(matches);
                    removed = true;
                }
            }
            if (removed) {
                save(world);
            }
        } catch (RuntimeException | LinkageError e) {
            SimpleGraves.LOGGER.warn("Could not remove the Xaero waypoint for the grave at {}", pos, e);
        }
    }

    /** The waypoint world Xaero uses for the dimension the player is in, if that is the grave's dimension. */
    private static MinimapWorld currentWorld(String dimension) {
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
            SimpleGraves.LOGGER.warn("Could not save Xaero waypoints", e);
        }
    }
}
