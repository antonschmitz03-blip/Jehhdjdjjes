package io.github.antonschmitz.happyghastplus;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A happy ghast can let down a rope ladder. The ladder is not made of blocks: it is drawn by the ghast's render layer
 * on its right-hand side, turns with the ghast, and reaches from the ghast's back down to the ground. Climbing it is
 * handled by {@link #isOnRope}, which the {@code onClimbable} mixin asks. It comes down when the whistle calls a nearby
 * ghast or a rider dismounts, and is pulled back up when someone rides the ghast, the ghast moves, or nobody has used
 * it for a minute. The ghast holds still while it is down, position and facing.
 */
public final class RopeLadders {
    public static final int MAX_LENGTH = 40;
    /** Distance of the rope from the ghast's centre, to its right, in blocks. */
    public static final double ROPE_SIDE = 2.45;
    /** Height of the rope's top above the ghast's feet; the ghast's back is just below this. */
    public static final double ROPE_TOP = 4.0;
    private static final double GRAB_HALF_WIDTH = 0.4;
    private static final double GRAB_HALF_DEPTH = 0.7;
    private static final double GRAB_TOP = 4.7;
    private static final int IDLE_TICKS = 20 * 60;

    /** How far the rope reaches below the ghast's feet (0 when it is pulled up). Synced so clients can draw it. */
    public static final AttachmentType<Float> ROPE = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(HappyGhastPlus.MOD_ID, "rope_length"),
            builder -> builder.syncWith(ByteBufCodecs.FLOAT, AttachmentSyncPredicate.all()));

    private static final Map<UUID, Deployed> DEPLOYED = new HashMap<>();
    private static final Map<UUID, UUID> LAST_VEHICLE = new HashMap<>();

    /** Where the ghast stood and which way it faced when the rope came down; it is held there, turning included. */
    private record Deployed(ServerLevel level, Vec3 anchor, float yaw, float pitch, int idleTicks) {}

    private RopeLadders() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(RopeLadders::tick);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> DEPLOYED.clear());
    }

    /** World offset of a point to the ghast's right ({@code right}) and in front of it ({@code forward}). */
    public static Vec3 offset(float bodyYaw, double right, double forward) {
        double yaw = Math.toRadians(bodyYaw);
        double sin = Math.sin(yaw);
        double cos = Math.cos(yaw);
        return new Vec3(-right * cos - forward * sin, 0, -right * sin + forward * cos);
    }

    /** Lets the rope ladder down on the ghast's right-hand side. */
    public static void lower(HappyGhast ghast) {
        if (ghast.isBaby() || ghast.isVehicle() || !(ghast.level() instanceof ServerLevel level)) {
            return;
        }
        retract(ghast.getUUID());

        Vec3 at = ghast.position().add(offset(ghast.yBodyRot, ROPE_SIDE, 0));
        int y = Mth.floor(ghast.getY());
        int x = Mth.floor(at.x);
        int z = Mth.floor(at.z);
        double ground = ghast.getY() - MAX_LENGTH;
        for (int by = y; by > y - MAX_LENGTH && by >= level.getMinY(); by--) {
            BlockPos pos = new BlockPos(x, by, z);
            BlockState state = level.getBlockState(pos);
            if (!state.getCollisionShape(level, pos).isEmpty()) {
                ground = by + 1;
                break;
            }
        }
        float length = (float) Mth.clamp(ghast.getY() - ground, 1.0, MAX_LENGTH);
        ghast.setAttached(ROPE, length);
        level.playSound(null, at.x, ghast.getY() + 2.0, at.z, SoundEvents.LADDER_PLACE, SoundSource.NEUTRAL, 1.0F, 0.8F);
        DEPLOYED.put(ghast.getUUID(), new Deployed(level, ghast.position(), ghast.getYRot(), ghast.getXRot(), 0));
    }

    public static boolean isDeployed(HappyGhast ghast) {
        return DEPLOYED.containsKey(ghast.getUUID());
    }

    public static void retract(UUID ghast) {
        Deployed deployed = DEPLOYED.remove(ghast);
        if (deployed != null) {
            pullUp(deployed, ghast);
        }
    }

    private static void pullUp(Deployed deployed, UUID id) {
        if (deployed.level().getEntity(id) instanceof HappyGhast ghast) {
            ghast.setAttached(ROPE, 0.0F);
            deployed.level().playSound(null, ghast.getX(), ghast.getY() + 2.0, ghast.getZ(), SoundEvents.LADDER_BREAK, SoundSource.NEUTRAL, 1.0F, 0.8F);
        }
    }

    /** Whether the entity is hanging on a happy ghast's rope ladder (checked on the server and on the client). */
    public static boolean isOnRope(Entity entity) {
        Level level = entity.level();
        AABB search = new AABB(entity.getX() - 3.5, entity.getY() - 6.0, entity.getZ() - 3.5,
                entity.getX() + 3.5, entity.getY() + MAX_LENGTH + 2.0, entity.getZ() + 3.5);
        for (HappyGhast ghast : level.getEntitiesOfClass(HappyGhast.class, search)) {
            float length = ghast.getAttachedOrElse(ROPE, 0.0F);
            if (length <= 0.0F) {
                continue;
            }
            double yaw = Math.toRadians(ghast.yBodyRot);
            double dx = entity.getX() - ghast.getX();
            double dz = entity.getZ() - ghast.getZ();
            double right = -(dx * Math.cos(yaw) + dz * Math.sin(yaw));
            double forward = -dx * Math.sin(yaw) + dz * Math.cos(yaw);
            double height = entity.getY() + 0.4 - ghast.getY();
            if (Math.abs(right - ROPE_SIDE) < GRAB_HALF_WIDTH && Math.abs(forward) < GRAB_HALF_DEPTH
                    && height > -length && height < GRAB_TOP) {
                return true;
            }
        }
        return false;
    }

    private static void tick(MinecraftServer server) {
        // Mounting pulls the ladder up, dismounting lets it down.
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Entity vehicle = player.getVehicle();
            UUID previous = LAST_VEHICLE.get(player.getUUID());
            if (vehicle instanceof HappyGhast ghast) {
                if (!ghast.getUUID().equals(previous)) {
                    LAST_VEHICLE.put(player.getUUID(), ghast.getUUID());
                    retract(ghast.getUUID());
                }
            } else if (previous != null) {
                LAST_VEHICLE.remove(player.getUUID());
                if (((ServerLevel) player.level()).getEntity(previous) instanceof HappyGhast ghast && !ghast.isVehicle()) {
                    lower(ghast);
                }
            }
        }

        Iterator<Map.Entry<UUID, Deployed>> it = DEPLOYED.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Deployed> entry = it.next();
            Deployed deployed = entry.getValue();
            Entity entity = deployed.level().getEntity(entry.getKey());
            boolean done = !(entity instanceof HappyGhast ghast) || !ghast.isAlive() || ghast.isVehicle()
                    || ghast.position().distanceTo(deployed.anchor()) > 1.5 || deployed.idleTicks() > IDLE_TICKS;
            if (done) {
                pullUp(deployed, entry.getKey());
                it.remove();
                continue;
            }
            HappyGhast ghast = (HappyGhast) entity;
            ghast.getNavigation().stop();
            ghast.setDeltaMovement(Vec3.ZERO);
            ghast.setPos(deployed.anchor());
            // Freeze the facing too, otherwise the ghast's own look control turns it under a climbing player.
            ghast.setYRot(deployed.yaw());
            ghast.setXRot(deployed.pitch());
            ghast.setYHeadRot(deployed.yaw());
            ghast.setYBodyRot(deployed.yaw());
            ghast.yRotO = deployed.yaw();
            ghast.xRotO = deployed.pitch();
            ghast.yHeadRotO = deployed.yaw();
            ghast.yBodyRotO = deployed.yaw();

            Vec3 rope = ghast.position().add(offset(ghast.yBodyRot, ROPE_SIDE, 0));
            float length = ghast.getAttachedOrElse(ROPE, 0.0F);
            AABB column = new AABB(rope.x - 2, ghast.getY() - length - 1, rope.z - 2, rope.x + 2, ghast.getY() + GRAB_TOP + 2, rope.z + 2);
            List<ServerPlayer> near = deployed.level().getEntitiesOfClass(ServerPlayer.class, column, p -> true);
            entry.setValue(new Deployed(deployed.level(), deployed.anchor(), deployed.yaw(), deployed.pitch(), near.isEmpty() ? deployed.idleTicks() + 1 : 0));
        }
    }
}
