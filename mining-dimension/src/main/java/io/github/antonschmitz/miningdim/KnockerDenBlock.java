package io.github.antonschmitz.miningdim;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A knocker den's stash sack. On random ticks it keeps up to three knockers living around it, and it fills up
 * (four looks, from empty to overflowing) each time its knockers rob someone. You can hear a den before you see it:
 * muffled giggling and knocking, and a fat sack glints with gold. Breaking it spills the stash and makes its knockers furious.
 */
public class KnockerDenBlock extends Block {
    static final int MAX_KNOCKERS = 3;
    public static final IntegerProperty FILL = IntegerProperty.create("fill", 0, 3);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape EMPTY_SHAPE = Block.box(4.0, 0.0, 4.0, 12.0, 5.0, 12.0);
    private static final VoxelShape SHAPE = Block.box(3.0, 0.0, 3.0, 13.0, 12.0, 13.0);

    public KnockerDenBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(FILL, 1).setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FILL, FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FILL) == 0 ? EMPTY_SHAPE : SHAPE;
    }

    static List<Knocker> residents(Level level, BlockPos pos) {
        return level.getEntitiesOfClass(Knocker.class, new AABB(pos).inflate(Knocker.DEN_RADIUS + 12), k -> pos.equals(k.getDen()));
    }

    /** The den's sack gets a little fatter with every theft. */
    static void addLoot(Level level, BlockPos den) {
        BlockState state = level.getBlockState(den);
        if (state.getBlock() instanceof KnockerDenBlock && state.getValue(FILL) < 3) {
            level.setBlock(den, state.setValue(FILL, state.getValue(FILL) + 1), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        populate(level, pos, random);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;
        if (random.nextInt(180) == 0) {
            level.playLocalSound(x, y, z, SoundEvents.WITCH_CELEBRATE, SoundSource.NEUTRAL, 0.3F, 1.8F + random.nextFloat() * 0.3F, false);
        } else if (random.nextInt(240) == 0) {
            level.playLocalSound(x, y, z, SoundEvents.NOTE_BLOCK_BASEDRUM.value(), SoundSource.NEUTRAL, 0.7F, 0.55F, false);
        }
        int fill = state.getValue(FILL);
        if (fill >= 2 && random.nextInt(6 - fill) == 0) {
            level.addParticle(ParticleTypes.WAX_ON, pos.getX() + 0.25 + random.nextDouble() * 0.5, pos.getY() + 0.5 + random.nextDouble() * 0.5,
                    pos.getZ() + 0.25 + random.nextDouble() * 0.5, 0.0, 0.02, 0.0);
        }
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
                    && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) {
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
