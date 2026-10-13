package io.github.antonschmitz.tbaddons.mixin;

import io.github.antonschmitz.tbaddons.MobWard;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Every natural spawn attempt asks {@code SpawnPlacements.checkSpawnRules}; the Mob Ward Upgrade vetoes hostile ones. */
@Mixin(SpawnPlacements.class)
public abstract class SpawnPlacementsMixin {
    @Inject(method = "checkSpawnRules", at = @At("HEAD"), cancellable = true)
    private static void tbaddons$mobWard(EntityType<?> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos,
                                         RandomSource random, CallbackInfoReturnable<Boolean> cir) {
        if (MobWard.blocksSpawn(type, level, reason, pos)) {
            cir.setReturnValue(false);
        }
    }
}
