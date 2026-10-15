package io.github.antonschmitz.miningdim;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityType;
import net.fabricmc.fabric.api.object.builder.v1.world.poi.PoiHelper;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
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
            .noLootTable()));

    public static final ResourceKey<EntityType<?>> PEBBLIN_KEY = ResourceKey.create(Registries.ENTITY_TYPE, id("pebblin"));
    public static final EntityType<Pebblin> PEBBLIN = Registry.register(BuiltInRegistries.ENTITY_TYPE, PEBBLIN_KEY,
            FabricEntityType.Builder.createMob(Pebblin::new, MobCategory.CREATURE, mob -> mob
                            .defaultAttributes(Pebblin::createAttributes)
                            .spawnPlacement(SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Pebblin::checkPebblinSpawnRules))
                    .sized(0.6F, 0.55F)
                    .eyeHeight(0.4F)
                    .clientTrackingRange(8)
                    .build(PEBBLIN_KEY));

    public static final ResourceKey<Item> PEBBLIN_SPAWN_EGG_KEY = ResourceKey.create(Registries.ITEM, id("pebblin_spawn_egg"));
    public static final Item PEBBLIN_SPAWN_EGG = Registry.register(BuiltInRegistries.ITEM, PEBBLIN_SPAWN_EGG_KEY,
            new SpawnEggItem(new Item.Properties().spawnEgg(PEBBLIN).setId(PEBBLIN_SPAWN_EGG_KEY)));

    public static final ResourceKey<EntityType<?>> KNOCKER_KEY = ResourceKey.create(Registries.ENTITY_TYPE, id("knocker"));
    public static final EntityType<Knocker> KNOCKER = Registry.register(BuiltInRegistries.ENTITY_TYPE, KNOCKER_KEY,
            FabricEntityType.Builder.createMob(Knocker::new, MobCategory.CREATURE, mob -> mob.defaultAttributes(Knocker::createAttributes))
                    .sized(0.5F, 1.25F)
                    .eyeHeight(1.0F)
                    .clientTrackingRange(8)
                    .build(KNOCKER_KEY));

    public static final ResourceKey<EntityType<?>> KNOCKER_PEBBLE_KEY = ResourceKey.create(Registries.ENTITY_TYPE, id("knocker_pebble"));
    public static final EntityType<KnockerPebble> KNOCKER_PEBBLE = Registry.register(BuiltInRegistries.ENTITY_TYPE, KNOCKER_PEBBLE_KEY,
            EntityType.Builder.<KnockerPebble>of(KnockerPebble::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build(KNOCKER_PEBBLE_KEY));

    public static final ResourceKey<Item> KNOCKER_SPAWN_EGG_KEY = ResourceKey.create(Registries.ITEM, id("knocker_spawn_egg"));
    public static final Item KNOCKER_SPAWN_EGG = Registry.register(BuiltInRegistries.ITEM, KNOCKER_SPAWN_EGG_KEY,
            new SpawnEggItem(new Item.Properties().spawnEgg(KNOCKER).setId(KNOCKER_SPAWN_EGG_KEY)));

    public static final ResourceKey<Block> KNOCKER_DEN_KEY = ResourceKey.create(Registries.BLOCK, id("knocker_den"));
    public static final Block KNOCKER_DEN = Registry.register(BuiltInRegistries.BLOCK, KNOCKER_DEN_KEY, new KnockerDenBlock(BlockBehaviour.Properties.of()
            .setId(KNOCKER_DEN_KEY)
            .strength(0.5F)
            .sound(SoundType.WOOL)
            .noOcclusion()
            .randomTicks()));
    public static final ResourceKey<Item> KNOCKER_DEN_ITEM_KEY = ResourceKey.create(Registries.ITEM, id("knocker_den"));
    public static final Item KNOCKER_DEN_ITEM = Registry.register(BuiltInRegistries.ITEM, KNOCKER_DEN_ITEM_KEY,
            new BlockItem(KNOCKER_DEN, new Item.Properties().useBlockDescriptionPrefix().setId(KNOCKER_DEN_ITEM_KEY)));

    public static final KnockerDenFeature KNOCKER_DEN_FEATURE = Registry.register(BuiltInRegistries.FEATURE, id("knocker_den"),
            new KnockerDenFeature(NoneFeatureConfiguration.CODEC));

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        PoiHelper.register(PORTAL_POI.identifier(), 0, 1, PORTAL);
        PortalLighting.register();
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(output -> {
            output.accept(PEBBLIN_SPAWN_EGG);
            output.accept(KNOCKER_SPAWN_EGG);
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output -> output.accept(KNOCKER_DEN_ITEM));

        if (Boolean.getBoolean("miningdim.selfTest")) {
            ServerLifecycleEvents.SERVER_STARTED.register(SelfTest::run);
        }
    }
}
