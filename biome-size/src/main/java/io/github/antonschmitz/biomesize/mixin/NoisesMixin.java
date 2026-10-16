package io.github.antonschmitz.biomesize.mixin;

import io.github.antonschmitz.biomesize.BiomeSize;
import io.github.antonschmitz.biomesize.ScaledNoise;
import net.minecraft.core.HolderGetter;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.levelgen.synth.Noise;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Every world-gen noise a RandomState uses is created here. Stretching the climate noises changes biome
 * size; stretching continentalness and erosion changes land shape. Weirdness (rivers, peaks) is left
 * alone, like vanilla Large Biomes does.
 */
@Mixin(Noises.class)
public abstract class NoisesMixin {
    @Inject(method = "instantiate", at = @At("RETURN"), cancellable = true)
    private static void biomesize$scale(HolderGetter<NormalNoise> noises, PositionalRandomFactory random,
                                        ResourceKey<NormalNoise> name, CallbackInfoReturnable<Noise> cir) {
        int percent;
        if (name == Noises.TEMPERATURE || name == Noises.VEGETATION
                || name == Noises.TEMPERATURE_LARGE || name == Noises.VEGETATION_LARGE) {
            percent = BiomeSize.activeBiomePercent();
        } else if (name == Noises.CONTINENTALNESS || name == Noises.EROSION
                || name == Noises.CONTINENTALNESS_LARGE || name == Noises.EROSION_LARGE) {
            percent = BiomeSize.activeTerrainPercent();
        } else {
            return;
        }
        if (percent != BiomeSize.DEFAULT_PERCENT) {
            cir.setReturnValue(new ScaledNoise(cir.getReturnValue(), BiomeSize.DEFAULT_PERCENT / (double) percent));
        }
    }
}
