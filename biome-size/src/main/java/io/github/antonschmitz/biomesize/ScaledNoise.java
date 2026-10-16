package io.github.antonschmitz.biomesize;

import net.minecraft.util.Interval;
import net.minecraft.world.level.levelgen.synth.Noise;

/**
 * Samples the wrapped noise at horizontally scaled coordinates. The coordinates already include the
 * vanilla shift, so the shift is scaled too and biome borders keep their usual wobble in blocks.
 * Vertical coordinates stay as they are. Bulk sampling falls back to Noise's default per-point loop.
 */
public record ScaledNoise(Noise inner, double factor) implements Noise {
    @Override
    public Interval range() {
        return inner.range();
    }

    @Override
    public float get(double x, double y) {
        return inner.get(x * factor, y * factor);
    }

    @Override
    public float get(double x, double y, double z) {
        return inner.get(x * factor, y, z * factor);
    }
}
