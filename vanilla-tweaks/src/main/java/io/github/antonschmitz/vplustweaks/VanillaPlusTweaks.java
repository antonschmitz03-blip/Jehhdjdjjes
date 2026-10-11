package io.github.antonschmitz.vplustweaks;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VanillaPlusTweaks implements ModInitializer {
    public static final String MOD_ID = "vplustweaks";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        DoubleDoors.register();

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
            if (FabricLoader.getInstance().isModLoaded("enchantinginfuser")) {
                Class.forName("fuzs.enchantinginfuser.common.world.inventory.InfuserMenu");
                LOGGER.info("vplustweaks self test: enchanting infuser mixin loaded");
            }
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
        LOGGER.info("vplustweaks self test passed");
    }
}
