package com.firemerald.additionalplacements.network;

import com.firemerald.additionalplacements.network.client.*;
import com.firemerald.additionalplacements.network.server.*;

import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.ApiStatus;

public class APNetwork {
        @ApiStatus.Internal
    public static void sendToServer(ServerPacket<?> packet) {
		com.firemerald.additionalplacements.network.fabric.APNetworkImpl.sendToServer(packet);
	}

        @ApiStatus.Internal
    public static void sendToClient(ClientPacket<?> packet, ServerPlayer player) {
		com.firemerald.additionalplacements.network.fabric.APNetworkImpl.sendToClient(packet, player);
	}
}