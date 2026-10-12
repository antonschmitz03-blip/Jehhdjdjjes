package io.github.antonschmitz.arsenal;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A thrown stone. It is a vanilla snowball carrying the ammo item, so it needs nothing on the client: it flies
 * as the item, shatters into item particles and works for players without the mod. Only the server side
 * (speed, gravity, damage) is different.
 */
public class SlingProjectile extends Snowball {
    private static final int MAX_AGE = 100;

    private final float damage;
    private final float knockback;

    public SlingProjectile(Level level, LivingEntity owner, ItemStack ammo, SlingAmmo type, float power) {
        super(level, owner, ammo.copyWithCount(1));
        this.damage = type.damage() * power;
        this.knockback = type.knockback() * power;
    }

    /** Stones fly flat: a snowball falls at 0.03, an arrow at 0.05. */
    @Override
    protected double getDefaultGravity() {
        return 0.01;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && tickCount > MAX_AGE) {
            discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        Entity target = hitResult.getEntity();
        if (level() instanceof ServerLevel serverLevel) {
            if (damage > 0) {
                target.hurtServer(serverLevel, damageSources().thrown(this, getOwner()), damage);
            }
            if (knockback > 0 && target instanceof LivingEntity living) {
                Vec3 velocity = getDeltaMovement();
                living.knockback(knockback, -velocity.x, -velocity.z);
            }
        }
    }

    public float damage() {
        return damage;
    }

    /** For the self test: hit an entity without flying there. */
    void hit(Entity target) {
        onHitEntity(new EntityHitResult(target));
    }
}
