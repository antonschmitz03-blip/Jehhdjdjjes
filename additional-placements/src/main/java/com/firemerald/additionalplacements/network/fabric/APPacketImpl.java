package com.firemerald.additionalplacements.network.fabric;

import com.firemerald.additionalplacements.network.APPacket;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.network.FriendlyByteBuf;

public interface APPacketImpl<T extends FriendlyByteBuf> extends APPacket<T> {
    default void send(PacketSender sender) {
        APNetworkImpl.send(this, sender);
    }
}
