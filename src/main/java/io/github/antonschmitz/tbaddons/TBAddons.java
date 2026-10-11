package io.github.antonschmitz.tbaddons;

import com.tiviacz.travelersbackpack.init.ModItemGroups;
import io.github.antonschmitz.tbaddons.item.ToolSwapperUpgradeItem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

public class TBAddons implements ModInitializer {
    public static final String MOD_ID = "tbaddons";

    public static Item TOOL_SWAPPER_UPGRADE;

    @Override
    public void onInitialize() {
        ResourceKey<Item> toolSwapperKey = ResourceKey.create(Registries.ITEM, id("tool_swapper_upgrade"));
        TOOL_SWAPPER_UPGRADE = Registry.register(
                BuiltInRegistries.ITEM,
                toolSwapperKey,
                new ToolSwapperUpgradeItem(new Item.Properties().stacksTo(16).setId(toolSwapperKey))
        );

        CreativeModeTabEvents.modifyOutputEvent(ModItemGroups.TRAVELERS_BACKPACK)
                .register(output -> output.accept(TOOL_SWAPPER_UPGRADE));

        ToolSwapHandler.register();
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
