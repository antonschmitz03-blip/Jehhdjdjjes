package io.github.antonschmitz.graves;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** Picks where a grave goes. */
public final class GravePlacement {
    /** Search radius (in blocks, each direction) for a free spot on solid ground. */
    private static final int RADIUS = 16;

    private GravePlacement() {
    }

    /**
     * The nearest free spot (air or a replaceable block like grass or water, not lava) with a solid, non-lava
     * block below it. If there is none nearby (e.g. a Nether lava ocean), the surface of the lava the player
     * died in, and as a last resort the death position itself (clamped into the world, e.g. after falling into
     * the void). Returns null only if even that is blocked.
     */
    public static @Nullable BlockPos find(ServerLevel level, BlockPos deathPos) {
        int minY = level.getMinY() + 1;
        int maxY = level.getMaxY() - 1;
        BlockPos origin = new BlockPos(deathPos.getX(), Math.clamp(deathPos.getY(), minY, maxY), deathPos.getZ());

        BlockPos best = null;
        int bestDistance = Integer.MAX_VALUE;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dy = -RADIUS; dy <= RADIUS; dy++) {
            int y = origin.getY() + dy;
            if (y < minY || y > maxY) {
                continue;
            }
            for (int dx = -RADIUS; dx <= RADIUS; dx++) {
                for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                    // Slightly prefer staying at the same height over going up or down.
                    int distance = dx * dx + dz * dz + dy * dy * 2;
                    if (distance >= bestDistance) {
                        continue;
                    }
                    pos.set(origin.getX() + dx, y, origin.getZ() + dz);
                    if (isFree(level, pos) && isSolidGround(level, pos.below())) {
                        best = pos.immutable();
                        bestDistance = distance;
                    }
                }
            }
        }
        if (best != null) {
            return best;
        }

        if (isLava(level.getBlockState(origin))) {
            pos.set(origin);
            while (pos.getY() < maxY && isLava(level.getBlockState(pos.above()))) {
                pos.move(Direction.UP);
            }
            return pos.immutable();
        }
        return isFree(level, origin) ? origin : null;
    }

    private static boolean isFree(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return (state.isAir() || state.canBeReplaced()) && !isLava(state);
    }

    private static boolean isSolidGround(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return !isLava(state) && state.isFaceSturdy(level, pos, Direction.UP);
    }

    private static boolean isLava(BlockState state) {
        return state.getFluidState().is(FluidTags.LAVA);
    }
}
