package io.github.antonschmitz.vplustweaks;

import io.github.antonschmitz.vplustweaks.config.FeatureEnabledCondition;
import java.util.Arrays;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VanillaPlusTweaks implements ModInitializer {
    public static final String MOD_ID = "vplustweaks";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        FeatureEnabledCondition.register();
        DoubleDoors.register();
        PetProtection.register();
        HorseStats.register();
        FastLadders.register();
        if (Boolean.getBoolean("vplustweaks.selfTest")) {
            ServerLifecycleEvents.SERVER_STARTED.register(VanillaPlusTweaks::selfTest);
        }
    }

    /** CI smoke test: force-load every server-side mixin target so a broken injection fails the run. */
    private static void selfTest(MinecraftServer server) {
        try {
            Class.forName("net.minecraft.world.inventory.AnvilMenu");
            Class.forName("net.minecraft.world.inventory.EnchantmentMenu");
            Class.forName("net.minecraft.world.level.block.FarmlandBlock");
            Class.forName("net.minecraft.world.entity.animal.equine.Horse");
            Class.forName("net.minecraft.world.entity.OwnableEntity");
            Class.forName("net.minecraft.world.level.block.LadderBlock");
            Class.forName("net.minecraft.world.level.block.grower.TreeGrower");
            Class.forName("net.minecraft.world.level.levelgen.feature.treedecorators.AlterGroundDecorator");
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
        if (TweaksConfig.enabled(TweaksConfig.FREE_ENCHANTMENT_COMBINING)) {
            checkEnchantmentCombinations(server);
        }
        LOGGER.info("vplustweaks self test passed");
    }

    private static void checkEnchantmentCombinations(MinecraftServer server) {
        HolderGetter<Enchantment> enchantments = server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> sharpness = enchantments.getOrThrow(Enchantments.SHARPNESS);
        Holder<Enchantment> smite = enchantments.getOrThrow(Enchantments.SMITE);
        Holder<Enchantment> fortune = enchantments.getOrThrow(Enchantments.FORTUNE);
        Holder<Enchantment> silkTouch = enchantments.getOrThrow(Enchantments.SILK_TOUCH);
        Holder<Enchantment> riptide = enchantments.getOrThrow(Enchantments.RIPTIDE);
        Holder<Enchantment> loyalty = enchantments.getOrThrow(Enchantments.LOYALTY);
        Holder<Enchantment> channeling = enchantments.getOrThrow(Enchantments.CHANNELING);
        boolean ok = Enchantment.areCompatible(sharpness, smite)
                && Enchantment.areCompatible(fortune, silkTouch)
                && !Enchantment.areCompatible(riptide, loyalty)
                && !Enchantment.areCompatible(channeling, riptide)
                && !Enchantment.areCompatible(sharpness, sharpness);
        if (!ok) {
            throw new IllegalStateException("Enchantment combination rules are wrong");
        }
        LOGGER.info("vplustweaks self test: enchantment combinations ok");
    }
}
