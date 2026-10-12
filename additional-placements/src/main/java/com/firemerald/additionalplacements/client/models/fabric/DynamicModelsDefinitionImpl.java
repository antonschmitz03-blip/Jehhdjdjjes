package com.firemerald.additionalplacements.client.models.fabric;

import com.firemerald.additionalplacements.client.models.DynamicModelsDefinition;
import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public class DynamicModelsDefinitionImpl extends DynamicModelsDefinition {
    public static final DynamicModelsDefinitionImpl INSTANCE = new DynamicModelsDefinitionImpl();
    public static final MapCodec<DynamicModelsDefinitionImpl> CODEC = DynamicModelsDefinition.CODEC.xmap(
            definition -> (DynamicModelsDefinitionImpl) definition,
            Function.identity()
    );

    public static DynamicModelsDefinition of() {
        return INSTANCE;
    }

    private DynamicModelsDefinitionImpl() {}

    @Override
    @NotNull
    public MapCodec<DynamicModelsDefinitionImpl> codec() {
        return CODEC;
    }
}
