package io.github.antonschmitz.vplustweaks;

import java.util.Arrays;
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
        PetProtection.register();
        HorseStats.register();
        FastLadders.register();
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
            if (FabricLoader.getInstance().isModLoaded("enchantinginfuser")) {
                Class<?> infuserMenu = Class.forName("fuzs.enchantinginfuser.common.world.inventory.InfuserMenu");
                // The mixin's handler is merged into the Infuser's menu class, so it only shows up if the patch was applied.
                boolean patched = Arrays.stream(infuserMenu.getDeclaredMethods()).anyMatch(method -> method.getName().contains("vplustweaks$quarterCost"));
                if (!patched) {
                    throw new IllegalStateException("The Enchanting Infuser cost patch was not applied");
                }
                LOGGER.info("vplustweaks self test: enchanting infuser mixin loaded");
            }
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
        LOGGER.info("vplustweaks self test passed");
    }
}
