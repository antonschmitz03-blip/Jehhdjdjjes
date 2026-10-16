package io.github.antonschmitz.biomesize;

import net.minecraft.util.Interval;
import net.minecraft.world.level.levelgen.synth.Noise;

/** Adds a constant to a noise value, shifting where the world sits on that noise. Range is shifted too. */
public record OffsetNoise(Noise inner, float offset) implements Noise {
    @Override
    public Interval range() {
        return Interval.add(inner.range(), Interval.ofExact(offset));
    }

    @Override
    public float get(double x, double y) {
        return inner.get(x, y) + offset;
    }

    @Override
    public float get(double x, double y, double z) {
        return inner.get(x, y, z) + offset;
    }
}
