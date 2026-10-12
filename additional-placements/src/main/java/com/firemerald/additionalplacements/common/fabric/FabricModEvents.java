package com.firemerald.additionalplacements.common.fabric;

import java.util.ArrayList;
import java.util.List;

import com.firemerald.additionalplacements.AdditionalPlacementsMod;
import com.firemerald.additionalplacements.common.CommonModEvents;
import com.firemerald.additionalplacements.common.TagMismatchChecker;
import com.firemerald.additionalplacements.compat.LoadedMods;
import com.firemerald.additionalplacements.network.fabric.APNetworkImpl;
import fuzs.forgeconfigapiport.fabric.api.v5.ConfigRegistry;
import fuzs.forgeconfigapiport.fabric.api.v5.ModConfigEvents;
import net.minecraft.resources.ResourceKey;
import org.apache.commons.lang3.tuple.Pair;

import com.firemerald.additionalplacements.config.APConfigs;
import com.firemerald.additionalplacements.generation.Registration;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.block.Block;

public class FabricModEvents implements ModInitializer {
    @Override
    public void onInitialize() {
        ModConfigEvents.loading(AdditionalPlacementsMod.MOD_ID).register(CommonModEvents::onConfigLoaded);
        ModConfigEvents.reloading(AdditionalPlacementsMod.MOD_ID).register(CommonModEvents::onConfigReloaded);
        APNetworkImpl.register();
        loadRegistry();
        CommandRegistrationCallback.EVENT.register(CommonModEvents::onRegisterCommands);
        CommonLifecycleEvents.TAGS_LOADED.register((registries, client) -> CommonModEvents.onTagsUpdated(client));
        ServerLifecycleEvents.SERVER_STARTED.register(FabricModEvents::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(CommonModEvents::onServerStopping);
        ServerPlayConnectionEvents.JOIN.register(FabricModEvents::onPlayerLogin);
    }

    private static void loadRegistry() {
        Registration.gatherTypes();
        APConfigs.init((type, spec) -> ConfigRegistry.INSTANCE.register(AdditionalPlacementsMod.MOD_ID, type, spec));
        List<Pair<ResourceKey<Block>, Block>> created = new ArrayList<>();
        BuiltInRegistries.BLOCK.entrySet().forEach(entry -> Registration.tryApply(entry.getValue(), entry.getKey().identifier(), (key, obj) -> created.add(Pair.of(key, obj))));
        created.forEach(pair -> Registry.register(BuiltInRegistries.BLOCK, pair.getLeft(), pair.getRight()));
        RegistryEntryAddedCallback.event(BuiltInRegistries.BLOCK).register((rawId, id, block) -> Registration.tryApply(block, id, (blockId, obj) -> Registry.register(BuiltInRegistries.BLOCK, blockId, obj)));
    }

    private static boolean hasInit = false;

    public static void init() {
        if (!hasInit) {
            LoadedMods.populate();
            CommonModEvents.modifyWaxables();
            hasInit = true;
        }
    }

    public static void onPlayerLogin(ServerGamePacketListenerImpl handler, PacketSender sender, MinecraftServer server) {
        if (CommonModEvents.misMatchedTags && TagMismatchChecker.canGenerateTags(handler.getPlayer())) handler.getPlayer().sendSystemMessage(CommonModEvents.autoGenerateFailed ? TagMismatchChecker.FAILED : TagMismatchChecker.MESSAGE);
    }

    public static void onServerStarted(MinecraftServer server) {
        init();
        CommonModEvents.onServerStarted(server);
    }
}