package io.github.antonschmitz.vplustweaks;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;

/** Opening or closing one half of a double door also toggles the other half. Sneak to use a single door. */
public final class DoubleDoors {
    private DoubleDoors() {
    }

    public static void register() {
        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            if (!level.isClientSide() && hand == InteractionHand.MAIN_HAND && !player.isSecondaryUseActive()) {
                toggleNeighbour(player, level, hitResult.getBlockPos());
            }
            return InteractionResult.PASS;
        });
    }

    private static void toggleNeighbour(Player player, Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof DoorBlock door) || !door.type().canOpenByHand()) {
            return;
        }

        Direction facing = state.getValue(DoorBlock.FACING);
        DoorHingeSide hinge = state.getValue(DoorBlock.HINGE);
        BlockPos otherPos = pos.relative(hinge == DoorHingeSide.LEFT ? facing.getClockWise() : facing.getCounterClockWise());
        BlockState other = level.getBlockState(otherPos);

        boolean isPartner = other.getBlock() instanceof DoorBlock otherDoor
                && otherDoor.type().canOpenByHand()
                && other.getValue(DoorBlock.FACING) == facing
                && other.getValue(DoorBlock.HINGE) != hinge
                && other.getValue(DoorBlock.HALF) == state.getValue(DoorBlock.HALF)
                && other.getValue(DoorBlock.OPEN) == state.getValue(DoorBlock.OPEN);
        if (isPartner) {
            // Vanilla toggles the clicked door right after this callback.
            ((DoorBlock) other.getBlock()).setOpen(player, level, other, otherPos, !state.getValue(DoorBlock.OPEN));
        }
    }
}
