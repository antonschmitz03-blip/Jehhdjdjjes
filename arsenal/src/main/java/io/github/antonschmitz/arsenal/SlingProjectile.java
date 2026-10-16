package io.github.antonschmitz.arsenal;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A thrown stone. It is a vanilla snowball carrying the ammo item, so it needs nothing on the client: it flies
 * as the item, shatters into item particles and works for players without the mod. Only the server side
 * (speed, gravity, damage) is different.
 */
public class SlingProjectile extends Snowball {
    private static final int MAX_AGE = 100;

    private float damage;
    private float knockback;
    private boolean flaming;
    private int shrapnel;
    private boolean pellet;
    private final boolean explosive;

    public SlingProjectile(Level level, LivingEntity owner, ItemStack ammo, SlingAmmo type, float power) {
        super(level, owner, ammo.copyWithCount(1));
        this.damage = type.damage() * power;
        this.knockback = type.knockback() * power;
        this.explosive = type.explosive();
    }

    /** Power, Punch and Flame from the sling, applied like the bow enchantments. */
    void enchant(int power, int punch, boolean flame, float whirl, int shrapnel) {
        this.damage *= (1.0F + 0.1F * power) * (1.0F + whirl);
        this.shrapnel = shrapnel;
        if (!explosive) {
            this.knockback += 0.4F * punch;
        }
        this.flaming = flame;
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
    protected void onHit(HitResult hitResult) {
        if (explosive && level() instanceof ServerLevel serverLevel) {
            serverLevel.explode(this, getX(), getY(), getZ(), 4.0F, Level.ExplosionInteraction.TNT);
            discard();
            return;
        }
        if (shrapnel > 0 && !pellet && level() instanceof ServerLevel serverLevel) {
            burst(serverLevel);
        }
        super.onHit(hitResult);
    }

    /** Shrapnel: the stone bursts into small fast pellets (4, 6 or 8) that fly out in all directions. */
    private void burst(ServerLevel serverLevel) {
        if (!(getOwner() instanceof LivingEntity owner)) {
            return;
        }
        for (int i = 0; i < 2 + 2 * shrapnel; i++) {
            SlingProjectile piece = new SlingProjectile(serverLevel, owner, getItem(), SlingAmmo.GRAVEL, 0.75F);
            piece.pellet = true;
            piece.setPos(getX(), getY(), getZ());
            Vec3 dir = new Vec3(random.nextGaussian(), random.nextGaussian() * 0.6 + 0.2, random.nextGaussian()).normalize();
            piece.setDeltaMovement(dir.scale(1.2));
            serverLevel.addFreshEntity(piece);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        Entity target = hitResult.getEntity();
        if (level() instanceof ServerLevel serverLevel) {
            target.hurtServer(serverLevel, damageSources().thrown(this, getOwner()), damage);
            if (flaming && damage > 0) {
                target.igniteForSeconds(5.0F);
            }
            if (knockback > 0) {
                Vec3 push = getDeltaMovement().multiply(1, 0, 1).normalize().scale(knockback);
                target.push(push.x, 0.1, push.z);
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
