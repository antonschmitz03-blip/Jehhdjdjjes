package com.firemerald.additionalplacements.mixin;

import com.firemerald.additionalplacements.block.interfaces.IPaneConnectable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(IronBarsBlock.class)
public class MixinIronBarsBlock {
    @Inject(method = "getStateForPlacement(Lnet/minecraft/world/item/context/BlockPlaceContext;)Lnet/minecraft/world/level/block/state/BlockState;", at = @At("RETURN"), cancellable = true)
    public void getStateForPlacement(BlockPlaceContext context, CallbackInfoReturnable<BlockState> cir) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = cir.getReturnValue();
        state = additionalplacements$updateConnect(state, level.getBlockState(pos.north()), Direction.NORTH);
        state = additionalplacements$updateConnect(state, level.getBlockState(pos.east()), Direction.EAST);
        state = additionalplacements$updateConnect(state, level.getBlockState(pos.south()), Direction.SOUTH);
        state = additionalplacements$updateConnect(state, level.getBlockState(pos.west()), Direction.WEST);
        cir.setReturnValue(state);
    }

    @Inject(method = BlockMethods.UPDATE_SHAPE_NAME, at = @At("RETURN"), cancellable = true)
    public void updateShape(BlockState state, LevelReader level, ScheduledTickAccess scheduledTickAccess, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random, CallbackInfoReturnable<BlockState> cir) {
        if (direction.getAxis().isHorizontal()) cir.setReturnValue(additionalplacements$updateConnect(cir.getReturnValue(), neighborState, direction));
    }

    @Unique
    private BlockState additionalplacements$updateConnect(BlockState currentState, BlockState theirState, Direction connectFace) {
        BooleanProperty prop = CrossCollisionBlock.PROPERTY_BY_DIRECTION.get(connectFace);
        if (!currentState.getValue(prop) && additionalplacements$connectOverride(theirState, connectFace)) {
            return currentState.setValue(prop, true);
        } else {
            return currentState;
        }
    }

    @Unique
    private boolean additionalplacements$connectOverride(BlockState theirState, Direction connectFace) {
        return theirState.getBlock() instanceof IPaneConnectable connectable && connectable.additionalplacements$paneConnectOverride(theirState, Direction.Axis.Y, connectFace);
    }
}
