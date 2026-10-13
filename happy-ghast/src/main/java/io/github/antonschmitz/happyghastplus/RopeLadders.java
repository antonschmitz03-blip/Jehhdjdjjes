package io.github.antonschmitz.happyghastplus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A happy ghast can let down a rope ladder (temporary ladder blocks hanging from its side to the ground).
 * It comes down when the whistle calls a nearby ghast or a rider dismounts, and is pulled back up when someone
 * rides the ghast, the ghast moves, or nobody has used it for a minute. The ghast holds still while it is down.
 */
public final class RopeLadders {
    private static final int MAX_LENGTH = 32;
    private static final int IDLE_TICKS = 20 * 60;

    private static final Map<UUID, Deployed> DEPLOYED = new HashMap<>();
    private static final Map<UUID, UUID> LAST_VEHICLE = new HashMap<>();

    private record Deployed(ServerLevel level, List<BlockPos> blocks, Vec3 anchor, int idleTicks) {}

    private RopeLadders() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(RopeLadders::tick);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            for (Deployed deployed : DEPLOYED.values()) {
                removeBlocks(deployed);
            }
            DEPLOYED.clear();
        });
    }

    /** Lets the ladder down on the side of the ghast that faces {@code towards}. */
    public static void lower(HappyGhast ghast, Vec3 towards) {
        if (ghast.isBaby() || ghast.isVehicle() || !(ghast.level() instanceof ServerLevel level)) {
            return;
        }
        retract(ghast.getUUID());

        double dx = towards.x - ghast.getX();
        double dz = towards.z - ghast.getZ();
        Direction side = Math.abs(dx) > Math.abs(dz) ? (dx > 0 ? Direction.EAST : Direction.WEST) : (dz > 0 ? Direction.SOUTH : Direction.NORTH);

        AABB box = ghast.getBoundingBox();
        double reach = ghast.getBbWidth() / 2 + 0.5;
        int x = Mth.floor(ghast.getX() + side.getStepX() * reach);
        int z = Mth.floor(ghast.getZ() + side.getStepZ() * reach);
        int top = Mth.floor(box.maxY) - 1;

        List<BlockPos> placed = new ArrayList<>();
        BlockState ladder = HappyGhastPlus.ROPE_LADDER.defaultBlockState().setValue(LadderBlock.FACING, side);
        for (int y = top; y > top - MAX_LENGTH && y >= level.getMinY(); y--) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState current = level.getBlockState(pos);
            boolean water = current.getFluidState().is(Fluids.WATER) && current.getFluidState().isSource() && current.is(Blocks.WATER);
            if (!current.isAir() && !water) {
                break;
            }
            level.setBlock(pos, ladder.setValue(LadderBlock.WATERLOGGED, water), 3);
            placed.add(pos);
        }
        if (placed.isEmpty()) {
            return;
        }
        level.playSound(null, x + 0.5, top, z + 0.5, SoundEvents.LADDER_PLACE, SoundSource.NEUTRAL, 1.0F, 0.8F);
        DEPLOYED.put(ghast.getUUID(), new Deployed(level, placed, ghast.position(), 0));
    }

    public static boolean isDeployed(HappyGhast ghast) {
        return DEPLOYED.containsKey(ghast.getUUID());
    }

    public static void retract(UUID ghast) {
        Deployed deployed = DEPLOYED.remove(ghast);
        if (deployed != null) {
            removeBlocks(deployed);
        }
    }

    private static void removeBlocks(Deployed deployed) {
        for (BlockPos pos : deployed.blocks()) {
            BlockState state = deployed.level().getBlockState(pos);
            if (state.is(HappyGhastPlus.ROPE_LADDER)) {
                boolean water = state.getValue(LadderBlock.WATERLOGGED);
                deployed.level().setBlock(pos, water ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    private static void tick(MinecraftServer server) {
        // Mounting pulls the ladder up, dismounting lets it down.
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Entity vehicle = player.getVehicle();
            UUID previous = LAST_VEHICLE.get(player.getUUID());
            if (vehicle instanceof HappyGhast ghast) {
                if (!ghast.getUUID().equals(previous)) {
                    LAST_VEHICLE.put(player.getUUID(), ghast.getUUID());
                    retract(ghast.getUUID());
                }
            } else if (previous != null) {
                LAST_VEHICLE.remove(player.getUUID());
                if (((ServerLevel) player.level()).getEntity(previous) instanceof HappyGhast ghast && !ghast.isVehicle()) {
                    lower(ghast, player.position());
                }
            }
        }

        Iterator<Map.Entry<UUID, Deployed>> it = DEPLOYED.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Deployed> entry = it.next();
            Deployed deployed = entry.getValue();
            Entity entity = deployed.level().getEntity(entry.getKey());
            boolean done = !(entity instanceof HappyGhast ghast) || !ghast.isAlive() || ghast.isVehicle()
                    || ghast.position().distanceTo(deployed.anchor()) > 1.5 || deployed.idleTicks() > IDLE_TICKS;
            if (done) {
                removeBlocks(deployed);
                it.remove();
                continue;
            }
            HappyGhast ghast = (HappyGhast) entity;
            ghast.getNavigation().stop();
            ghast.setDeltaMovement(Vec3.ZERO);
            ghast.setPos(deployed.anchor());

            BlockPos bottom = deployed.blocks().getLast();
            AABB column = new AABB(bottom).expandTowards(0, deployed.blocks().size(), 0).inflate(1.5);
            boolean inUse = !deployed.level().getEntitiesOfClass(ServerPlayer.class, column, p -> true).isEmpty();
            entry.setValue(new Deployed(deployed.level(), deployed.blocks(), deployed.anchor(), inUse ? 0 : deployed.idleTicks() + 1));
        }
    }
}
