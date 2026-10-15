package io.github.antonschmitz.happyghastplus.client;

import io.github.antonschmitz.happyghastplus.GhastFeatures;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;
import net.minecraft.world.phys.Vec3;

/**
 * A faint trail of small flames and smoke from the rocket engines of a speed-upgraded happy ghast while it flies forward.
 * Purely visual and client side: the upgrade flag is synced to every client that can see the ghast.
 */
public final class GhastEngineParticles {
    /** Blocks per tick the ghast has to move along its heading before the engines fire. */
    private static final double MIN_FORWARD_SPEED = 0.05;
    private static final double RANGE = 64.0;

    private GhastEngineParticles() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(GhastEngineParticles::tick);
    }

    private static void tick(Minecraft client) {
        ClientLevel level = client.level;
        if (level == null || client.player == null || client.isPaused()) {
            return;
        }
        for (HappyGhast ghast : level.getEntitiesOfClass(HappyGhast.class, client.player.getBoundingBox().inflate(RANGE),
                g -> !g.isBaby() && !g.isInvisible() && g.getAttachedOrElse(GhastFeatures.SPEED_UPGRADED, false))) {
            RandomSource random = ghast.getRandom();
            double yaw = ghast.getYRot() * Mth.DEG_TO_RAD;
            double forwardX = -Math.sin(yaw);
            double forwardZ = Math.cos(yaw);
            Vec3 velocity = new Vec3(ghast.getX() - ghast.xo, ghast.getY() - ghast.yo, ghast.getZ() - ghast.zo);
            double forwardSpeed = velocity.x * forwardX + velocity.z * forwardZ;
            if (forwardSpeed < MIN_FORWARD_SPEED) {
                continue;
            }
            // a little more fire the faster it goes
            double power = Math.min(1.0, forwardSpeed / 0.3);
            double rightX = -forwardZ;
            double rightZ = forwardX;
            for (int side = -1; side <= 1; side += 2) {
                double x = ghast.getX() + rightX * side * GhastEngineLayer.SIDE_OFFSET - forwardX * GhastEngineLayer.NOZZLE_BACK;
                double y = ghast.getY() + GhastEngineLayer.HEIGHT;
                double z = ghast.getZ() + rightZ * side * GhastEngineLayer.SIDE_OFFSET - forwardZ * GhastEngineLayer.NOZZLE_BACK;
                double jitter = 0.015;
                double backX = velocity.x - forwardX * 0.12;
                double backZ = velocity.z - forwardZ * 0.12;
                if (random.nextDouble() < 0.25 + 0.25 * power) {
                    level.addParticle(ParticleTypes.SMALL_FLAME, x, y, z,
                            backX + random.nextGaussian() * jitter, velocity.y + random.nextGaussian() * jitter,
                            backZ + random.nextGaussian() * jitter);
                }
                if (random.nextDouble() < 0.12 + 0.13 * power) {
                    level.addParticle(ParticleTypes.SMOKE, x - forwardX * 0.1, y, z - forwardZ * 0.1,
                            backX * 0.6 + random.nextGaussian() * jitter, velocity.y + 0.02,
                            backZ * 0.6 + random.nextGaussian() * jitter);
                }
            }
        }
    }
}
