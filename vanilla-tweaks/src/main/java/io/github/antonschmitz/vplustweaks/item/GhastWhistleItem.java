package io.github.antonschmitz.vplustweaks.item;

import io.github.antonschmitz.vplustweaks.GhastFeatures;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Calls every happy ghast you have ridden within 128 blocks to fly to you. */
public class GhastWhistleItem extends Item {
    public GhastWhistleItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GHAST_AMBIENT, SoundSource.PLAYERS, 1.0F, 1.6F);
        if (player instanceof ServerPlayer serverPlayer) {
            int count = GhastFeatures.callGhasts(serverPlayer);
            serverPlayer.sendSystemMessage(count > 0
                    ? Component.translatable("message.vplustweaks.ghast_whistle.called", count)
                    : Component.translatable("message.vplustweaks.ghast_whistle.none", GhastFeatures.WHISTLE_RANGE), true);
            player.getCooldowns().addCooldown(stack, 60);
        }
        return InteractionResult.SUCCESS;
    }
}
