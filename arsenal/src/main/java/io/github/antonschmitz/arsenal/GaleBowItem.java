package io.github.antonschmitz.arsenal;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** A bow made of breeze rods. The air jump, focus and long hop live in {@link GaleMovement}. */
public class GaleBowItem extends BowItem {
    public GaleBowItem(Properties properties) {
        super(properties);
    }

    /** Steady Hand: every level draws 25% faster, by counting the draw as longer than it was. */
    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity user, int remainingUseTicks) {
        int steady = ArsenalEnchantments.level(user, stack, ArsenalEnchantments.STEADY_HAND);
        if (steady > 0) {
            int duration = getUseDuration(stack, user);
            int drawn = duration - remainingUseTicks;
            remainingUseTicks = duration - (int) (drawn * (1.0F + 0.25F * steady));
        }
        return super.releaseUsing(stack, level, user, remainingUseTicks);
    }
}
