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
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Right-click a new waystone to discover it, right-click a known one to pick where to go (and, for the owner,
 * to rename it). All waystone info lives in {@link WaystoneData}, so there is no block entity.
 */
public class WaystoneBlock extends Block {
    private static final VoxelShape SHAPE = Shapes.or(Block.box(1, 0, 1, 15, 3, 15), Block.box(3, 3, 3, 13, 16, 13));

    public WaystoneBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
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
            activate(serverLevel, pos, serverPlayer);
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
