package io.github.antonschmitz.biomesize.mixin;

import io.github.antonschmitz.biomesize.BiomeSize;
import io.github.antonschmitz.biomesize.AmplifiedNoise;
import io.github.antonschmitz.biomesize.OffsetNoise;
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
    /** How far the erosion noise moves per 100% of the Mountains slider. */
    private static final float MOUNTAIN_SHIFT = 0.3F;

    @Inject(method = "instantiate", at = @At("RETURN"), cancellable = true)
    private static void biomesize$scale(HolderGetter<NormalNoise> noises, PositionalRandomFactory random,
                                        ResourceKey<NormalNoise> name, CallbackInfoReturnable<Noise> cir) {
        BiomeSize.Settings s = BiomeSize.active();
        if (s.isDefault()) {
            return;
        }
        Noise noise = cir.getReturnValue();
        if (name == Noises.TEMPERATURE || name == Noises.VEGETATION
                || name == Noises.TEMPERATURE_LARGE || name == Noises.VEGETATION_LARGE) {
            noise = stretch(noise, s.biomePercent());
        } else if (name == Noises.CONTINENTALNESS || name == Noises.EROSION
                || name == Noises.CONTINENTALNESS_LARGE || name == Noises.EROSION_LARGE) {
            noise = stretch(noise, s.terrainPercent());
            if (name == Noises.EROSION || name == Noises.EROSION_LARGE) {
                // Low erosion is where the tall mountains are: shifting it down makes them cover more of the world.
                float shift = -MOUNTAIN_SHIFT * (s.mountainPercent() - BiomeSize.DEFAULT_PERCENT) / 100.0F;
                noise = shift == 0.0F ? noise : new OffsetNoise(noise, shift);
            }
        } else if (name == Noises.JAGGED) {
            // Strength of mountain peaks and cliffs.
            noise = amplify(noise, s.dramaticPercent() / 100.0F);
        } else if (name == Noises.CAVE_CHEESE) {
            // Big open caves: a stronger noise reaches the cave threshold more often.
            noise = amplify(noise, s.cavePercent() / 100.0F);
        } else if (name == Noises.CAVE_LAYER) {
            // The layered caverns are carved where this is small, so a weaker noise means more of them.
            noise = amplify(noise, 100.0F / s.cavePercent());
        } else if (name == Noises.SPAGHETTI_2D || name == Noises.SPAGHETTI_3D_1 || name == Noises.SPAGHETTI_3D_2) {
            // Tunnels are carved where the noise is near zero, so a stronger noise gives thinner tunnels.
            noise = amplify(noise, s.cavePercent() / 100.0F);
        }
        if (noise != cir.getReturnValue()) {
            cir.setReturnValue(noise);
        }
    }

    private static Noise stretch(Noise noise, int percent) {
        return percent == BiomeSize.DEFAULT_PERCENT ? noise : new ScaledNoise(noise, BiomeSize.DEFAULT_PERCENT / (double) percent);
    }

    private static Noise amplify(Noise noise, float amplitude) {
        return amplitude == 1.0F ? noise : new AmplifiedNoise(noise, amplitude);
    }
}
