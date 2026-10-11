package io.github.antonschmitz.tbaddons;

import com.tiviacz.travelersbackpack.init.ModItemGroups;
import io.github.antonschmitz.tbaddons.item.StackUpgradeItem;
import io.github.antonschmitz.tbaddons.item.ToolSwapperUpgradeItem;
import io.github.antonschmitz.tbaddons.upgrade.StackUpgrade;
import java.util.function.Function;
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
    public static Item IRON_STACK_UPGRADE;
    public static Item GOLD_STACK_UPGRADE;
    public static Item DIAMOND_STACK_UPGRADE;

    @Override
    public void onInitialize() {
        TOOL_SWAPPER_UPGRADE = register("tool_swapper_upgrade", properties -> new ToolSwapperUpgradeItem(properties));
        IRON_STACK_UPGRADE = register("iron_stack_upgrade",
                properties -> new StackUpgradeItem(properties, "iron_stack_upgrade", 2, StackUpgrade.Iron.class, StackUpgrade.Iron::new));
        GOLD_STACK_UPGRADE = register("gold_stack_upgrade",
                properties -> new StackUpgradeItem(properties, "gold_stack_upgrade", 4, StackUpgrade.Gold.class, StackUpgrade.Gold::new));
        DIAMOND_STACK_UPGRADE = register("diamond_stack_upgrade",
                properties -> new StackUpgradeItem(properties, "diamond_stack_upgrade", 8, StackUpgrade.Diamond.class, StackUpgrade.Diamond::new));

        CreativeModeTabEvents.modifyOutputEvent(ModItemGroups.TRAVELERS_BACKPACK).register(output -> {
            output.accept(TOOL_SWAPPER_UPGRADE);
            output.accept(IRON_STACK_UPGRADE);
            output.accept(GOLD_STACK_UPGRADE);
            output.accept(DIAMOND_STACK_UPGRADE);
        });

        ToolSwapHandler.register();
    }

    private static Item register(String name, Function<Item.Properties, Item> factory) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id(name));
        return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(new Item.Properties().stacksTo(16).setId(key)));
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
