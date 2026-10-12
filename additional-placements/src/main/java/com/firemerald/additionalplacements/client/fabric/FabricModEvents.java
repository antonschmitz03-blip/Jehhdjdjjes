package com.firemerald.additionalplacements.client.fabric;

import com.firemerald.additionalplacements.client.APClientData;
import com.firemerald.additionalplacements.client.ClientModEvents;
import com.firemerald.additionalplacements.common.CommonModEvents;
import com.firemerald.additionalplacements.util.PlatformUtils;

import com.firemerald.additionalplacements.config.APConfigs;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

public class FabricModEvents implements ClientModInitializer {
    static {
        PlatformUtils.checkIsClient();
    }

    @Override
    public void onInitializeClient() {
        ItemTooltipCallback.EVENT.register(CommonModEvents::onItemTooltip);
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> com.firemerald.additionalplacements.common.fabric.FabricModEvents.init());
        ClientTickEvents.END_CLIENT_TICK.register(FabricModEvents::onClientEndTick);
        ClientPlayConnectionEvents.JOIN.register(FabricModEvents::onServerJoined);
        KeyMappingHelper.registerKeyMapping(APClientData.AP_PLACEMENT_KEY);
    }

    public static void onServerJoined(ClientPacketListener handler, PacketSender sender, Minecraft client) {
        APClientData.setPlacementEnabledAndSynchronize(APConfigs.client().defaultPlacementLogicState.get(), APConfigs.client().loginPlacementLogicStateMessage.get());
    }

    public static void onClientEndTick(Minecraft mc) {
        ClientModEvents.onInput();
        ClientModEvents.onClientPostTick();
    }
}