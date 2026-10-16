package io.github.antonschmitz.biomesize;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.synth.Noise;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

/**
 * Wraps the world-gen noises of an overworld RandomState. Stretching the climate noises changes biome
 * size; stretching continentalness and erosion changes land shape. Weirdness (rivers, peaks) is left
 * alone, like vanilla Large Biomes does.
 */
public final class NoiseScaling {
    /** How far the erosion noise moves per 100% of the Mountains slider. */
    private static final float MOUNTAIN_SHIFT = 0.3F;
    /** How far the aquifer floodedness noise moves per 100% of the Underground Water slider. */
    private static final float WATER_SHIFT = 0.4F;

    private NoiseScaling() {
    }

    public static Noise apply(ResourceKey<NormalNoise> name, Noise noise, BiomeSize.Settings s) {
        if (s.isDefault()) {
            return noise;
        }
        if (name == Noises.TEMPERATURE || name == Noises.VEGETATION
                || name == Noises.TEMPERATURE_LARGE || name == Noises.VEGETATION_LARGE) {
            return stretch(noise, s.biomePercent());
        } else if (name == Noises.CONTINENTALNESS || name == Noises.EROSION
                || name == Noises.CONTINENTALNESS_LARGE || name == Noises.EROSION_LARGE) {
            noise = stretch(noise, s.terrainPercent());
            if (name == Noises.EROSION || name == Noises.EROSION_LARGE) {
                // Low erosion is where the tall mountains are: shifting it down makes them cover more of the world.
                float shift = -MOUNTAIN_SHIFT * (s.mountainPercent() - BiomeSize.DEFAULT_PERCENT) / 100.0F;
                noise = shift == 0.0F ? noise : new OffsetNoise(noise, shift);
            }
            return noise;
        } else if (name == Noises.AQUIFER_FLUID_LEVEL_FLOODEDNESS) {
            // Higher floodedness fills more underground pools with water.
            float shift = WATER_SHIFT * (s.waterPercent() - BiomeSize.DEFAULT_PERCENT) / 100.0F;
            return shift == 0.0F ? noise : new OffsetNoise(noise, shift);
        } else if (name == Noises.AQUIFER_LAVA) {
            // Deep pools turn to lava where this is above a threshold, so a stronger noise gives more lava.
            return amplify(noise, s.lavaPercent() / 100.0F);
        } else if (name == Noises.JAGGED) {
            // Strength of mountain peaks and cliffs.
            return amplify(noise, s.dramaticPercent() / 100.0F);
        } else if (name == Noises.CAVE_CHEESE) {
            // Big open caves: a stronger noise reaches the cave threshold more often.
            return amplify(noise, s.cavePercent() / 100.0F);
        } else if (name == Noises.CAVE_LAYER) {
            // The layered caverns are carved where this is small, so a weaker noise means more of them.
            return amplify(noise, 100.0F / s.cavePercent());
        } else if (name == Noises.SPAGHETTI_2D || name == Noises.SPAGHETTI_3D_1 || name == Noises.SPAGHETTI_3D_2) {
            // Tunnels are carved where the noise is near zero, so a stronger noise gives thinner tunnels.
            return amplify(noise, s.cavePercent() / 100.0F);
        }
        return noise;
    }

    private static Noise stretch(Noise noise, int percent) {
        return percent == BiomeSize.DEFAULT_PERCENT ? noise : new ScaledNoise(noise, BiomeSize.DEFAULT_PERCENT / (double) percent);
    }

    private static Noise amplify(Noise noise, float amplitude) {
        return amplitude == 1.0F ? noise : new AmplifiedNoise(noise, amplitude);
    }
}
