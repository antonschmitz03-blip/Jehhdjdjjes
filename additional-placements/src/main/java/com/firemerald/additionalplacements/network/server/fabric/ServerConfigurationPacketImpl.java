package com.firemerald.additionalplacements.network.server.fabric;

import com.firemerald.additionalplacements.network.client.ClientConfigurationPacket;
import com.firemerald.additionalplacements.network.client.fabric.ClientConfigurationPacketImpl;
import com.firemerald.additionalplacements.network.fabric.APPacketImpl;
import com.firemerald.additionalplacements.network.server.ServerConfigurationPacket;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.minecraft.network.FriendlyByteBuf;

public interface ServerConfigurationPacketImpl extends APPacketImpl<FriendlyByteBuf>, ServerConfigurationPacket {
    default void handleServer(ServerConfigurationNetworking.Context context) {
        ClientConfigurationPacket reply = handleServer(context.server()::execute, context.packetListener()::disconnect, context.packetListener()::completeTask);
        if (reply instanceof ClientConfigurationPacketImpl apPacket) apPacket.send(context.responseSender());
    }
}
