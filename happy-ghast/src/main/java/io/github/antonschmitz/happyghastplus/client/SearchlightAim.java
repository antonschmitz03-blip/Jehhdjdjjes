package io.github.antonschmitz.happyghastplus.client;

import io.github.antonschmitz.happyghastplus.GhastSearchlight;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Where a ghast's searchlight sits and points, worked out on the client each frame. The lamp turns towards the block
 * the rider looks at (the same ray the server uses for the light), so the beam ends where the light is.
 */
public record SearchlightAim(boolean on, float yaw, float pitch, Vec3 lens, Vec3 target, Vec3 normal) {
    /** Lamp placement in blocks: on top of the harness, this far ahead of the ghast's centre. */
    static final float MOUNT_HEIGHT = 4.0F;
    static final float MOUNT_FORWARD = 1.3F;
    static final float SCALE = 2.0F;
    /** Height of the head's tilt axis above the lamp's foot, and the lens's distance in front of that axis. */
    static final float PIVOT = 6.5F / 16.0F * SCALE;
    static final float LENS = 3.6F / 16.0F * SCALE;
    /** Resting pose while switched off or nobody rides: straight ahead, a little down. */
    private static final float REST_PITCH = 20.0F;

    /** For a ghast without a (visible) searchlight. */
    public static final SearchlightAim NONE = new SearchlightAim(false, 0.0F, 0.0F, null, null, null);

    public static SearchlightAim of(HappyGhast ghast, float partialTick) {
        if (ghast.isBaby() || !GhastSearchlight.isMounted(ghast) || ghast.getItemBySlot(EquipmentSlot.BODY).isEmpty()) {
            return NONE;
        }
        float bodyYaw = Mth.rotLerp(partialTick, ghast.yBodyRotO, ghast.yBodyRot);
        double yawRad = bodyYaw * Mth.DEG_TO_RAD;
        Vec3 forward = new Vec3(-Math.sin(yawRad), 0.0, Math.cos(yawRad));
        Vec3 pivot = ghast.getPosition(partialTick).add(forward.scale(MOUNT_FORWARD)).add(0.0, MOUNT_HEIGHT + PIVOT, 0.0);

        LivingEntity rider = ghast.getControllingPassenger();
        if (!ghast.getAttachedOrElse(GhastSearchlight.ON, false) || !(rider instanceof Player)) {
            return new SearchlightAim(false, 0.0F, REST_PITCH, pivot.add(forward.scale(LENS)), null, null);
        }
        BlockHitResult hit = GhastSearchlight.target(ghast.level(), rider, partialTick);
        Vec3 target = hit != null ? hit.getLocation()
                : rider.getEyePosition(partialTick).add(rider.getViewVector(partialTick).scale(GhastSearchlight.RANGE));
        Vec3 normal = hit != null ? hit.getDirection().getUnitVec3() : null;

        Vec3 d = target.subtract(pivot);
        double horizontal = Math.sqrt(d.x * d.x + d.z * d.z);
        float targetYaw = (float) (Mth.atan2(-d.x, d.z) * Mth.RAD_TO_DEG);
        float yaw = Mth.wrapDegrees(targetYaw - bodyYaw);
        float pitch = (float) (-Mth.atan2(d.y, horizontal) * Mth.RAD_TO_DEG);
        Vec3 lens = pivot.add(d.normalize().scale(LENS));
        return new SearchlightAim(true, yaw, pitch, lens, target, normal);
    }
}
