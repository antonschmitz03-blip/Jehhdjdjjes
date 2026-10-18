package io.github.antonschmitz.diamondmachines;

import com.zurrtum.create.AllCreativeModeTabs;
import com.zurrtum.create.AllItems;
import com.zurrtum.create.api.behaviour.movement.MovementBehaviour;
import com.zurrtum.create.api.stress.BlockStressValues;
import com.zurrtum.create.foundation.blockEntity.behaviour.CachedInventoryBehaviour;
import java.util.function.Function;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.MapColor;

public class DiamondMachines implements ModInitializer {
    public static final String MOD_ID = "diamondmachines";

    /** Diamond drills and saws break blocks this many times as fast as Create's at the same speed. */
    public static final float BREAK_SPEED_MULTIPLIER = 3;
    /** Diamond saws cut items on belts this many times as fast. */
    public static final int SAW_PROCESSING_MULTIPLIER = 2;
    /** A pair of diamond crushing wheels crushes this many times as fast. */
    public static final float CRUSHING_SPEED_MULTIPLIER = 2;
    /** Chance for each crushed output item to come out twice. */
    public static final float CRUSHING_BONUS_CHANCE = 0.5f;

    public static DiamondDrillBlock DRILL;
    public static DiamondSawBlock SAW;
    public static DiamondCrushingWheelBlock CRUSHING_WHEEL;
    public static Item DRILL_ITEM;
    public static Item SAW_ITEM;
    public static Item CRUSHING_WHEEL_ITEM;
    public static BlockEntityType<DiamondDrillBlockEntity> DRILL_BE;
    public static BlockEntityType<DiamondSawBlockEntity> SAW_BE;
    public static BlockEntityType<DiamondCrushingWheelBlockEntity> CRUSHING_WHEEL_BE;
    public static DiamondDrillMovementBehaviour DRILL_MOVEMENT;
    public static DiamondSawMovementBehaviour SAW_MOVEMENT;

    @Override
    public void onInitialize() {
        // Same properties as Create's machines; diamond makes them a little tougher to break.
        DRILL = block("diamond_drill", DiamondDrillBlock::new,
                Properties.ofFullCopy(Blocks.ANDESITE).mapColor(MapColor.DIAMOND).strength(2.5f, 6f));
        SAW = block("diamond_saw", DiamondSawBlock::new,
                Properties.ofFullCopy(Blocks.ANDESITE).mapColor(MapColor.DIAMOND).strength(2.5f, 6f));
        CRUSHING_WHEEL = block("diamond_crushing_wheel", DiamondCrushingWheelBlock::new,
                Properties.ofFullCopy(Blocks.ANDESITE).mapColor(MapColor.DIAMOND).strength(2.5f, 6f).noOcclusion());

        DRILL_ITEM = item("diamond_drill", DRILL);
        SAW_ITEM = item("diamond_saw", SAW);
        CRUSHING_WHEEL_ITEM = item("diamond_crushing_wheel", CRUSHING_WHEEL);

        DRILL_BE = blockEntity("diamond_drill", DiamondDrillBlockEntity::new, DRILL);
        SAW_BE = blockEntity("diamond_saw", DiamondSawBlockEntity::new, SAW);
        CRUSHING_WHEEL_BE = blockEntity("diamond_crushing_wheel", DiamondCrushingWheelBlockEntity::new, CRUSHING_WHEEL);

        // Stress impact per RPM (Create: drill 4, saw 4, crushing wheel 8).
        BlockStressValues.IMPACTS.register(DRILL, () -> 6.0);
        BlockStressValues.IMPACTS.register(SAW, () -> 6.0);
        BlockStressValues.IMPACTS.register(CRUSHING_WHEEL, () -> 12.0);

        DRILL_MOVEMENT = new DiamondDrillMovementBehaviour();
        SAW_MOVEMENT = new DiamondSawMovementBehaviour();
        MovementBehaviour.REGISTRY.register(DRILL, DRILL_MOVEMENT);
        MovementBehaviour.REGISTRY.register(SAW, SAW_MOVEMENT);

        // Funnels, hoppers and chutes find the saw's inventory by block entity type, so offer it for ours too.
        // The behaviour itself is added by Create, because the saw's constructor passes Create's saw type.
        ItemStorage.SIDED.registerForBlockEntity(CachedInventoryBehaviour::get, SAW_BE);

        CreativeModeTabEvents.modifyOutputEvent(AllCreativeModeTabs.BASE_GROUP).register(output -> {
            output.insertAfter(AllItems.MECHANICAL_DRILL, DRILL_ITEM);
            output.insertAfter(AllItems.MECHANICAL_SAW, SAW_ITEM);
            output.insertAfter(AllItems.CRUSHING_WHEEL, CRUSHING_WHEEL_ITEM);
        });
    }

    private static <T extends Block> T block(String name, Function<Properties, T> factory, Properties properties) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id(name));
        return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key)));
    }

    private static Item item(String name, Block block) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id(name));
        return Registry.register(BuiltInRegistries.ITEM, key,
                new BlockItem(block, new Item.Properties().setId(key).useBlockDescriptionPrefix()));
    }

    private static <T extends BlockEntity> BlockEntityType<T> blockEntity(
            String name, FabricBlockEntityTypeBuilder.Factory<T> factory, Block block) {
        return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id(name),
                FabricBlockEntityTypeBuilder.create(factory, block).build());
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
