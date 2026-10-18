package io.github.antonschmitz.graves;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class GraveBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

    private static final VoxelShape BASE = Block.box(1, 0, 1, 15, 2, 15);
    /** The headstone stands at the back, so you face its front when you face the grave. */
    private static final VoxelShape SHAPE_NORTH = Shapes.or(BASE, Block.box(2, 2, 10, 14, 15, 13));
    private static final VoxelShape SHAPE_SOUTH = Shapes.or(BASE, Block.box(2, 2, 3, 14, 15, 6));
    private static final VoxelShape SHAPE_WEST = Shapes.or(BASE, Block.box(10, 2, 2, 13, 15, 14));
    private static final VoxelShape SHAPE_EAST = Shapes.or(BASE, Block.box(3, 2, 2, 6, 15, 14));

    public GraveBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case SOUTH -> SHAPE_SOUTH;
            case WEST -> SHAPE_WEST;
            case EAST -> SHAPE_EAST;
            default -> SHAPE_NORTH;
        };
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!(player instanceof ServerPlayer serverPlayer) || !(level.getBlockEntity(pos) instanceof GraveBlockEntity grave)) {
            return InteractionResult.SUCCESS;
        }
        if (grave.isOwner(serverPlayer)) {
            // Right-click: chest screen. Sneak + right-click (empty hands): everything straight back into its slots.
            if (serverPlayer.isShiftKeyDown()) {
                grave.restoreTo(serverPlayer);
            } else {
                grave.openMenu(serverPlayer);
            }
        } else {
            serverPlayer.sendSystemMessage(Component.translatable("simplegraves.message.not_yours", grave.ownerName())
                    .withStyle(ChatFormatting.GRAY), true);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GraveBlockEntity(pos, state);
    }
}
