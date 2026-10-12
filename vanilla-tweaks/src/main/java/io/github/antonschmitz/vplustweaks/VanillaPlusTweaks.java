package io.github.antonschmitz.vplustweaks;

import io.github.antonschmitz.vplustweaks.item.GhastWhistleItem;
import java.util.function.Function;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VanillaPlusTweaks implements ModInitializer {
    public static final String MOD_ID = "vplustweaks";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Item GHAST_SPEED_UPGRADE;
    public static Item GHAST_WHISTLE;

    @Override
    public void onInitialize() {
        DoubleDoors.register();
        PetProtection.register();
        HorseStats.register();
        FastLadders.register();
        GHAST_SPEED_UPGRADE = register("ghast_speed_upgrade", Item::new);
        GHAST_WHISTLE = register("ghast_whistle", properties -> new GhastWhistleItem(properties.stacksTo(1)));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> {
            output.accept(GHAST_SPEED_UPGRADE);
            output.accept(GHAST_WHISTLE);
        });

        GhastChest.register();
        GhastFeatures.register();

        if (Boolean.getBoolean("vplustweaks.selfTest")) {
            ServerLifecycleEvents.SERVER_STARTED.register(server -> selfTest());
        }
    }

    /** CI smoke test: force-load every server-side mixin target so a broken injection fails the run. */
    private static void selfTest() {
        try {
            Class.forName("net.minecraft.world.inventory.AnvilMenu");
            Class.forName("net.minecraft.world.inventory.EnchantmentMenu");
            Class.forName("net.minecraft.world.level.block.FarmlandBlock");
            Class.forName("net.minecraft.world.entity.animal.equine.Horse");
            Class.forName("net.minecraft.world.entity.OwnableEntity");
            Class.forName("net.minecraft.world.level.block.LadderBlock");
            Class.forName("net.minecraft.world.entity.animal.happyghast.HappyGhast");
            if (FabricLoader.getInstance().isModLoaded("enchantinginfuser")) {
                Class.forName("fuzs.enchantinginfuser.common.world.inventory.InfuserMenu");
                LOGGER.info("vplustweaks self test: enchanting infuser mixin loaded");
            }
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
        LOGGER.info("vplustweaks self test passed");
    }

    private static Item register(String name, Function<Item.Properties, Item> factory) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, name));
        return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(new Item.Properties().setId(key)));
    }
}
