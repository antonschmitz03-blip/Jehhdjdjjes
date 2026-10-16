package io.github.antonschmitz.arsenal;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
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
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
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
        drawn = (int) (drawn * (1.0F + 0.25F * ArsenalEnchantments.level(player, stack, ArsenalEnchantments.STEADY_HAND)));
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
            fire(level, player, ammoStack, type, power, stack, drawn);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW,
                    SoundSource.PLAYERS, 0.9F, 0.6F + 0.4F * power);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BREEZE_WIND_CHARGE_BURST.value(),
                    SoundSource.PLAYERS, 0.3F, 1.4F);
            if (!player.hasInfiniteMaterials()) {
                // Infinity keeps the stone, but never a TNT block.
                if (type == SlingAmmo.TNT || enchantLevel(level, stack, Enchantments.INFINITY) == 0) {
                    ammoStack.shrink(1);
                }
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
        fire(level, player, ammoStack, type, power, ItemStack.EMPTY, 0);
    }

    /** Level of an enchantment on the stack, 0 when it has none. */
    public static int enchantLevel(Level level, ItemStack stack, ResourceKey<Enchantment> key) {
        return level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(key)
                .map(holder -> EnchantmentHelper.getItemEnchantmentLevel(holder, stack)).orElse(0);
    }

    /**
     * Throws the ammo; Power, Punch, Flame, Whirl and Shrapnel on the sling add to every pellet. Whirl rewards
     * holding the draw past full power: its full bonus is reached a second after that.
     */
    public static void fire(Level level, Player player, ItemStack ammoStack, SlingAmmo type, float power, ItemStack sling,
            int drawnTicks) {
        float whirl = ArsenalEnchantments.level(player, sling, ArsenalEnchantments.WHIRL) * 0.2F
                * Mth.clamp((drawnTicks - FULL_DRAW_TICKS) / (float) FULL_DRAW_TICKS, 0.0F, 1.0F);
        int shrapnel = ArsenalEnchantments.level(player, sling, ArsenalEnchantments.SHRAPNEL);
        int powerLevel = enchantLevel(level, sling, Enchantments.POWER);
        int punchLevel = enchantLevel(level, sling, Enchantments.PUNCH);
        boolean flame = enchantLevel(level, sling, Enchantments.FLAME) > 0;
        for (int i = 0; i < type.pellets(); i++) {
            SlingProjectile projectile = new SlingProjectile(level, player, ammoStack, type, power);
            projectile.enchant(powerLevel, punchLevel, flame, whirl, shrapnel);
            projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F,
                    MAX_SPEED * Mth.clamp(power, 0.1F, 1.0F), type.spread() * (1.5F - power));
            level.addFreshEntity(projectile);
        }
    }

    /**
     * The off hand first, then deepslate, then any other stone, then gravel and snowballs from the inventory.
     * TNT is only thrown from the off hand.
     */
    public static ItemStack findAmmo(Player player) {
        if (SlingAmmo.of(player.getOffhandItem()) != null) {
            return player.getOffhandItem();
        }
        for (SlingAmmo preferred : new SlingAmmo[]{SlingAmmo.DEEPSLATE_BLOCK, SlingAmmo.STONE_BLOCK, null}) {
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                SlingAmmo type = SlingAmmo.of(stack);
                if (type != null && type != SlingAmmo.TNT && (preferred == null || type == preferred)) {
                    return stack;
                }
            }
        }
        return null;
    }
}
