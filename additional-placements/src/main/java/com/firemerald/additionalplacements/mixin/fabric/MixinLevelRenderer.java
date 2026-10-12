package com.firemerald.additionalplacements.mixin.fabric;

import com.firemerald.additionalplacements.client.ClientModEvents;
import com.firemerald.additionalplacements.config.APConfigs;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class MixinLevelRenderer {
    @Inject(method = "renderBlockOutline(Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lcom/mojang/blaze3d/vertex/PoseStack;ZLnet/minecraft/client/renderer/state/level/LevelRenderState;)V", at = @At("HEAD"))
    public void renderBlockOutline(MultiBufferSource.BufferSource bufferSource, PoseStack poseStack, boolean isTranslucent, LevelRenderState levelRenderState, CallbackInfo ci) {
        if (!APConfigs.client().enablePlacementHighlight.get()) return;
        BlockOutlineRenderState blockOutlineRenderState = levelRenderState.blockOutlineRenderState;
        if (blockOutlineRenderState != null) {
            if (blockOutlineRenderState.isTranslucent() != isTranslucent) return;
            Minecraft minecraft = Minecraft.getInstance();
            if (!(minecraft.hitResult instanceof BlockHitResult hitResult && hitResult.getType() == HitResult.Type.BLOCK && hitResult.getBlockPos().equals(blockOutlineRenderState.pos()))) return;
            ClientModEvents.performBlockHighlight(levelRenderState, (block, player, deltaTracker) ->
                    block.additionalplacements$renderHighlight(poseStack, bufferSource, player, hitResult, levelRenderState, deltaTracker));
        }
    }
}
