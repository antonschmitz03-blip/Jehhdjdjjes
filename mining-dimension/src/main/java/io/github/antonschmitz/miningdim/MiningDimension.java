package io.github.antonschmitz.miningdim;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.object.builder.v1.world.poi.PoiHelper;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MiningDimension implements ModInitializer {
    public static final String MOD_ID = "miningdim";
    static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final ResourceKey<Level> MINING = ResourceKey.create(Registries.DIMENSION, id("mining"));
    public static final ResourceKey<PoiType> PORTAL_POI = ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, id("mining_portal"));
    public static final ResourceKey<Block> PORTAL_KEY = ResourceKey.create(Registries.BLOCK, id("mining_portal"));
    public static final Block PORTAL = Registry.register(BuiltInRegistries.BLOCK, PORTAL_KEY, new MiningPortalBlock(BlockBehaviour.Properties.of()
            .setId(PORTAL_KEY)
            .noCollision()
            .strength(-1.0F)
            .sound(SoundType.GLASS)
            .lightLevel(state -> 11)
            .pushReaction(PushReaction.BLOCK)
            .noLootTable()));

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        PoiHelper.register(PORTAL_POI.identifier(), 0, 1, PORTAL);
        PortalLighting.register();

        if (Boolean.getBoolean("miningdim.selfTest")) {
            ServerLifecycleEvents.SERVER_STARTED.register(SelfTest::run);
        }
    }
}
