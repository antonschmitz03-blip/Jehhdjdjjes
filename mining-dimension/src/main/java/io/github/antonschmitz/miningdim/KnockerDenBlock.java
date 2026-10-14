package io.github.antonschmitz.miningdim;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A knocker den's stash sack. On random ticks it keeps up to three knockers living around it.
 * Breaking it spills the stash and makes its knockers furious.
 */
public class KnockerDenBlock extends Block {
    static final int MAX_KNOCKERS = 3;
    private static final VoxelShape SHAPE = Block.box(3.0, 0.0, 3.0, 13.0, 12.0, 13.0);

    public KnockerDenBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    static List<Knocker> residents(Level level, BlockPos pos) {
        return level.getEntitiesOfClass(Knocker.class, new AABB(pos).inflate(Knocker.DEN_RADIUS + 12), k -> pos.equals(k.getDen()));
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        populate(level, pos, random);
    }

    /** Tops the den up to three knockers, out of sight of nearby players. Returns how many spawned. */
    static int populate(ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getNearestPlayer(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 6.0, false) != null) return 0;
        List<Knocker> residents = residents(level, pos);
        int missing = MAX_KNOCKERS - residents.size();
        // An empty den fills up at once; an established one regrows one knocker at a time.
        int toSpawn = residents.isEmpty() ? missing : Math.min(1, missing);
        int spawned = 0;
        for (int i = 0; i < toSpawn; i++) {
            BlockPos spot = findSpot(level, pos, random);
            if (spot == null) continue;
            Knocker knocker = MiningDimension.KNOCKER.spawn(level, spot, EntitySpawnReason.SPAWNER);
            if (knocker == null) continue;
            knocker.setDen(pos);
            if (!residents.isEmpty()) knocker.copyMoodFrom(residents.get(0));
            spawned++;
        }
        return spawned;
    }

    private static BlockPos findSpot(ServerLevel level, BlockPos den, RandomSource random) {
        for (int attempt = 0; attempt < 12; attempt++) {
            BlockPos pos = den.offset(random.nextInt(7) - 3, random.nextInt(5) - 2, random.nextInt(7) - 3);
            if (level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir()
                    && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP)) {
                return pos;
            }
        }
        return null;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && !player.isCreative()) {
            for (Knocker knocker : residents(level, pos)) {
                knocker.setTarget(player);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
}
