package io.github.antonschmitz.waystones;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A two-block-tall stone (about 1.6 blocks of model). Right-click a new waystone to discover it, right-click a
 * known one to pick where to go (and, for the owner, to rename it). Everything is keyed by the lower half's
 * position; all waystone info lives in {@link WaystoneData}, so there is no block entity.
 */
public class WaystoneBlock extends Block {
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;

    private static final VoxelShape LOWER_SHAPE = Shapes.or(Block.box(1, 0, 1, 15, 4, 15), Block.box(3, 4, 3, 13, 10, 13),
            Block.box(4, 10, 4, 12, 16, 12));
    private static final VoxelShape UPPER_SHAPE = Shapes.or(Block.box(4.5, 0, 4.5, 11.5, 2, 11.5), Block.box(3, 2, 3, 13, 6, 13),
            Block.box(4.5, 6, 4.5, 11.5, 11, 11.5));

    public WaystoneBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF);
    }

    /** The position the waystone is registered under. */
    public static BlockPos lowerPos(BlockState state, BlockPos pos) {
        return state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(HALF) == DoubleBlockHalf.UPPER ? UPPER_SHAPE : LOWER_SHAPE;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        if (pos.getY() < level.getMaxY() && level.getBlockState(pos.above()).canBeReplaced(context)) {
            return defaultBlockState();
        }
        return null;
    }

    /** Either half disappears with the other one. The half that was broken drops the item, the other goes quietly. */
    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        BlockPos otherPos = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
        BlockState other = level.getBlockState(otherPos);
        if (other.is(this) && other.getValue(HALF) != state.getValue(HALF)) {
            level.setBlock(otherPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
        }
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }

    /** Sets both halves; used by placement and the self test. */
    static void placeBoth(Level level, BlockPos lower) {
        level.setBlock(lower, SimpleWaystones.WAYSTONE.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(lower.above(), SimpleWaystones.WAYSTONE.defaultBlockState().setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
        if (!(level instanceof ServerLevel serverLevel) || !(placer instanceof ServerPlayer player)) {
            return;
        }
        Component customName = stack.get(DataComponents.CUSTOM_NAME);
        WaystoneData.Waystone waystone = register(serverLevel, pos, player, customName != null ? customName.getString() : null);
        WaystoneData.get().discover(player.getUUID(), waystone);
        player.sendSystemMessage(Component.translatable("simplewaystones.message.placed", waystone.displayName())
                .withStyle(ChatFormatting.AQUA), true);
    }

    static WaystoneData.Waystone register(ServerLevel level, BlockPos pos, ServerPlayer owner, @Nullable String name) {
        String biome = level.getBiome(pos).unwrapKey()
                .map(key -> "biome." + key.identifier().getNamespace() + "." + key.identifier().getPath())
                .orElse("biome.minecraft.plains");
        return WaystoneData.get().add(level, pos, name, biome, owner.getUUID(), owner.getPlainTextName());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            activate(serverLevel, lowerPos(state, pos), serverPlayer);
        }
        return InteractionResult.SUCCESS;
    }

    /** Discovers the waystone, or opens the list when it is already known. */
    static void activate(ServerLevel level, BlockPos pos, ServerPlayer player) {
        WaystoneData data = WaystoneData.get();
        WaystoneData.Waystone waystone = data.at(level, pos);
        if (waystone == null) {
            // Placed without a player (commands, structures): whoever finds it first owns it.
            waystone = register(level, pos, player, null);
        }
        if (data.discover(player.getUUID(), waystone)) {
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0F, 1.0F);
            player.sendSystemMessage(Component.translatable("simplewaystones.message.discovered", waystone.displayName())
                    .withStyle(ChatFormatting.AQUA), true);
            return;
        }
        WaystoneMenu.open(player, waystone, null);
    }
}
