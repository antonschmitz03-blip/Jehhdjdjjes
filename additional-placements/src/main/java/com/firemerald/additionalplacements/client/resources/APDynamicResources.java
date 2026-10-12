package com.firemerald.additionalplacements.client.resources;

import java.io.InputStream;
import java.util.*;

import com.firemerald.additionalplacements.AdditionalPlacementsMod;
import com.firemerald.additionalplacements.block.*;
import com.firemerald.additionalplacements.generation.Registration;

import com.firemerald.additionalplacements.util.PlatformUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackMetadataResources;
import net.minecraft.server.packs.PackResources;
import java.util.stream.Stream;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.NotNull;

public class APDynamicResources implements PackResources {
    static {
        PlatformUtils.checkIsClient();
    }

    public static final PackLocationInfo LOCATION =  new PackLocationInfo(
            "Additional Placements Dynamic Resources",
            Component.literal("Additional Placements dynamic resources"),
            PackSource.BUILT_IN,
            Optional.empty());

    public static final Pack PACK = new Pack(
            LOCATION,
            new Pack.ResourcesSupplier() {
                @Override
                public @NotNull PackMetadataResources openMetadata(PackLocationInfo location) {
                    return new APDynamicResources();
                }

                @Override
                public @NotNull Stream<PackResources> openResources(PackLocationInfo location, Pack.Metadata metadata) {
                    return Stream.of(new APDynamicResources());
                }
            },
            getPackMetadata(
                    Component.literal("description"),
                    PackCompatibility.COMPATIBLE,
                    FeatureFlagSet.of(),
                    List.of()),
            new PackSelectionConfig(
                    true,
                    Pack.Position.BOTTOM,
                    true)
    );

        public static Pack.Metadata getPackMetadata(Component description, PackCompatibility compatibility, FeatureFlagSet featureFlagSet, List<String> overlays) {
		return com.firemerald.additionalplacements.client.resources.fabric.APDynamicResourcesImpl.getPackMetadata(description, compatibility, featureFlagSet, overlays);
	}

    @Override
    public IoSupplier<InputStream> getRootResource(String @NotNull ... strings) {
        return null;
    }

    @Override
    public IoSupplier<InputStream> getResource(@NotNull PackType packType, @NotNull Identifier resourceLocation) {
        if (packType != PackType.CLIENT_RESOURCES) return null;
        else if (!resourceLocation.getNamespace().equals(AdditionalPlacementsMod.MOD_ID)) return null;
        else if (!resourceLocation.getPath().endsWith(".json")) return null;
        else if (resourceLocation.getPath().startsWith("blockstates/")) { //blockstate json
            String blockName = resourceLocation.getPath().substring(12, resourceLocation.getPath().length() - 5);
            Optional<Holder.Reference<Block>> block = BuiltInRegistries.BLOCK.get(AdditionalPlacementsMod.rl(blockName));
            if (block.isPresent() && block.get().value() instanceof AdditionalPlacementBlock<?>) return DynamicBlockstateJson.INSTANCE;
            else return null;
        }
        else return null;
    }

    @SuppressWarnings("unchecked")
    private <T extends Comparable<T>> BlockState set(BlockState state, Property<?> prop, Comparable<?> val) {
        return state.setValue((Property<T>) prop, (T) val);
    }

    @Override
    public void listResources(@NotNull PackType packType, @NotNull String domain, @NotNull String path, @NotNull ResourceOutput resourceOutput) {
        if (packType == PackType.CLIENT_RESOURCES && AdditionalPlacementsMod.MOD_ID.equals(domain)) {
            if ("blockstates".equals(path)) {
                Registration.forEach(type -> type.forEachCreated(entry -> {
                    Identifier id = entry.newId();
                    resourceOutput.accept(
                            AdditionalPlacementsMod.rl("blockstates/" + id.getPath() + ".json"),
                            DynamicBlockstateJson.INSTANCE);
                }));
            }
        }
    }

    @Override
    public @NotNull Set<String> getNamespaces(@NotNull PackType packType) {
        return Collections.singleton(AdditionalPlacementsMod.MOD_ID);
    }

    @Override
    public <T> T getMetadataSection(MetadataSectionType<T> type) {
        return null;
    }

    @Override
    public @NotNull PackLocationInfo location() {
        return LOCATION;
    }

    @Override
    public @NotNull String packId() {
        return LOCATION.id();
    }

    @Override
    public void close() {}
}
