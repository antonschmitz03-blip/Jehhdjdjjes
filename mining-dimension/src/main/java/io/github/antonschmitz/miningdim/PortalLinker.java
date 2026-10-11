package io.github.antonschmitz.miningdim;

import java.util.Comparator;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.BlockUtil;
import net.minecraft.util.BlockUtil.FoundRectangle;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Finds or builds the matching portal on the other side. Both dimensions share coordinates 1:1. */
public final class PortalLinker {
    private static final int SEARCH_RADIUS = 64;

    private PortalLinker() {
    }

    static @Nullable TeleportTransition destination(ServerLevel currentLevel, Entity entity, BlockPos entryPos) {
        ResourceKey<Level> targetKey = currentLevel.dimension() == MiningDimension.MINING ? Level.OVERWORLD : MiningDimension.MINING;
        ServerLevel target = currentLevel.getServer().getLevel(targetKey);
        if (target == null || !PortalLighting.canHavePortal(currentLevel)) {
            return null;
        }

        WorldBorder border = target.getWorldBorder();
        BlockPos approximate = border.clampToBounds(entity.getX(), entity.getY(), entity.getZ());
        BlockState entryState = currentLevel.getBlockState(entryPos);
        Axis entryAxis = entryState.getOptionalValue(MiningPortalBlock.AXIS).orElse(Axis.X);

        FoundRectangle exit;
        TeleportTransition.PostTeleportTransition post;
        Optional<BlockPos> existing = findPortal(target, approximate, border);
        if (existing.isPresent()) {
            BlockPos pos = existing.get();
            BlockState state = target.getBlockState(pos);
            exit = BlockUtil.getLargestRectangleAround(pos, state.getValue(MiningPortalBlock.AXIS), 21, Axis.Y, 21,
                    p -> target.getBlockState(p) == state);
            post = TeleportTransition.PLAY_PORTAL_SOUND.then(e -> e.placePortalTicket(pos));
        } else {
            if (entity.isSpectator()) {
                return null;
            }
            exit = createPortal(target, approximate, entryAxis);
            post = TeleportTransition.PLAY_PORTAL_SOUND.then(TeleportTransition.PLACE_PORTAL_TICKET);
        }

        Vec3 offset;
        if (entryState.is(MiningDimension.PORTAL)) {
            FoundRectangle entryArea = BlockUtil.getLargestRectangleAround(entryPos, entryAxis, 21, Axis.Y, 21,
                    p -> currentLevel.getBlockState(p) == entryState);
            offset = entity.getRelativePortalPosition(entryAxis, entryArea);
        } else {
            offset = new Vec3(0.5, 0.0, 0.0);
        }
        return transition(target, exit, entryAxis, offset, entity, post);
    }

    private static Optional<BlockPos> findPortal(ServerLevel level, BlockPos approximate, WorldBorder border) {
        PoiManager poi = level.getPoiManager();
        poi.ensureLoadedAndValid(level, approximate, SEARCH_RADIUS);
        return poi.getInSquare(type -> type.is(MiningDimension.PORTAL_POI), approximate, SEARCH_RADIUS, PoiManager.Occupancy.ANY)
                .map(PoiRecord::getPos)
                .filter(border::isWithinBounds)
                .filter(pos -> level.getBlockState(pos).is(MiningDimension.PORTAL))
                .min(Comparator.<BlockPos>comparingDouble(p -> p.distSqr(approximate)).thenComparingInt(Vec3i::getY));
    }

    /**
     * Builds a 2x3 portal with a chiseled deepslate frame and clears a small room around it, so it also works
     * inside solid rock. In the overworld it goes on the surface; in the mining dimension at the same height.
     */
    static FoundRectangle createPortal(ServerLevel level, BlockPos approximate, Axis axis) {
        int x = approximate.getX();
        int z = approximate.getZ();
        int y;
        if (level.dimension() == MiningDimension.MINING) {
            // Stay clear of the bedrock floor and roof.
            y = Mth.clamp(approximate.getY(), level.getMinY() + 8, level.getMinY() + level.getLogicalHeight() - 12);
        } else {
            y = Mth.clamp(level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), level.getMinY() + 8, level.getMaxY() - 8);
        }
        BlockPos origin = new BlockPos(x, y, z);
        Direction along = Direction.get(AxisDirection.POSITIVE, axis);
        Direction across = along.getClockWise();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        // Room: 4 wide along the portal, 5 deep across it, 4 high, with a solid floor.
        for (int w = -1; w <= 2; w++) {
            for (int d = -2; d <= 2; d++) {
                for (int h = -1; h <= 3; h++) {
                    cursor.setWithOffset(origin, along.getStepX() * w + across.getStepX() * d, h, along.getStepZ() * w + across.getStepZ() * d);
                    if (h == -1) {
                        BlockState floor = level.getBlockState(cursor);
                        if (!floor.isSolid() || !floor.getFluidState().isEmpty()) {
                            level.setBlockAndUpdate(cursor, Blocks.STONE.defaultBlockState());
                        }
                    } else if (d != 0 || h == 3) {
                        level.setBlockAndUpdate(cursor, Blocks.AIR.defaultBlockState());
                    }
                }
            }
        }
        // Frame.
        for (int w = -1; w <= 2; w++) {
            for (int h = -1; h <= 3; h++) {
                if (w == -1 || w == 2 || h == -1 || h == 3) {
                    cursor.setWithOffset(origin, along.getStepX() * w, h, along.getStepZ() * w);
                    level.setBlockAndUpdate(cursor, Blocks.CHISELED_DEEPSLATE.defaultBlockState());
                }
            }
        }
        // Portal.
        BlockState portal = MiningDimension.PORTAL.defaultBlockState().setValue(MiningPortalBlock.AXIS, axis);
        for (int w = 0; w < 2; w++) {
            for (int h = 0; h < 3; h++) {
                cursor.setWithOffset(origin, along.getStepX() * w, h, along.getStepZ() * w);
                level.setBlock(cursor, portal, 18);
            }
        }
        return new FoundRectangle(origin.immutable(), 2, 3);
    }

    private static TeleportTransition transition(ServerLevel level, FoundRectangle exit, Axis entryAxis, Vec3 offset, Entity entity,
                                                 TeleportTransition.PostTeleportTransition post) {
        BlockPos bottomLeft = exit.minCorner;
        Axis axis = level.getBlockState(bottomLeft).getOptionalValue(MiningPortalBlock.AXIS).orElse(Axis.X);
        double width = exit.axis1Size;
        double height = exit.axis2Size;
        EntityDimensions dimensions = entity.getDimensions(entity.getPose());
        int rotation = entryAxis == axis ? 0 : 90;
        double right = dimensions.width() / 2.0 + (width - dimensions.width()) * offset.x();
        double up = (height - dimensions.height()) * offset.y();
        double forward = 0.5 + offset.z();
        boolean xAligned = axis == Axis.X;
        Vec3 target = new Vec3(
                bottomLeft.getX() + (xAligned ? right : forward),
                bottomLeft.getY() + up,
                bottomLeft.getZ() + (xAligned ? forward : right));
        Vec3 free = net.minecraft.world.level.portal.PortalShape.findCollisionFreePosition(target, level, entity, dimensions);
        return new TeleportTransition(level, free, Vec3.ZERO, rotation, 0.0F, Relative.union(Relative.DELTA, Relative.ROTATION), post);
    }
}
