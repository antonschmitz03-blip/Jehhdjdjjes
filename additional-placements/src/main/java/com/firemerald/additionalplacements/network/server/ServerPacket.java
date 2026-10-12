package com.firemerald.additionalplacements.network.server;

import com.firemerald.additionalplacements.network.APNetwork;
import com.firemerald.additionalplacements.network.APPacket;
import net.minecraft.network.FriendlyByteBuf;

public interface ServerPacket<T extends FriendlyByteBuf> extends APPacket<T> {
    default void sendToServer() {
        APNetwork.sendToServer(this);
    }
}
