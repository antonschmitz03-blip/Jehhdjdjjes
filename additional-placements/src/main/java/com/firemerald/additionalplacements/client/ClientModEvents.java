package com.firemerald.additionalplacements.client;

import com.firemerald.additionalplacements.block.interfaces.IPlacementBlock;
import com.firemerald.additionalplacements.client.block.highlight.IBlockHighlightAction;
import com.firemerald.additionalplacements.config.APConfigs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public class ClientModEvents {
    public static void onInput() {
        if (Minecraft.getInstance().player == null) return;
        if (APClientData.AP_PLACEMENT_KEY.consumeClick() && !APClientData.placementKeyDown) {
            APClientData.togglePlacementEnabled();
            APClientData.placementKeyPressTime = System.currentTimeMillis();
            APClientData.placementKeyDown = true;
        }
        else if (APClientData.placementKeyDown && !APClientData.AP_PLACEMENT_KEY.isDown()) { //released
            APClientData.placementKeyDown = false;
            if ((System.currentTimeMillis() - APClientData.placementKeyPressTime) > APConfigs.client().toggleQuickpressTime.get()) { //more than half-second press, toggle back
                APClientData.togglePlacementEnabled();
            }
        }
    }

    public static void onClientPostTick() {
        if (Minecraft.getInstance().player == null) return;
        if ((System.currentTimeMillis() - APClientData.lastSynchronizedTime) > 10000) { //synchronize every 10 seconds in case of desync
            APClientData.synchronizePlacementEnabled();
        }
    }

    public static void performBlockHighlight(LevelRenderState levelRenderState, IBlockHighlightAction highlightAction) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) stack = player.getOffhandItem();
        if (stack.getItem() instanceof BlockItem) {
            Block block = ((BlockItem) stack.getItem()).getBlock();
            if (block instanceof IPlacementBlock<?> verticalBlock) {
                if (verticalBlock.additionalplacements$hasAdditionalStates()) {
                    highlightAction.perform(verticalBlock, player, minecraft.getDeltaTracker());
                }
            }
        }
    }
}
