package io.github.antonschmitz.happyghastplus.item;

import io.github.antonschmitz.happyghastplus.GhastNestBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;

/** Places a 6x6 ghast nest on top of the clicked block, centred on the block above it. */
public class GhastNestItem extends Item {
    public GhastNestItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getClickedFace() != Direction.UP) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        Player player = context.getPlayer();
        BlockPos center = context.getClickedPos().above();
        if (!GhastNestBlock.placeNest(level, center)) {
            if (player != null) {
                player.sendOverlayMessage(Component.translatable("message.happyghastplus.ghast_nest.no_room"));
            }
            return InteractionResult.FAIL;
        }
        level.playSound(null, center, SoundType.GRASS.getPlaceSound(), SoundSource.BLOCKS, 1.0F, 0.8F);
        if (player == null || !player.hasInfiniteMaterials()) {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.SUCCESS;
    }
}
