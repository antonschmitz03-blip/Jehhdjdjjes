package com.firemerald.additionalplacements.network.server.fabric;

import com.firemerald.additionalplacements.network.client.ClientPlayPacket;
import com.firemerald.additionalplacements.network.client.fabric.ClientPlayPacketImpl;
import com.firemerald.additionalplacements.network.fabric.APPacketImpl;
import com.firemerald.additionalplacements.network.server.ServerPlayPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;

public interface ServerPlayPacketImpl extends APPacketImpl<RegistryFriendlyByteBuf>, ServerPlayPacket {
    default void handleServer(ServerPlayNetworking.Context context) {
        ClientPlayPacket reply = handleServer(context.player(), context.server()::execute);
        if (reply instanceof ClientPlayPacketImpl apPacket) apPacket.send(context.responseSender());
    }
}
