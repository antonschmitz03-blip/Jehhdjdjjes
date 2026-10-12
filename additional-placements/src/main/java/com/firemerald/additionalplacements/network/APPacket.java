package com.firemerald.additionalplacements.network;

import com.firemerald.additionalplacements.AdditionalPlacementsMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public interface APPacket<T extends FriendlyByteBuf> extends CustomPacketPayload {
	static <T extends APPacket<?>> CustomPacketPayload.Type<T> type(String id) {
		return new CustomPacketPayload.Type<>(AdditionalPlacementsMod.rl(id));
	}

	void write(T buf);
}