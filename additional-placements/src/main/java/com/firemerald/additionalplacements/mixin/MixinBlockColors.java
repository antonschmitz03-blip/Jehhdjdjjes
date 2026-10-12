package com.firemerald.additionalplacements.mixin;

import com.firemerald.additionalplacements.block.AdditionalPlacementBlock;
import com.firemerald.additionalplacements.client.BlockTintSourceWrapper;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;

@Mixin(BlockColors.class)
public class MixinBlockColors {
    @Shadow
    @Final
    private Map<Block, List<? extends BlockTintSource>> sources;
    @Unique
    private Set<AdditionalPlacementBlock<?>> additionalplacements$injected = new HashSet<>();

    @Inject(method = "getTintSources(Lnet/minecraft/world/level/block/state/BlockState;)Ljava/util/List;", at = @At("HEAD"))
    private void getTintSources(BlockState state, CallbackInfoReturnable<List<BlockTintSource>> cir) {
        if (state.getBlock() instanceof AdditionalPlacementBlock<?> block) {
            additionalplacements$injectTintSources(block);
        }
    }

    @Inject(method = "getColoringProperties(Lnet/minecraft/world/level/block/Block;)Ljava/util/Set;", at = @At("HEAD"))
    private void getColoringProperties(Block block, CallbackInfoReturnable<Set<Property<?>>> cir) {
        if (block instanceof AdditionalPlacementBlock<?> block2) {
            additionalplacements$injectTintSources(block2);
        }
    }

    @Unique
    private void additionalplacements$injectTintSources(AdditionalPlacementBlock<?> block) {
        if (!additionalplacements$injected.contains(block)) {
            additionalplacements$injected.add(block);
            List<? extends BlockTintSource> tintSources = sources.get(block.parentBlock);
            if (tintSources != null) {
                sources.put(block, tintSources.stream().map(tintSource -> new BlockTintSourceWrapper(block, tintSource)).toList());
            }
        }
    }
}
