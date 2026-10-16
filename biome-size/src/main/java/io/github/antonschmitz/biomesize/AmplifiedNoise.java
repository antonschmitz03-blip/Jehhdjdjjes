package io.github.antonschmitz.biomesize;

import net.minecraft.util.Interval;
import net.minecraft.world.level.levelgen.synth.Noise;

/** Multiplies a noise value. The range is scaled too so the density function optimizer stays correct. */
public record AmplifiedNoise(Noise inner, float amplitude) implements Noise {
    @Override
    public Interval range() {
        return Interval.mul(inner.range(), Interval.ofExact(amplitude));
    }

    @Override
    public float get(double x, double y) {
        return inner.get(x, y) * amplitude;
    }

    @Override
    public float get(double x, double y, double z) {
        return inner.get(x, y, z) * amplitude;
    }
}
