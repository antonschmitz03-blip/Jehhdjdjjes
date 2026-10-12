package com.firemerald.additionalplacements.network.client;

import com.firemerald.additionalplacements.network.APNetwork;
import com.firemerald.additionalplacements.network.APPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public interface ClientPacket<T extends FriendlyByteBuf> extends APPacket<T> {
	default void send(ServerPlayer player) {
		APNetwork.sendToClient(this, player);
	}
}
