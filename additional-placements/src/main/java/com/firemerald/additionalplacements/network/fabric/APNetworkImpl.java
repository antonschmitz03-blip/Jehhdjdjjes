package com.firemerald.additionalplacements.network.fabric;

import java.util.function.Function;

import com.firemerald.additionalplacements.network.APStreamCodec;
import com.firemerald.additionalplacements.network.client.*;
import com.firemerald.additionalplacements.network.client.fabric.CheckDataClientPacketImpl;
import com.firemerald.additionalplacements.network.client.fabric.ClientConfigurationPacketImpl;
import com.firemerald.additionalplacements.network.client.fabric.ClientPlayPacketImpl;
import com.firemerald.additionalplacements.network.client.fabric.ConfigurationCheckFailedPacketImpl;
import com.firemerald.additionalplacements.network.server.*;

import com.firemerald.additionalplacements.network.server.fabric.CheckDataServerPacketImpl;
import com.firemerald.additionalplacements.network.server.fabric.ServerConfigurationPacketImpl;
import com.firemerald.additionalplacements.network.server.fabric.ServerPlayPacketImpl;
import com.firemerald.additionalplacements.network.server.fabric.SetPlacementTogglePacketImpl;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.*;
import net.fabricmc.fabric.impl.networking.server.ServerConfigurationNetworkAddon;
import net.fabricmc.fabric.impl.networking.server.ServerNetworkingImpl;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public class APNetworkImpl {
    public static void register() {
        registerServerPlayPacket(SetPlacementTogglePacketImpl.TYPE, SetPlacementTogglePacketImpl::new);
        registerClientConfigurationPacket(CheckDataClientPacketImpl.TYPE, CheckDataClientPacketImpl::new);
        registerServerConfigurationPacket(CheckDataServerPacketImpl.TYPE, CheckDataServerPacketImpl::new);
        registerClientConfigurationPacket(ConfigurationCheckFailedPacketImpl.TYPE, ConfigurationCheckFailedPacketImpl::new);

        ServerConfigurationConnectionEvents.CONFIGURE.register((handler, server) -> {
            final ServerConfigurationNetworkAddon addon = ServerNetworkingImpl.getAddon(handler);
            handler.addTask(new CheckDataConfigurationTaskImpl(addon));
        });
    }

    public static <T extends ClientPlayPacketImpl> void registerClientPlayPacket(CustomPacketPayload.Type<T> type, Function<RegistryFriendlyByteBuf, T> fromBuffer) {
        PayloadTypeRegistry.clientboundPlay().register(type, new APStreamCodec<>(fromBuffer));
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) ClientPlayNetworking.registerGlobalReceiver(type, ClientPlayPacketImpl::handleClient);
    }

    public static <T extends ServerPlayPacketImpl> void registerServerPlayPacket(CustomPacketPayload.Type<T> type, Function<RegistryFriendlyByteBuf, T> fromBuffer) {
        PayloadTypeRegistry.serverboundPlay().register(type, new APStreamCodec<>(fromBuffer));
        ServerPlayNetworking.registerGlobalReceiver(type, ServerPlayPacketImpl::handleServer);
    }

    public static <T extends ClientConfigurationPacketImpl> void registerClientConfigurationPacket(CustomPacketPayload.Type<T> type, Function<FriendlyByteBuf, T> fromBuffer) {
        PayloadTypeRegistry.clientboundConfiguration().register(type, new APStreamCodec<>(fromBuffer));
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) ClientConfigurationNetworking.registerGlobalReceiver(type, ClientConfigurationPacketImpl::handleClient);
    }

    public static <T extends ServerConfigurationPacketImpl> void registerServerConfigurationPacket(CustomPacketPayload.Type<T> type, Function<FriendlyByteBuf, T> fromBuffer) {
        PayloadTypeRegistry.serverboundConfiguration().register(type, new APStreamCodec<>(fromBuffer));
        ServerConfigurationNetworking.registerGlobalReceiver(type, ServerConfigurationPacketImpl::handleServer);
    }

    public static <T extends APPacketImpl<?>> void send(T packet, PacketSender sender) {
        sender.sendPacket(packet);
    }

    public static void sendToServer(ServerPacket<?> packet) {
        assert packet instanceof APPacketImpl;
        APPacketImpl<?> apPacket = (APPacketImpl<?>) packet;
        ClientPlayNetworking.send(apPacket);
    }

    public static void sendToClient(ClientPacket<?> packet, ServerPlayer player) {
        assert packet instanceof APPacketImpl;
        APPacketImpl<?> apPacket = (APPacketImpl<?>) packet;
        ServerPlayNetworking.send(player, apPacket);
    }
}