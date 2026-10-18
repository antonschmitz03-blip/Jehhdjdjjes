package io.github.antonschmitz.diamondmachines.mixin;

import io.github.antonschmitz.diamondmachines.DiamondVariant;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Lets {@code state.is(AllBlocks.CRUSHING_WHEEL)} (and the drill and saw checks) also match our diamond versions.
 * BlockStateBase inherits {@code is(T)} as a default method from TypedInstance; this adds an override with the same
 * erased signature, so vanilla behaviour is unchanged except for our blocks.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseMixin {
    @Shadow
    public abstract Block getBlock();

    public boolean is(Object block) {
        Block own = getBlock();
        return own == block || own instanceof DiamondVariant variant && variant.baseBlock() == block;
    }
}
