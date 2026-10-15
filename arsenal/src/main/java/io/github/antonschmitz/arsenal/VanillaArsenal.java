package io.github.antonschmitz.arsenal;

import java.util.function.Function;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VanillaArsenal implements ModInitializer {
    public static final String MOD_ID = "vanillaarsenal";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Item SLING;
    public static Item GALE_BOW;

    @Override
    public void onInitialize() {
        SLING = registerItem("sling", properties -> new SlingItem(properties
                .stacksTo(1)
                .durability(250)
                .enchantable(1)
                .repairable(Items.LEATHER)));

        GALE_BOW = registerItem("gale_bow", properties -> new GaleBowItem(properties
                .stacksTo(1)
                .durability(450)
                .enchantable(1)
                .repairable(Items.BREEZE_ROD)));

        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(output -> {
            output.accept(SLING);
            output.accept(GALE_BOW);
        });

        GaleMovement.register();

        if (Boolean.getBoolean("vanillaarsenal.selfTest")) {
            ServerLifecycleEvents.SERVER_STARTED.register(SelfTest::run);
        }
    }

    private static Item registerItem(String name, Function<Item.Properties, Item> factory) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id(name));
        return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(new Item.Properties().setId(key)));
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
