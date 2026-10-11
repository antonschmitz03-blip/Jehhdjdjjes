package io.github.antonschmitz.vplustweaks;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

/**
 * Right-clicking a ladder with a ladder extends the column to the next free spot below it
 * (or above it while sneaking), however long the column is.
 */
public final class FastLadders {
    private static final int MAX_COLUMN = 320;

    private FastLadders() {}

    public static void register() {
        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            ItemStack stack = player.getItemInHand(hand);
            if (!(stack.getItem() instanceof BlockItem item) || !(item.getBlock() instanceof LadderBlock ladder)) {
                return InteractionResult.PASS;
            }
            BlockState clicked = level.getBlockState(hitResult.getBlockPos());
            if (!(clicked.getBlock() instanceof LadderBlock)) {
                return InteractionResult.PASS;
            }

            Direction step = player.isSecondaryUseActive() ? Direction.UP : Direction.DOWN;
            Direction facing = clicked.getValue(LadderBlock.FACING);
            BlockPos target = hitResult.getBlockPos();
            int steps = 0;
            do {
                target = target.relative(step);
                steps++;
            } while (steps < MAX_COLUMN
                    && level.getBlockState(target).getBlock() instanceof LadderBlock
                    && level.getBlockState(target).getValue(LadderBlock.FACING) == facing);

            if (level.isOutsideBuildHeight(target) || !level.getBlockState(target).canBeReplaced()) {
                return InteractionResult.PASS;
            }

            if (!level.isClientSide()) {
                BlockState placed = ladder.defaultBlockState()
                        .setValue(LadderBlock.FACING, facing)
                        .setValue(LadderBlock.WATERLOGGED, level.getFluidState(target).is(Fluids.WATER));
                level.setBlock(target, placed, 3);
                level.playSound(null, target, placed.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
                if (!player.hasInfiniteMaterials()) {
                    stack.shrink(1);
                }
            }
            return InteractionResult.SUCCESS;
        });
    }
}
