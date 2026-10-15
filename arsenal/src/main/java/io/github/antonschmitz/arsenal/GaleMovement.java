package io.github.antonschmitz.arsenal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * What the Gale Bow does while it is held (all server side, so the mod needs no client code):
 * <ul>
 * <li>Long hop: jump while sneaking to be thrown where you look (about 7 blocks when looking level).</li>
 * <li>Air jump: one extra jump per time in the air.</li>
 * <li>Focus: while airborne, drawing the bow holds you in place and slows nearby mobs and projectiles for up to
 * 6 seconds; arrows loosed meanwhile do +50% damage. Once per time in the air.</li>
 * </ul>
 */
public final class GaleMovement {
    static final int FOCUS_TICKS = 120;
    /** Projectiles keep this share of their speed while slowed. */
    static final double PROJECTILE_SPEED = 0.1;
    /** Slowness 7 stops a walking mob; the velocity multiplier covers falling and flung ones. */
    static final int MOB_SLOWNESS = 6;
    static final double MOB_MOTION = 0.3;
    static final double FOCUS_RADIUS = 12.0;
    static final double HOP_SPEED = 1.0;
    static final double HOP_LIFT = 0.3;
    static final double AIR_JUMP_LIFT = 0.75;
    static final int HOP_COOLDOWN_TICKS = 20;
    static final double FOCUS_ARROW_BONUS = 1.5;

    private static final Map<UUID, State> STATES = new HashMap<>();

    private static final class State {
        boolean prevJump;
        boolean airJumpUsed;
        boolean focusUsed;
        int focusTicksLeft;
        long hopReadyAt;
        int airTicks;
        /** Projectiles slowed by this player's focus, with the gravity flag they had. */
        final Map<Projectile, Boolean> slowed = new HashMap<>();

        boolean focusing() {
            return focusTicksLeft > 0;
        }
    }

    private GaleMovement() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(GaleMovement::tick);
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (entity instanceof AbstractArrow arrow && arrow.getOwner() instanceof ServerPlayer player) {
                State state = STATES.get(player.getUUID());
                if (state != null && state.focusing()) {
                    // An arrow's damage scales with its speed.
                    arrow.setDeltaMovement(arrow.getDeltaMovement().scale(FOCUS_ARROW_BONUS));
                }
            }
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> handler.getPlayer().setNoGravity(false));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> endFocus(handler.getPlayer()));
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                endFocus(player);
            }
            STATES.clear();
        });
    }

    private static boolean holdsGaleBow(ServerPlayer player) {
        return player.getMainHandItem().getItem() instanceof GaleBowItem
                || player.getOffhandItem().getItem() instanceof GaleBowItem;
    }

    private static boolean drawingGaleBow(ServerPlayer player) {
        ItemStack using = player.getUseItem();
        return player.isUsingItem() && using.getItem() instanceof GaleBowItem;
    }

    static void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            State state = STATES.get(player.getUUID());
            boolean holding = player.isAlive() && !player.isSpectator() && holdsGaleBow(player);
            if (!holding) {
                if (state != null) {
                    endFocus(player);
                    STATES.remove(player.getUUID());
                }
                continue;
            }
            if (state == null) {
                state = new State();
                STATES.put(player.getUUID(), state);
            }
            tickPlayer(player, state);
        }
    }

    private static void tickPlayer(ServerPlayer player, State state) {
        // Jump is the air jump here; if an elytra opened anyway, fold it again.
        if (player.isFallFlying()) {
            player.stopFallFlying();
        }

        Input input = player.getLastClientInput();
        boolean jumpPressed = input.jump() && !state.prevJump;
        state.prevJump = input.jump();

        // The client jumps on its own, so by the time the server sees the jump key the player has usually already
        // left the ground: a jump press on the first tick in the air still counts as a ground jump.
        boolean groundJump = jumpPressed && (player.onGround() || state.airTicks == 0);
        if (groundJump && input.shift()
                && player.level().getGameTime() >= state.hopReadyAt) {
            longHop(player, state);
        }

        boolean grounded = player.onGround() || player.isInWater() || player.isInLava() || player.onClimbable()
                || player.isPassenger() || player.isFallFlying() || player.getAbilities().flying;
        if (grounded) {
            state.airTicks = 0;
            state.airJumpUsed = false;
            state.focusUsed = false;
            if (state.focusing()) {
                endFocus(player);
            }
            return;
        }
        state.airTicks++;

        // The first air tick the jump key is still held from the ground jump; a fresh press is needed.
        if (jumpPressed && !groundJump && !state.airJumpUsed && !state.focusing() && state.airTicks > 1) {
            state.airJumpUsed = true;
            // The server's velocity for a player is stale, so keep the horizontal speed the client actually moved at.
            Vec3 moved = clientMotion(player);
            player.setDeltaMovement(moved.x, AIR_JUMP_LIFT, moved.z);
            player.fallDistance = 0;
            sync(player);
        }

        if (!state.focusing()) {
            if (!state.focusUsed && drawingGaleBow(player)) {
                startFocus(player, state);
            }
        } else if (!drawingGaleBow(player)) {
            endFocus(player);
        } else {
            holdFocus(player, state);
        }
    }

    /** How far the player moved this tick, as reported by their client. */
    private static Vec3 clientMotion(ServerPlayer player) {
        return new Vec3(player.getX() - player.xo, player.getY() - player.yo, player.getZ() - player.zo);
    }

    /** Throws the player where they look, with a little extra lift so a level look still clears gaps. */
    private static void longHop(ServerPlayer player, State state) {
        Vec3 push = player.getLookAngle().scale(HOP_SPEED).add(0, HOP_LIFT, 0);
        player.setDeltaMovement(push);
        state.hopReadyAt = player.level().getGameTime() + HOP_COOLDOWN_TICKS;
        sync(player);
    }

    private static void startFocus(ServerPlayer player, State state) {
        state.focusUsed = true;
        state.focusTicksLeft = FOCUS_TICKS;
        player.setNoGravity(true);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0;
        sync(player);
    }

    private static void holdFocus(ServerPlayer player, State state) {
        if (--state.focusTicksLeft <= 0) {
            endFocus(player);
            return;
        }
        player.fallDistance = 0;
        AABB area = player.getBoundingBox().inflate(FOCUS_RADIUS);
        for (Mob mob : player.level().getEntitiesOfClass(Mob.class, area)) {
            mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 3, MOB_SLOWNESS, false, false, false));
            // Falling and flung mobs crawl as well.
            mob.setDeltaMovement(mob.getDeltaMovement().multiply(MOB_MOTION, MOB_MOTION, MOB_MOTION));
        }
        Set<Projectile> inRange = new HashSet<>();
        for (Projectile projectile : player.level().getEntitiesOfClass(Projectile.class, area)) {
            Entity owner = projectile.getOwner();
            if (owner == player || projectile.onGround()) {
                continue;
            }
            inRange.add(projectile);
            if (!state.slowed.containsKey(projectile)) {
                state.slowed.put(projectile, projectile.isNoGravity());
                projectile.setNoGravity(true);
                projectile.setDeltaMovement(projectile.getDeltaMovement().scale(PROJECTILE_SPEED));
            }
        }
        // Projectiles that left the area or hit something get their gravity back (a hit one is already removed).
        state.slowed.entrySet().removeIf(entry -> {
            Projectile projectile = entry.getKey();
            if (projectile.isRemoved()) {
                return true;
            }
            if (!inRange.contains(projectile)) {
                release(projectile, entry.getValue());
                return true;
            }
            return false;
        });
    }

    private static void release(Projectile projectile, boolean hadNoGravity) {
        projectile.setNoGravity(hadNoGravity);
        projectile.setDeltaMovement(projectile.getDeltaMovement().scale(1.0 / PROJECTILE_SPEED));
    }

    private static void endFocus(ServerPlayer player) {
        State state = STATES.get(player.getUUID());
        if (state != null) {
            state.focusTicksLeft = 0;
            state.slowed.forEach((projectile, hadNoGravity) -> {
                if (!projectile.isRemoved()) {
                    release(projectile, hadNoGravity);
                }
            });
            state.slowed.clear();
        }
        player.setNoGravity(false);
    }

    /** The client moves itself, so velocity changes have to be sent to it. */
    private static void sync(ServerPlayer player) {
        player.connection.send(new ClientboundSetEntityMotionPacket(player));
    }
}
