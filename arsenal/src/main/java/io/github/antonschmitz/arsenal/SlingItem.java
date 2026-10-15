package io.github.antonschmitz.arsenal;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * Hold right-click to swing it, release to throw. Ammo comes from the off hand first, then the inventory.
 * A full draw takes one second; shorter draws throw slower and softer.
 */
public class SlingItem extends Item {
    public static final int FULL_DRAW_TICKS = 20;
    /** Blocks per tick at a full draw (a bow tops out at 3.0). */
    public static final float MAX_SPEED = 4.5F;

    public SlingItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!player.hasInfiniteMaterials() && findAmmo(player) == null) {
            return InteractionResult.FAIL;
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity user, int remainingUseTicks) {
        if (!(user instanceof Player player)) {
            return false;
        }
        int drawn = getUseDuration(stack, user) - remainingUseTicks;
        float power = power(drawn);
        if (power < 0.1F) {
            return false;
        }
        ItemStack ammoStack = findAmmo(player);
        if (ammoStack == null) {
            if (!player.hasInfiniteMaterials()) {
                return false;
            }
            ammoStack = new ItemStack(Items.COBBLESTONE);
        }
        if (!level.isClientSide()) {
            SlingAmmo type = SlingAmmo.of(ammoStack);
            fire(level, player, ammoStack, type, power);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW,
                    SoundSource.PLAYERS, 0.9F, 0.6F + 0.4F * power);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BREEZE_WIND_CHARGE_BURST.value(),
                    SoundSource.PLAYERS, 0.3F, 1.4F);
            if (!player.hasInfiniteMaterials()) {
                ammoStack.shrink(1);
                stack.hurtAndBreak(1, player, player.getUsedItemHand());
            }
        }
        return true;
    }

    /** Same curve as a bow: slow start, full power after a second. */
    public static float power(int drawnTicks) {
        float f = drawnTicks / (float) FULL_DRAW_TICKS;
        f = (f * f + f * 2.0F) / 3.0F;
        return Math.min(f, 1.0F);
    }

    /** Throws the ammo (several pellets for gravel). */
    public static void fire(Level level, Player player, ItemStack ammoStack, SlingAmmo type, float power) {
        for (int i = 0; i < type.pellets(); i++) {
            SlingProjectile projectile = new SlingProjectile(level, player, ammoStack, type, power);
            projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F,
                    MAX_SPEED * Mth.clamp(power, 0.1F, 1.0F), type.spread() * (1.5F - power));
            level.addFreshEntity(projectile);
        }
    }

    /** The off hand first, then deepslate, then any other stone, then gravel and snowballs from the inventory. */
    public static ItemStack findAmmo(Player player) {
        if (SlingAmmo.of(player.getOffhandItem()) != null) {
            return player.getOffhandItem();
        }
        for (SlingAmmo preferred : new SlingAmmo[]{SlingAmmo.DEEPSLATE_BLOCK, SlingAmmo.STONE_BLOCK, null}) {
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                SlingAmmo type = SlingAmmo.of(stack);
                if (type != null && (preferred == null || type == preferred)) {
                    return stack;
                }
            }
        }
        return null;
    }
}
