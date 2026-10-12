package com.firemerald.additionalplacements.client.fabric;

import com.firemerald.additionalplacements.client.models.IAPCustomBlockModelDefinition;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

public interface BlockStateModelDispatcherExtensions {
    @Nullable
    IAPCustomBlockModelDefinition additionalplacements$getCustomModelDefinition();

    @ApiStatus.Internal
    void additionalplacements$setCustomModelDefinition(@Nullable IAPCustomBlockModelDefinition customDefinition);
}
