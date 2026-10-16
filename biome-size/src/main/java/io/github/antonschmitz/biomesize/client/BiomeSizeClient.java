package io.github.antonschmitz.biomesize.client;

import io.github.antonschmitz.biomesize.BiomeSize;

/** Slider values of the Create World screen that is currently open. */
public final class BiomeSizeClient {
    public static int biomePercent = BiomeSize.DEFAULT_PERCENT;
    public static int terrainPercent = BiomeSize.DEFAULT_PERCENT;
    public static int dramaticPercent = BiomeSize.DEFAULT_PERCENT;
    public static int cavePercent = BiomeSize.DEFAULT_PERCENT;

    private BiomeSizeClient() {
    }

    public static void reset() {
        biomePercent = BiomeSize.DEFAULT_PERCENT;
        terrainPercent = BiomeSize.DEFAULT_PERCENT;
        dramaticPercent = BiomeSize.DEFAULT_PERCENT;
        cavePercent = BiomeSize.DEFAULT_PERCENT;
    }

    public static BiomeSize.Settings settings() {
        return new BiomeSize.Settings(biomePercent, terrainPercent, dramaticPercent, cavePercent);
    }
}
