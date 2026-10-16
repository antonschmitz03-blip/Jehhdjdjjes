package io.github.antonschmitz.vplustweaks.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.antonschmitz.vplustweaks.TweaksConfig;
import io.github.antonschmitz.vplustweaks.VanillaPlusTweaks;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.Identifier;

/** Lets a data file load only while a feature is switched on in config/vplustweaks.json. */
public record FeatureEnabledCondition(String feature) implements ResourceCondition {
    public static final MapCodec<FeatureEnabledCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.fieldOf("feature").forGetter(FeatureEnabledCondition::feature)
    ).apply(instance, FeatureEnabledCondition::new));

    public static final ResourceConditionType<FeatureEnabledCondition> TYPE =
            ResourceConditionType.create(Identifier.fromNamespaceAndPath(VanillaPlusTweaks.MOD_ID, "feature_enabled"), CODEC);

    public static void register() {
        ResourceConditions.register(TYPE);
    }

    @Override
    public ResourceConditionType<?> getType() {
        return TYPE;
    }

    @Override
    public boolean test(RegistryOps.RegistryInfoLookup registryInfo) {
        return TweaksConfig.enabled(feature);
    }
}
