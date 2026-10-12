package com.firemerald.additionalplacements.network.server;

import com.firemerald.additionalplacements.network.client.ClientPlayPacket;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Consumer;

public interface ServerPlayPacket extends ServerPacket<RegistryFriendlyByteBuf> {
    ClientPlayPacket handleServer(ServerPlayer player, Consumer<Runnable> enqueueWork);
}
