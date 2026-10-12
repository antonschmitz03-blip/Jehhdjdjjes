package com.firemerald.additionalplacements.client.models;

import com.firemerald.additionalplacements.block.AdditionalPlacementBlock;
import com.firemerald.additionalplacements.client.models.definitions.StateModelDefinition;
import com.firemerald.additionalplacements.client.models.retextured.BakedRetexturedPlacementModel;
import com.firemerald.additionalplacements.client.models.rotated.BakedRotatedPlacementModel;
import com.firemerald.additionalplacements.util.BlockRotation;
import com.mojang.math.Quadrant;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.SingleVariant;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public final class UnbakedDynamicModel implements BlockStateModel.UnbakedRoot {
    private final AdditionalPlacementBlock<?> block;

    public UnbakedDynamicModel(AdditionalPlacementBlock<?> block) {
        this.block = block;
    }

    @Override
    @NotNull
    public BlockStateModel bake(BlockState state, ModelBaker baker) {
        if (block.rotatesModel(state)) {
            BlockState theirModelState = block.getModelState(state);
            BlockRotation modelRotation = block.getRotation(state);
            boolean rotatesTexture = block.rotatesTexture(state);
            return BakedRotatedPlacementModel.of(theirModelState, modelRotation, rotatesTexture);
        } else {
            StateModelDefinition modelDefinition = block.getModelDefinition(state);
            OurModelKey operationKey = new OurModelKey(modelDefinition, block.getBaseModelPrefix());
            BlockState theirModelState = block.getModelState(state);
            return BakedRetexturedPlacementModel.of(baker.compute(operationKey), theirModelState);
        }
    }

    @Override
    @NotNull
    public Object visualEqualityGroup(BlockState state) {
        return this;
    }

    @Override
    public void resolveDependencies(Resolver resolver) {
        Identifier rootFolder = block.getBaseModelPrefix();
        for (String model : block.getAllModels()) resolver.markDependency(rootFolder.withSuffix(model));
    }

    public record OurModelKey(StateModelDefinition modelDefinition, Identifier baseModelPrefix) implements ModelBaker.SharedOperationKey<BlockStateModel> {
        @Override
        public BlockStateModel compute(ModelBaker baker) {
            return new SingleVariant(SimpleModelWrapper.bake(
                    baker,
                    modelDefinition.location(baseModelPrefix),
                    BlockModelRotation.get(Quadrant.fromXYAngles(modelDefinition.xRotation(), modelDefinition.yRotation())).withUvLock()
            ));
        }
    }
}
