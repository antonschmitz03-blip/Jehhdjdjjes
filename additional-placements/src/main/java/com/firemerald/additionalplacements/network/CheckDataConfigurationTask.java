package com.firemerald.additionalplacements.network;

import com.firemerald.additionalplacements.AdditionalPlacementsMod;
import net.minecraft.server.network.ConfigurationTask;
import org.jetbrains.annotations.NotNull;

public abstract class CheckDataConfigurationTask implements ConfigurationTask {
	public static final Type TYPE = new Type(AdditionalPlacementsMod.MOD_ID + ":configuration_checks");

    @Override
    public @NotNull Type type() {
        return TYPE;
    }
}
