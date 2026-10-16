package io.github.antonschmitz.biomesize.mixin;

import io.github.antonschmitz.biomesize.BiomeSize;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.SpringFeature;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Single-block water and lava springs (the ones that make waterfalls and lavafalls in cave walls) follow the
 * Underground Water and Underground Lava sliders in the overworld. Below 100% attempts are skipped; above 100%
 * extra attempts are made near the original one.
 */
@Mixin(SpringFeature.class)
public abstract class SpringFeatureMixin {
    private static final ThreadLocal<Boolean> biomesize$extra = ThreadLocal.withInitial(() -> false);

    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void biomesize$scale(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin,
                                 CallbackInfoReturnable<Boolean> cir) {
        if (biomesize$extra.get() || level.getLevel().dimension() != Level.OVERWORLD) {
            return;
        }
        SpringFeature self = (SpringFeature) (Object) this;
        FluidState state = self.state();
        BiomeSize.Settings s = BiomeSize.active();
        int percent;
        if (state.is(FluidTags.LAVA)) {
            percent = s.lavaPercent();
        } else if (state.is(FluidTags.WATER)) {
            percent = s.waterPercent();
        } else {
            return;
        }
        if (percent == BiomeSize.DEFAULT_PERCENT) {
            return;
        }
        if (percent < BiomeSize.DEFAULT_PERCENT) {
            if (random.nextInt(100) >= percent) {
                cir.setReturnValue(false);
            }
            return;
        }
        float more = percent / 100.0F - 1.0F;
        int extra = (int) more;
        if (random.nextFloat() < more - extra) {
            extra++;
        }
        biomesize$extra.set(true);
        try {
            for (int i = 0; i < extra; i++) {
                BlockPos nearby = origin.offset(random.nextInt(15) - 7, random.nextInt(49) - 24, random.nextInt(15) - 7);
                self.place(level, generator, random, nearby);
            }
        } finally {
            biomesize$extra.set(false);
        }
    }
}
