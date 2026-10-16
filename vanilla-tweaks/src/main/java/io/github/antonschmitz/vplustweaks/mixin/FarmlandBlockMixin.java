package io.github.antonschmitz.vplustweaks.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import io.github.antonschmitz.vplustweaks.TweaksConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Landing on farmland no longer tramples it. Fall damage is unaffected. */
@Mixin(FarmlandBlock.class)
public abstract class FarmlandBlockMixin {
    @WrapWithCondition(
            method = "fallOn",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/FarmlandBlock;turnToBaseBlock(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V")
    )
    private boolean vplustweaks$preventTrampling(FarmlandBlock block, Entity entity, BlockState state, Level level, BlockPos pos) {
        return !TweaksConfig.enabled(TweaksConfig.NO_CROP_TRAMPLING);
    }
}
