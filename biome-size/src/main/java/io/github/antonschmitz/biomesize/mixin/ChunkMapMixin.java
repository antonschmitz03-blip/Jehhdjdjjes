package io.github.antonschmitz.biomesize.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.antonschmitz.biomesize.BiomeSize;
import io.github.antonschmitz.biomesize.ScalingFlag;
import net.minecraft.core.HolderGetter;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Marks the overworld's RandomState, so other dimensions (Nether, End, the Mining Dimension) stay as they are. */
@Mixin(ChunkMap.class)
public abstract class ChunkMapMixin {
    @WrapOperation(method = "<init>", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/levelgen/RandomState;create(Lnet/minecraft/core/HolderGetter;JLnet/minecraft/world/level/levelgen/NoiseGeneratorSettings;)Lnet/minecraft/world/level/levelgen/RandomState;"))
    private RandomState biomesize$markOverworld(HolderGetter<NormalNoise> noises, long seed, NoiseGeneratorSettings settings,
                                                Operation<RandomState> original, @Local(argsOnly = true) ServerLevel level) {
        RandomState state = original.call(noises, seed, settings);
        boolean overworld = level.dimension() == Level.OVERWORLD;
        ((ScalingFlag) (Object) state).biomesize$setScaled(overworld);
        if (overworld && !BiomeSize.active().isDefault()) {
            BiomeSize.LOGGER.info("Biome Size: scaling the overworld noises");
        }
        return state;
    }
}
