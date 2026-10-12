package com.firemerald.additionalplacements.network.client.fabric;

import com.firemerald.additionalplacements.network.client.ClientPlayPacket;
import com.firemerald.additionalplacements.network.fabric.APPacketImpl;
import com.firemerald.additionalplacements.network.server.ServerPlayPacket;
import com.firemerald.additionalplacements.network.server.fabric.ServerPlayPacketImpl;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;

public interface ClientPlayPacketImpl extends APPacketImpl<RegistryFriendlyByteBuf>, ClientPlayPacket {
    default void handleClient(ClientPlayNetworking.Context context) {
        ServerPlayPacket reply = handleClient(context.client()::execute);
        if (reply instanceof ServerPlayPacketImpl apPacket) apPacket.send(context.responseSender());
    }
}
