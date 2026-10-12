package io.github.antonschmitz.waystones;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** Right-click: pick any discovered waystone and go there for free. Used up on a successful teleport. */
public class WarpScrollItem extends Item {
    public WarpScrollItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer) {
            WaystoneMenu.open(serverPlayer, null, hand);
        }
        return InteractionResult.SUCCESS;
    }
}
