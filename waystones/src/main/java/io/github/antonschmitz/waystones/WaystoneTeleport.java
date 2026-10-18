package io.github.antonschmitz.waystones;

import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** XP cost and the actual teleport. */
public final class WaystoneTeleport {
    private WaystoneTeleport() {
    }

    /**
     * Free below {@link WaystoneConfig#blocksPerLevel} blocks, then one level per step (capped), plus the extra
     * levels for changing dimension. Distance is horizontal, between the player and the waystone.
     */
    public static int cost(ServerPlayer player, WaystoneData.Waystone target) {
        if (player.isCreative()) {
            return 0;
        }
        WaystoneConfig config = WaystoneConfig.get();
        double dx = player.getX() - (target.pos().getX() + 0.5);
        double dz = player.getZ() - (target.pos().getZ() + 0.5);
        int levels = Math.min(config.maxDistanceLevels, (int) (Math.sqrt(dx * dx + dz * dz) / config.blocksPerLevel));
        if (!target.dimension().equals(WaystoneData.dimensionId(player.level()))) {
            levels += config.dimensionChangeLevels;
        }
        return levels;
    }

    static @Nullable ServerLevel level(ServerPlayer player, WaystoneData.Waystone waystone) {
        Identifier id = Identifier.tryParse(waystone.dimension());
        if (id == null) {
            return null;
        }
        return player.level().getServer().getLevel(ResourceKey.create(Registries.DIMENSION, id));
    }

    /**
     * Teleports next to the waystone. {@code free} skips the XP cost (warp scroll). Returns false, with a message
     * to the player, when the waystone is gone or the player can't pay.
     */
    public static boolean teleport(ServerPlayer player, WaystoneData.Waystone target, boolean free) {
        ServerLevel level = level(player, target);
        BlockPos pos = target.pos();
        if (level != null) {
            level.getChunk(pos);
        }
        if (level == null || !level.getBlockState(pos).is(SimpleWaystones.WAYSTONE)) {
            WaystoneData.get().removeKey(target.key());
            player.sendSystemMessage(Component.translatable("simplewaystones.message.gone", target.displayName())
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }
        int cost = free ? 0 : cost(player, target);
        if (player.experienceLevel < cost) {
            player.sendSystemMessage(Component.translatable("simplewaystones.message.not_enough_xp", cost)
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }

        Level from = player.level();
        from.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
        Vec3 spot = safeSpot(level, pos);
        player.teleport(new TeleportTransition(level, spot, Vec3.ZERO, player.getYRot(), player.getXRot(), Set.of(),
                TeleportTransition.DO_NOTHING));
        player.resetFallDistance();
        if (cost > 0) {
            player.giveExperienceLevels(-cost);
        }
        level.playSound(null, spot.x, spot.y, spot.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    /** A free two-block space next to the waystone with solid ground, else on top of it. {@code pos} is the lower half. */
    static Vec3 safeSpot(ServerLevel level, BlockPos pos) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos candidate = pos.relative(direction);
            for (BlockPos feet : new BlockPos[]{candidate, candidate.below(), candidate.above()}) {
                if (isFree(level, feet) && isFree(level, feet.above())
                        && level.getBlockState(feet.below()).isFaceSturdy(level, feet.below(), Direction.UP)) {
                    return Vec3.atBottomCenterOf(feet);
                }
            }
        }
        return Vec3.atBottomCenterOf(pos.above(2));
    }

    private static boolean isFree(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                && !level.getFluidState(pos).is(FluidTags.LAVA);
    }
}
