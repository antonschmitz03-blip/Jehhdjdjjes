package io.github.antonschmitz.vplustweaks.mixin;

import io.github.antonschmitz.vplustweaks.TweaksConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Ladders no longer need a solid block behind them, so they can float and hang from other ladders. */
@Mixin(LadderBlock.class)
public abstract class LadderBlockMixin {
    @Inject(method = "canSurvive", at = @At("HEAD"), cancellable = true)
    private void vplustweaks$floatingLadders(BlockState state, LevelReader level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (TweaksConfig.enabled(TweaksConfig.FLOATING_LADDERS)) {
            cir.setReturnValue(true);
        }
    }
}
