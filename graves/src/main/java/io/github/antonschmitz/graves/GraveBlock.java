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

import java.util.EnumMap;
import java.util.Map;

public class GraveBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

    /** Plinth, burial mound, pedestal and headstone, modelled facing north (headstone at the back). */
    private static final double[][] BOXES = {
            {1, 0, 1, 15, 1, 15},
            {3, 1, 2, 13, 3, 9},
            {2, 1, 9, 14, 3, 14},
            {3, 3, 10, 13, 15, 13},
    };
    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

    static {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            VoxelShape shape = Shapes.empty();
            for (double[] b : BOXES) {
                shape = Shapes.or(shape, rotated(facing, b));
            }
            SHAPES.put(facing, shape);
        }
    }

    /** Rotates a north-facing box the same way the blockstate rotates the model (y = 90 per step clockwise). */
    private static VoxelShape rotated(Direction facing, double[] b) {
        double x1 = b[0], z1 = b[2], x2 = b[3], z2 = b[5];
        double[] r = switch (facing) {
            case EAST -> new double[] {16 - z2, x1, 16 - z1, x2};
            case SOUTH -> new double[] {16 - x2, 16 - z2, 16 - x1, 16 - z1};
            case WEST -> new double[] {z1, 16 - x2, z2, 16 - x1};
            default -> new double[] {x1, z1, x2, z2};
        };
        return Block.box(r[0], b[1], r[1], r[2], b[4], r[3]);
    }

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
        return SHAPES.get(state.getValue(FACING));
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
