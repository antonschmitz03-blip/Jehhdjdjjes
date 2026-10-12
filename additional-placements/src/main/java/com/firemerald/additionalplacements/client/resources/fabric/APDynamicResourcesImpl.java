package com.firemerald.additionalplacements.client.resources.fabric;

import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.world.flag.FeatureFlagSet;

import java.util.List;

public class APDynamicResourcesImpl {
    public static Pack.Metadata getPackMetadata(Component description, PackCompatibility compatibility, FeatureFlagSet featureFlagSet, List<String> overlays) {
        return new Pack.Metadata(description, compatibility, featureFlagSet, overlays);
    }
}
