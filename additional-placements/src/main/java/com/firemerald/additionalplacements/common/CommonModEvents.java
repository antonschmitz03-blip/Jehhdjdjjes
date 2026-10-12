package com.firemerald.additionalplacements.common;

import com.firemerald.additionalplacements.AdditionalPlacementsMod;
import com.firemerald.additionalplacements.block.interfaces.IPlacementBlock;
import com.firemerald.additionalplacements.commands.CommandExportTags;
import com.firemerald.additionalplacements.commands.CommandGenerateStairsDebugger;
import com.firemerald.additionalplacements.config.APConfigs;
import com.firemerald.additionalplacements.generation.Registration;
import com.google.common.base.Suppliers;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WeatheringCopper;
import net.neoforged.fml.config.ModConfig;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public class CommonModEvents {
    public static boolean misMatchedTags = false, autoGenerateFailed = false;
    protected static boolean reloadedFromChecker = false;

    public static void onItemTooltip(ItemStack stack, Item.TooltipContext context, TooltipFlag tooltipType, List<Component> lines) {
        if (stack.getItem() instanceof BlockItem) {
            Block block = ((BlockItem) stack.getItem()).getBlock();
            if (block instanceof IPlacementBlock<?> verticalBlock) {
                if (verticalBlock.additionalplacements$hasAdditionalStates()) verticalBlock.additionalplacements$appendHoverTextImpl(stack, context, lines, tooltipType);
            }
        }
    }

    public static void onRegisterCommands(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
        CommandExportTags.register(dispatcher);
        CommandGenerateStairsDebugger.register(dispatcher, registryAccess);
    }

    public static void onTagsUpdated(boolean client) {
        if (!client) {
            Registration.forEach(type -> type.onTagsUpdated(false));
            boolean fromAutoGenerate;
            if (reloadedFromChecker) {
                reloadedFromChecker = false;
                fromAutoGenerate = true;
            } else fromAutoGenerate = false;
            if (currentServer != null) possiblyCheckTags(fromAutoGenerate);
        }
        else Registration.forEach(type -> type.onTagsUpdated(true));
    }

    private static void possiblyCheckTags(boolean fromAutoGenerate) {
        misMatchedTags = false;
        autoGenerateFailed = false;
        if (APConfigs.common().checkTags.get() && APConfigs.server().checkTags.get()) {
            TagMismatchChecker.startChecker(currentServer, !fromAutoGenerate && APConfigs.common().autoRebuildTags.get() && APConfigs.server().autoRebuildTags.get(), fromAutoGenerate); //TODO halt on datapack reload
        }
    }

    private static MinecraftServer currentServer = null;

    public static void onServerStarted(MinecraftServer server) {
        currentServer = server;
        possiblyCheckTags(false);
    }

    public static void onServerStopping(MinecraftServer server) {
        currentServer = null;
        TagMismatchChecker.stopChecker();
        reloadedFromChecker = false;
    }


    public static void modifyWaxables() {
        try { //we need to do this hack because we can't have non-final static fields on interfaces, because Java doesn't let us have nice things. However, it is volatile, and should be replaced when it becomes possible.
            Class<?> clazz = Class.forName("com.google.common.base.Suppliers$NonSerializableMemoizingSupplier");
            Field delegate = clazz.getDeclaredField("delegate");
            delegate.setAccessible(true);
            Field value = clazz.getDeclaredField("value");
            value.setAccessible(true);
            Field successfullyComputedField = clazz.getDeclaredField("SUCCESSFULLY_COMPUTED");
            successfullyComputedField.setAccessible(true);
            Object successfullyComputed = successfullyComputedField.get(null);
            Function<BiMap<Block, Block>, BiMap<Block, Block>> withAdditionalStates = oldMap -> {
                BiMap<Block, Block> newMap = HashBiMap.create(oldMap);
                oldMap.forEach((b1, b2) -> {
                    if (b1 instanceof IPlacementBlock<?> p1 && b2 instanceof IPlacementBlock<?> p2) {
                        if (p1.additionalplacements$hasAdditionalStates() && p2.additionalplacements$hasAdditionalStates()) newMap.put(p1.additionalplacements$getOtherBlock(), p2.additionalplacements$getOtherBlock());
                    }
                });
                return newMap;
            };
            try {
                modifyMap(WeatheringCopper.NEXT_BY_BLOCK, WeatheringCopper.PREVIOUS_BY_BLOCK, withAdditionalStates, delegate, value, successfullyComputed);
            }
            catch (IllegalArgumentException | IllegalAccessException e) {
                AdditionalPlacementsMod.LOGGER.error("Failed to update WeatheringCopper maps, copper slabs and stairs will weather into vanilla states. Sorry.", e);
            }
        }
        catch (ClassNotFoundException | NoSuchFieldException | SecurityException | IllegalAccessException e) {
            AdditionalPlacementsMod.LOGGER.error("Failed to update WeatheringCopper maps, copper slabs and stairs will weather into vanilla states. Sorry.", e);
        }
        Supplier<BiMap<Block, Block>> waxables = HoneycombItem.WAXABLES;
        HoneycombItem.WAXABLES = Suppliers.memoize(() -> addVariants(waxables.get()));
        HoneycombItem.WAX_OFF_BY_BLOCK = Suppliers.memoize(() -> HoneycombItem.WAXABLES.get().inverse());
    }

    public static BiMap<Block, Block> addVariants(Map<Block, Block> oldMap) {
        BiMap<Block, Block> newMap = HashBiMap.create(oldMap);
        oldMap.forEach((b1, b2) -> {
            if (b1 instanceof IPlacementBlock<?> p1 && b2 instanceof IPlacementBlock<?> p2) {
                if (p1.additionalplacements$hasAdditionalStates() && p2.additionalplacements$hasAdditionalStates()) newMap.put(p1.additionalplacements$getOtherBlock(), p2.additionalplacements$getOtherBlock());
            }
        });
        return newMap;
    }

    public static <T, U> void modifyMap(Supplier<BiMap<T, U>> forwardMemoized, Supplier<BiMap<U, T>> backwardMemoized, Function<BiMap<T, U>, BiMap<T, U>> modify, Field delegate, Field value, Object successfullyComputed) throws IllegalArgumentException, IllegalAccessException {
        @SuppressWarnings("unchecked")
        com.google.common.base.Supplier<BiMap<T, U>> forwardSupplier = (com.google.common.base.Supplier<BiMap<T, U>>) delegate.get(forwardMemoized);
        if (forwardSupplier == successfullyComputed) //already computed
        {
            @SuppressWarnings("unchecked")
            BiMap<T, U> map = (BiMap<T, U>) value.get(forwardMemoized); //get existing map
            value.set(forwardMemoized, null); //clear value
            delegate.set(forwardMemoized, (com.google.common.base.Supplier<BiMap<T, U>>) () -> modify.apply(map)); //replace with supplier that modifies the existing map
        }
        else delegate.set(forwardMemoized, (com.google.common.base.Supplier<BiMap<T, U>>) () -> modify.apply(forwardSupplier.get())); //replace with supplier that modifies the result of the existing supplier
        @SuppressWarnings("unchecked")
        com.google.common.base.Supplier<BiMap<U, T>> backwardSupplier = (com.google.common.base.Supplier<BiMap<U, T>>) delegate.get(backwardMemoized);
        if (backwardSupplier == successfullyComputed) value.set(backwardMemoized, null); //clear computed value
        delegate.set(backwardMemoized, (com.google.common.base.Supplier<BiMap<U, T>>) () -> forwardMemoized.get().inverse()); //replace with supplier that gets the inverse of the forward map
    }

    public static void onConfigLoaded(ModConfig config) {
        APConfigs.onConfigLoaded(config.getSpec());
    }

    public static void onConfigReloaded(ModConfig config) {
        APConfigs.onConfigReloaded(config.getSpec());
    }
}
