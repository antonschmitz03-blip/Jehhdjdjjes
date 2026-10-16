package io.github.antonschmitz.biomesize;

/** Implemented by RandomState (through a mixin): whether this world-gen state belongs to the overworld. */
public interface ScalingFlag {
    void biomesize$setScaled(boolean scaled);
}
