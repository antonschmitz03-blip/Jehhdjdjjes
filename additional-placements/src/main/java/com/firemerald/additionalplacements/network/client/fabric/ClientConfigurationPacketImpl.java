package com.firemerald.additionalplacements.network.client.fabric;

import com.firemerald.additionalplacements.network.client.ClientConfigurationPacket;
import com.firemerald.additionalplacements.network.fabric.APPacketImpl;
import com.firemerald.additionalplacements.network.server.ServerConfigurationPacket;
import com.firemerald.additionalplacements.network.server.fabric.ServerConfigurationPacketImpl;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.minecraft.network.FriendlyByteBuf;

public interface ClientConfigurationPacketImpl extends APPacketImpl<FriendlyByteBuf>, ClientConfigurationPacket {
    default void handleClient(ClientConfigurationNetworking.Context context) {
        ServerConfigurationPacket reply = handleClient(context.client()::execute, context.responseSender()::disconnect);
        if (reply instanceof ServerConfigurationPacketImpl apPacket) apPacket.send(context.responseSender());
    }
}
