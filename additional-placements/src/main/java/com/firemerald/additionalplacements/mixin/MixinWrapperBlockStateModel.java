package com.firemerald.additionalplacements.mixin;

import com.firemerald.additionalplacements.client.models.WrapperBlockStateModelExtension;
import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(WrapperBlockStateModel.class)
public abstract class MixinWrapperBlockStateModel implements WrapperBlockStateModelExtension {
    @Shadow
    protected BlockStateModel wrapped;

    @Override
    public BlockStateModel additionalplacements$wrapped() {
        return wrapped;
    }
}
