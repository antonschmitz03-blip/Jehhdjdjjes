package io.github.antonschmitz.miningdim;

import java.util.Optional;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

/** Using flint and steel on the inside of a chiseled deepslate frame lights a mining portal. */
public final class PortalLighting {
    private PortalLighting() {
    }

    public static void register() {
        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            ItemStack stack = player.getItemInHand(hand);
            if (!stack.is(Items.FLINT_AND_STEEL) || !canHavePortal(level)) {
                return InteractionResult.PASS;
            }
            BlockPos clicked = hitResult.getBlockPos();
            if (!PortalShape.isFrame(level.getBlockState(clicked))) {
                return InteractionResult.PASS;
            }
            BlockPos inside = clicked.relative(hitResult.getDirection());
            if (!level.getBlockState(inside).isAir()) {
                return InteractionResult.PASS;
            }
            Direction facing = player.getDirection();
            Optional<PortalShape> shape = PortalShape.findEmpty(level, inside, facing.getCounterClockWise().getAxis());
            if (shape.isEmpty()) {
                return InteractionResult.PASS;
            }

            level.playSound(player, inside, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.4F + 0.8F);
            if (!level.isClientSide()) {
                shape.get().createPortalBlocks(level);
                level.gameEvent(player, GameEvent.BLOCK_PLACE, clicked);
                stack.hurtAndBreak(1, player, hand);
            }
            return InteractionResult.SUCCESS;
        });
    }

    /** Portals link the overworld and the mining dimension only. */
    static boolean canHavePortal(Level level) {
        return level.dimension() == Level.OVERWORLD || level.dimension() == MiningDimension.MINING;
    }
}
