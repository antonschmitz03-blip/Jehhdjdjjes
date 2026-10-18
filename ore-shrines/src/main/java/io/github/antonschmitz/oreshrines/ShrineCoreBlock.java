package io.github.antonschmitz.oreshrines;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The block on top of the pyramid. Right-click with an offering to pick the effect, with ore to fill a Wisdom
 * shrine, or with anything else to open the overview screen (and see the range outlined for a few seconds).
 */
public class ShrineCoreBlock extends BaseEntityBlock {
    /** The effect, so the client can colour the crystal. */
    public static final EnumProperty<ShrineEffect> EFFECT = EnumProperty.create("effect", ShrineEffect.class);
    /** Has an effect and a pyramid: the crystal glows in the effect's colour instead of dull grey. */
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    /** Stone base, plinth and dish, four corner posts and the floating crystal. */
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(0, 0, 0, 16, 4, 16),
            Block.box(3, 4, 3, 13, 8, 13),
            Block.box(0.5, 4, 0.5, 3, 13, 3),
            Block.box(13, 4, 0.5, 15.5, 13, 3),
            Block.box(0.5, 4, 13, 3, 13, 15.5),
            Block.box(13, 4, 13, 15.5, 13, 15.5),
            Block.box(6, 8, 6, 10, 16, 10));

    public ShrineCoreBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(EFFECT, ShrineEffect.NONE).setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(EFFECT, LIT);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
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
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hitResult) {
        if (!(player instanceof ServerPlayer serverPlayer) || !(level.getBlockEntity(pos) instanceof ShrineBlockEntity shrine)) {
            return InteractionResult.SUCCESS;
        }
        ShrineEffect offered = ShrineEffect.byOffering(stack);
        if (offered != null && offered != shrine.effect()) {
            if (!serverPlayer.isCreative()) {
                stack.shrink(1);
            }
            applyOffering(serverPlayer, shrine, offered);
            return InteractionResult.SUCCESS;
        }
        int value = ShrineBlockEntity.xpValue(stack);
        if (value > 0 && shrine.effect() == ShrineEffect.WISDOM) {
            int taken = shrine.feed(value, stack.getCount());
            if (taken > 0) {
                if (!serverPlayer.isCreative()) {
                    stack.shrink(taken);
                }
                level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 1.2F);
                serverPlayer.sendSystemMessage(Component.translatable("oreshrines.message.stored", shrine.storedXp(),
                        ShrineBlockEntity.MAX_XP).withStyle(ChatFormatting.AQUA), true);
            } else {
                serverPlayer.sendSystemMessage(Component.translatable("oreshrines.message.full").withStyle(ChatFormatting.GRAY), true);
            }
            return InteractionResult.SUCCESS;
        }
        ShrineMenu.open(serverPlayer, shrine);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof ShrineBlockEntity shrine) {
            ShrineMenu.open(serverPlayer, shrine);
        }
        return InteractionResult.SUCCESS;
    }

    /** Switches the shrine to {@code effect}. The offering itself has already been taken by the caller. */
    static void applyOffering(ServerPlayer player, ShrineBlockEntity shrine, ShrineEffect effect) {
        shrine.setEffect(effect);
        BlockPos pos = shrine.getBlockPos();
        ServerLevel level = player.level();
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.sendParticles(new net.minecraft.core.particles.DustParticleOptions(effect.color(), 1.5F),
                pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 20, 0.4, 0.4, 0.4, 0.0);
        player.sendSystemMessage(Component.translatable("oreshrines.message.effect_set",
                Component.translatable(effect.nameKey()).withStyle(effect.formatting())), true);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof ShrineBlockEntity shrine ? shrine.comparatorSignal() : 0;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ShrineBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, OreShrines.SHRINE_BLOCK_ENTITY, ShrineBlockEntity::serverTick);
    }
}
