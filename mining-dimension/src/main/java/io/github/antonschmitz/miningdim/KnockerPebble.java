package io.github.antonschmitz.miningdim;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** A little stone a knocker throws when you hit one of its den. Stings for half a heart. */
public class KnockerPebble extends ThrowableItemProjectile {
    public KnockerPebble(EntityType<? extends KnockerPebble> type, Level level) {
        super(type, level);
    }

    public KnockerPebble(Level level, LivingEntity owner) {
        super(MiningDimension.KNOCKER_PEBBLE, owner, level, new ItemStack(Items.COBBLESTONE));
    }

    @Override
    protected Item getDefaultItem() {
        return Items.COBBLESTONE;
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 3) {
            for (int i = 0; i < 6; i++) {
                this.level().addParticle(new ItemParticleOption(ParticleTypes.ITEM, ItemStackTemplate.fromNonEmptyStack(this.getItem())),
                        this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
            }
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        if (hit.getEntity() instanceof Knocker) return;
        hit.getEntity().hurt(this.damageSources().thrown(this, this.getOwner()), 1.0F);
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (!this.level().isClientSide()) {
            this.level().broadcastEntityEvent(this, (byte) 3);
            this.discard();
        }
    }
}
