package io.github.antonschmitz.oreshrines;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OreShrines implements ModInitializer {
    public static final String MOD_ID = "oreshrines";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Block SHRINE_CORE;
    public static Item SHRINE_CORE_ITEM;
    public static BlockEntityType<ShrineBlockEntity> SHRINE_BLOCK_ENTITY;

    @Override
    public void onInitialize() {
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id("shrine_core"));
        SHRINE_CORE = Registry.register(BuiltInRegistries.BLOCK, blockKey, new ShrineCoreBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.GOLD)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops()
                .strength(5.0F, 6.0F)
                .lightLevel(state -> state.getValue(ShrineCoreBlock.LIT) ? 15 : 7)
                .noOcclusion()
                .setId(blockKey)));
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id("shrine_core"));
        SHRINE_CORE_ITEM = Registry.register(BuiltInRegistries.ITEM, itemKey,
                new BlockItem(SHRINE_CORE, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix().rarity(Rarity.RARE)));
        SHRINE_BLOCK_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id("shrine_core"),
                FabricBlockEntityTypeBuilder.create(ShrineBlockEntity::new, SHRINE_CORE).build());

        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output -> output.accept(SHRINE_CORE_ITEM));

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> ShrineRegistry.clear());
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
