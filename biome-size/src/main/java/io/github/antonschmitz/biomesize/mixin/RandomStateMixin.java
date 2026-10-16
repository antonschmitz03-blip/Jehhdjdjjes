package io.github.antonschmitz.biomesize.mixin;

import io.github.antonschmitz.biomesize.BiomeSize;
import io.github.antonschmitz.biomesize.NoiseScaling;
import io.github.antonschmitz.biomesize.ScalingFlag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.synth.Noise;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Every noise a RandomState hands out passes through here; only the overworld's state scales them. */
@Mixin(RandomState.class)
public abstract class RandomStateMixin implements ScalingFlag {
    @Unique
    private boolean biomesize$scaled;

    @Override
    public void biomesize$setScaled(boolean scaled) {
        this.biomesize$scaled = scaled;
    }

    @Inject(method = "getOrCreateNoise", at = @At("RETURN"), cancellable = true)
    private void biomesize$scale(ResourceKey<NormalNoise> name, CallbackInfoReturnable<Noise> cir) {
        if (!biomesize$scaled) {
            return;
        }
        Noise original = cir.getReturnValue();
        Noise scaled = NoiseScaling.apply(name, original, BiomeSize.active());
        if (scaled != original) {
            cir.setReturnValue(scaled);
        }
    }
}
