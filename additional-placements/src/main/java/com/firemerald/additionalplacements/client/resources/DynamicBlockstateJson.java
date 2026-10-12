package com.firemerald.additionalplacements.client.resources;

import com.firemerald.additionalplacements.client.models.DynamicModelsDefinition;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class DynamicBlockstateJson implements IoSupplier<InputStream> {
    public static final DynamicBlockstateJson INSTANCE = new DynamicBlockstateJson();
    public static final byte[] BYTES = ("{\"neoforge:definition_type\":\"" + DynamicModelsDefinition.ID + "\"}").getBytes(StandardCharsets.UTF_8);

    private DynamicBlockstateJson() {}

    @Override
    public @NotNull InputStream get() {
        return new ByteArrayInputStream(BYTES);
    }
}