package io.github.antonschmitz.miningdim;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Vanilla's nether portal frame search, with a chiseled deepslate frame and the mining portal block. */
public final class PortalShape {
    private static final int MIN_WIDTH = 2;
    private static final int MIN_HEIGHT = 3;
    private static final int MAX_SIZE = 21;

    private final Axis axis;
    private final Direction rightDir;
    private final BlockPos bottomLeft;
    private final int width;
    private final int height;
    private final int portalBlocks;

    private PortalShape(Axis axis, Direction rightDir, BlockPos bottomLeft, int width, int height, int portalBlocks) {
        this.axis = axis;
        this.rightDir = rightDir;
        this.bottomLeft = bottomLeft;
        this.width = width;
        this.height = height;
        this.portalBlocks = portalBlocks;
    }

    public static boolean isFrame(BlockState state) {
        return state.is(Blocks.CHISELED_DEEPSLATE);
    }

    private static boolean isEmpty(BlockState state) {
        return state.isAir() || state.is(BlockTags.FIRE) || state.is(MiningDimension.PORTAL);
    }

    /** An empty, valid frame around {@code pos}, trying {@code preferredAxis} first. */
    public static Optional<PortalShape> findEmpty(LevelAccessor level, BlockPos pos, Axis preferredAxis) {
        PortalShape first = find(level, pos, preferredAxis);
        if (first.isValid() && first.portalBlocks == 0) {
            return Optional.of(first);
        }
        PortalShape second = find(level, pos, preferredAxis == Axis.X ? Axis.Z : Axis.X);
        return second.isValid() && second.portalBlocks == 0 ? Optional.of(second) : Optional.empty();
    }

    public static PortalShape find(BlockGetter level, BlockPos pos, Axis axis) {
        Direction rightDir = axis == Axis.X ? Direction.WEST : Direction.SOUTH;
        BlockPos bottomLeft = bottomLeft(level, rightDir, pos);
        if (bottomLeft == null) {
            return new PortalShape(axis, rightDir, pos, 0, 0, 0);
        }
        int width = distanceUntilEdgeAboveFrame(level, bottomLeft, rightDir);
        if (width < MIN_WIDTH || width > MAX_SIZE) {
            return new PortalShape(axis, rightDir, bottomLeft, 0, 0, 0);
        }

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int portalBlocks = 0;
        int height = MAX_SIZE;
        rows:
        for (int y = 0; y < MAX_SIZE; y++) {
            if (!isFrame(level.getBlockState(cursor.set(bottomLeft).move(Direction.UP, y).move(rightDir, -1)))
                    || !isFrame(level.getBlockState(cursor.set(bottomLeft).move(Direction.UP, y).move(rightDir, width)))) {
                height = y;
                break;
            }
            for (int x = 0; x < width; x++) {
                BlockState state = level.getBlockState(cursor.set(bottomLeft).move(Direction.UP, y).move(rightDir, x));
                if (!isEmpty(state)) {
                    height = y;
                    break rows;
                }
                if (state.is(MiningDimension.PORTAL)) {
                    portalBlocks++;
                }
            }
        }
        if (height < MIN_HEIGHT || height > MAX_SIZE) {
            return new PortalShape(axis, rightDir, bottomLeft, width, 0, portalBlocks);
        }
        for (int x = 0; x < width; x++) {
            if (!isFrame(level.getBlockState(cursor.set(bottomLeft).move(Direction.UP, height).move(rightDir, x)))) {
                return new PortalShape(axis, rightDir, bottomLeft, width, 0, portalBlocks);
            }
        }
        return new PortalShape(axis, rightDir, bottomLeft, width, height, portalBlocks);
    }

    private static BlockPos bottomLeft(BlockGetter level, Direction rightDir, BlockPos pos) {
        int minY = Math.max(level.getMinY(), pos.getY() - MAX_SIZE);
        while (pos.getY() > minY && isEmpty(level.getBlockState(pos.below()))) {
            pos = pos.below();
        }
        Direction leftDir = rightDir.getOpposite();
        int edge = distanceUntilEdgeAboveFrame(level, pos, leftDir) - 1;
        return edge < 0 ? null : pos.relative(leftDir, edge);
    }

    private static int distanceUntilEdgeAboveFrame(BlockGetter level, BlockPos pos, Direction direction) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int i = 0; i <= MAX_SIZE; i++) {
            cursor.set(pos).move(direction, i);
            BlockState state = level.getBlockState(cursor);
            if (!isEmpty(state)) {
                return isFrame(state) ? i : 0;
            }
            if (!isFrame(level.getBlockState(cursor.move(Direction.DOWN)))) {
                return 0;
            }
        }
        return 0;
    }

    public boolean isValid() {
        return width >= MIN_WIDTH && width <= MAX_SIZE && height >= MIN_HEIGHT && height <= MAX_SIZE;
    }

    public boolean isComplete() {
        return isValid() && portalBlocks == width * height;
    }

    public void createPortalBlocks(LevelAccessor level) {
        BlockState portal = MiningDimension.PORTAL.defaultBlockState().setValue(MiningPortalBlock.AXIS, axis);
        BlockPos.betweenClosed(bottomLeft, bottomLeft.relative(Direction.UP, height - 1).relative(rightDir, width - 1))
                .forEach(pos -> level.setBlock(pos, portal, 18));
    }
}
