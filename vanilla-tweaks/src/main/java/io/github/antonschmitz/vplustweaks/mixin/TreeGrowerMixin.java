package io.github.antonschmitz.vplustweaks.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.antonschmitz.vplustweaks.LargeTrees;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;

/** Marks the time while a sapling grows into a tree, so {@link AlterGroundDecoratorMixin} can tell it from world generation. */
@Mixin(TreeGrower.class)
public abstract class TreeGrowerMixin {
    @WrapMethod(method = "growTree")
    private boolean vplustweaks$markSaplingGrowth(
            ServerLevel level, ChunkGenerator generator, BlockPos pos, BlockState state, RandomSource random,
            Operation<Boolean> original) {
        LargeTrees.GROWING_FROM_SAPLING.set(true);
        try {
            return original.call(level, generator, pos, state, random);
        } finally {
            LargeTrees.GROWING_FROM_SAPLING.set(false);
        }
    }
}
