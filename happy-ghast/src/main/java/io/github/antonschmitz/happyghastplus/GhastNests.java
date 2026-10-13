package io.github.antonschmitz.happyghastplus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * A harnessed happy ghast that hovers slowly just above a ghast nest settles into it, and falls asleep (eyes closed,
 * smile kept) a few seconds later. While asleep it slowly heals. Mounting it, hurting it or calling it with the
 * whistle wakes it up.
 */
public final class GhastNests {
    /** Synced to clients so they can close the ghast's eyes. Not saved: a ghast re-settles after a reload. */
    public static final AttachmentType<Boolean> SLEEPING = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(HappyGhastPlus.MOD_ID, "ghast_sleeping"),
            builder -> builder.syncWith(ByteBufCodecs.BOOL, AttachmentSyncPredicate.all()));

    private static final int SEARCH_INTERVAL = 10;
    /** How far above the nest and how far from its centre (sideways) a ghast may be to settle. */
    private static final int SEARCH_HEIGHT = 6;
    private static final double SEARCH_RADIUS = 3.0;
    private static final double MAX_SPEED = 0.12;
    private static final double SINK_SPEED = 0.08;
    private static final double CENTRE_EASING = 0.15;
    private static final int SLEEP_AFTER = 60;
    private static final int HEAL_INTERVAL = 80;
    private static final int WAKE_COOLDOWN = 100;

    private static final Set<HappyGhast> GHASTS = new HashSet<>();
    private static final Map<UUID, Rest> RESTING = new HashMap<>();
    private static final Map<UUID, Integer> COOLDOWN = new HashMap<>();
    private static int ticks;

    /** {@code origin} is cell (0, 0) of the nest; {@code restTicks} counts ticks spent at rest on its floor. */
    private record Rest(Level level, BlockPos origin, int restTicks) {}

    private GhastNests() {}

    public static void register() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (entity instanceof HappyGhast ghast) {
                GHASTS.add(ghast);
            }
        });
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
            if (entity instanceof HappyGhast ghast) {
                GHASTS.remove(ghast);
                RESTING.remove(ghast.getUUID());
            }
        });
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damage, blocked) -> {
            if (entity instanceof HappyGhast ghast) {
                wake(ghast);
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(GhastNests::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            GHASTS.clear();
            RESTING.clear();
            COOLDOWN.clear();
        });
    }

    /** Ends any rest or sleep; the ghast won't settle again for a few seconds. */
    public static void wake(HappyGhast ghast) {
        if (RESTING.remove(ghast.getUUID()) != null) {
            COOLDOWN.put(ghast.getUUID(), WAKE_COOLDOWN);
        }
        if (ghast.getAttachedOrElse(SLEEPING, false)) {
            ghast.removeAttached(SLEEPING);
        }
    }

    private static void tick(MinecraftServer server) {
        ticks++;
        COOLDOWN.replaceAll((id, left) -> left - 1);
        COOLDOWN.values().removeIf(left -> left <= 0);

        for (HappyGhast ghast : new ArrayList<>(GHASTS)) {
            if (ghast.isRemoved()) {
                GHASTS.remove(ghast);
                RESTING.remove(ghast.getUUID());
                continue;
            }
            Rest rest = RESTING.get(ghast.getUUID());
            if (rest != null) {
                tickResting(ghast, rest);
            } else if ((ticks + ghast.getId()) % SEARCH_INTERVAL == 0 && canSettle(ghast)) {
                BlockPos origin = findNest(ghast);
                if (origin != null) {
                    RESTING.put(ghast.getUUID(), new Rest(ghast.level(), origin, 0));
                }
            }
        }
    }

    private static boolean canSettle(HappyGhast ghast) {
        return ghast.isAlive() && !ghast.isBaby() && !ghast.isVehicle()
                && !ghast.getItemBySlot(EquipmentSlot.BODY).isEmpty()
                && !COOLDOWN.containsKey(ghast.getUUID())
                && !GhastFeatures.isRecalled(ghast) && !RopeLadders.isDeployed(ghast)
                && ghast.getDeltaMovement().length() < MAX_SPEED;
    }

    /** Finds a free nest below the ghast whose centre is close enough sideways; returns its origin (cell 0, 0). */
    private static BlockPos findNest(HappyGhast ghast) {
        Level level = ghast.level();
        BlockPos feet = ghast.blockPosition();
        int reach = (int) Math.ceil(SEARCH_RADIUS) + GhastNestBlock.SIZE / 2;
        for (int dy = 0; dy <= SEARCH_HEIGHT; dy++) {
            for (int dx = -reach; dx <= reach; dx++) {
                for (int dz = -reach; dz <= reach; dz++) {
                    BlockPos pos = feet.offset(dx, -dy, dz);
                    BlockState state = level.getBlockState(pos);
                    if (!state.is(HappyGhastPlus.GHAST_NEST)) {
                        continue;
                    }
                    BlockPos origin = GhastNestBlock.origin(pos, state);
                    Vec3 centre = centre(origin);
                    double sx = ghast.getX() - centre.x;
                    double sz = ghast.getZ() - centre.z;
                    if (sx * sx + sz * sz <= SEARCH_RADIUS * SEARCH_RADIUS && ghast.getY() >= centre.y && !occupied(level, origin)) {
                        return origin;
                    }
                }
            }
        }
        return null;
    }

    private static boolean occupied(Level level, BlockPos origin) {
        for (Rest rest : RESTING.values()) {
            if (rest.level() == level && rest.origin().equals(origin)) {
                return true;
            }
        }
        return false;
    }

    /** Where a settled ghast's feet go: the middle of the 6x6 square, on top of the collision floor. */
    private static Vec3 centre(BlockPos origin) {
        return new Vec3(origin.getX() + GhastNestBlock.SIZE / 2.0, origin.getY() + GhastNestBlock.FLOOR, origin.getZ() + GhastNestBlock.SIZE / 2.0);
    }

    private static boolean nestIntact(Level level, BlockPos origin) {
        BlockState middle = level.getBlockState(origin.offset(3, 0, 3));
        return middle.is(HappyGhastPlus.GHAST_NEST) && GhastNestBlock.origin(origin.offset(3, 0, 3), middle).equals(origin);
    }

    private static void tickResting(HappyGhast ghast, Rest rest) {
        if (!ghast.isAlive() || ghast.isVehicle() || ghast.level() != rest.level()
                || ghast.getItemBySlot(EquipmentSlot.BODY).isEmpty()
                || GhastFeatures.isRecalled(ghast) || RopeLadders.isDeployed(ghast)
                || !nestIntact(rest.level(), rest.origin())) {
            wake(ghast);
            return;
        }
        Vec3 target = centre(rest.origin());
        double x = ghast.getX() + (target.x - ghast.getX()) * CENTRE_EASING;
        double z = ghast.getZ() + (target.z - ghast.getZ()) * CENTRE_EASING;
        double y = Math.max(target.y, ghast.getY() - SINK_SPEED);
        boolean atRest = Math.abs(target.x - x) < 0.02 && Math.abs(target.z - z) < 0.02 && y - target.y < 0.01;
        if (atRest) {
            x = target.x;
            y = target.y;
            z = target.z;
        }
        ghast.getNavigation().stop();
        ghast.setDeltaMovement(Vec3.ZERO);
        ghast.setPos(x, y, z);

        int restTicks = atRest ? rest.restTicks() + 1 : 0;
        RESTING.put(ghast.getUUID(), new Rest(rest.level(), rest.origin(), restTicks));
        if (restTicks == SLEEP_AFTER) {
            ghast.setAttached(SLEEPING, true);
        }
        if (restTicks > SLEEP_AFTER && restTicks % HEAL_INTERVAL == 0 && ghast.getHealth() < ghast.getMaxHealth()) {
            ghast.heal(1.0F);
        }
    }
}
