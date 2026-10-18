package io.github.antonschmitz.oreshrines.mixin;

import io.github.antonschmitz.oreshrines.ShrineRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Ward shrines: no natural hostile spawns inside their range. Spawners and mobs that walk in are not affected. */
@Mixin(NaturalSpawner.class)
public abstract class NaturalSpawnerMixin {
    @Inject(method = "isValidSpawnPostitionForType", at = @At("HEAD"), cancellable = true)
    private static void oreshrines$ward(ServerLevel level, MobCategory category, StructureManager structureManager,
                                        ChunkGenerator generator, MobSpawnSettings.SpawnerData data,
                                        BlockPos.MutableBlockPos pos, double distance, CallbackInfoReturnable<Boolean> cir) {
        if (category == MobCategory.MONSTER && ShrineRegistry.isWarded(level, pos)) {
            cir.setReturnValue(false);
        }
    }
}
