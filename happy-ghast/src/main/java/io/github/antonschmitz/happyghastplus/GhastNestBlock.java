package io.github.antonschmitz.happyghastplus;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * One cell of a ghast nest. A nest covers 6x6 blocks minus the four corners (32 cells); {@link #X} and {@link #Z}
 * say where the cell sits in that square, so any cell can find the whole nest. Only the low floor has collision,
 * so the ghast and players stand on the straw and walk through the rim.
 */
public class GhastNestBlock extends Block {
    public static final int SIZE = 6;
    public static final int CELLS = SIZE * SIZE - 4;
    public static final IntegerProperty X = IntegerProperty.create("x", 0, SIZE - 1);
    public static final IntegerProperty Z = IntegerProperty.create("z", 0, SIZE - 1);
    /** The way a ghast faces when it settles on the nest: the way the player looked when placing it. Same on every cell. */
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    /** Height of the collision floor in blocks; a settled ghast rests on it. */
    public static final double FLOOR = 4.0 / 16.0;

    private static final VoxelShape OUTLINE = Block.box(0, 0, 0, 16, 14, 16);
    private static final VoxelShape COLLISION = Block.box(0, 0, 0, 16, 4, 16);

    public GhastNestBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(X, 3).setValue(Z, 3).setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(X, Z, FACING);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return OUTLINE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return COLLISION;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            breakNest(level, pos, state, !player.hasInfiniteMaterials());
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    public static boolean isCell(int x, int z) {
        boolean edgeX = x == 0 || x == SIZE - 1;
        boolean edgeZ = z == 0 || z == SIZE - 1;
        return !(edgeX && edgeZ);
    }

    /** The block position of cell (0, 0) of the nest that {@code state} at {@code pos} belongs to. */
    public static BlockPos origin(BlockPos pos, BlockState state) {
        return pos.offset(-state.getValue(X), 0, -state.getValue(Z));
    }

    /**
     * Places a nest centred on {@code center} (cell (3, 3)). Returns false and changes nothing if any cell is
     * not free.
     */
    public static boolean placeNest(Level level, BlockPos center) {
        return placeNest(level, center, Direction.NORTH);
    }

    /** Same, with the direction a settled ghast will face. */
    public static boolean placeNest(Level level, BlockPos center, Direction facing) {
        BlockPos origin = center.offset(-3, 0, -3);
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                if (isCell(x, z) && !level.getBlockState(origin.offset(x, 0, z)).canBeReplaced()) {
                    return false;
                }
            }
        }
        BlockState nest = HappyGhastPlus.GHAST_NEST.defaultBlockState();
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                if (isCell(x, z)) {
                    level.setBlock(origin.offset(x, 0, z), nest.setValue(X, x).setValue(Z, z).setValue(FACING, facing), Block.UPDATE_ALL);
                }
            }
        }
        return true;
    }

    /**
     * Removes every other cell of the nest that the cell at {@code broken} belongs to (the caller removes that one)
     * and drops one nest item if {@code drop} is set.
     */
    public static void breakNest(Level level, BlockPos broken, BlockState state, boolean drop) {
        BlockPos origin = origin(broken, state);
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                BlockPos pos = origin.offset(x, 0, z);
                BlockState cell = level.getBlockState(pos);
                if (!pos.equals(broken) && cell.is(HappyGhastPlus.GHAST_NEST)
                        && cell.getValue(X) == x && cell.getValue(Z) == z) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
        if (drop) {
            Block.popResource(level, broken, new ItemStack(HappyGhastPlus.GHAST_NEST_ITEM));
        }
    }
}
