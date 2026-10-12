package com.firemerald.additionalplacements.network.client;

import com.firemerald.additionalplacements.network.server.ServerPlayPacket;
import net.minecraft.network.RegistryFriendlyByteBuf;

import java.util.function.Consumer;

public interface ClientPlayPacket extends ClientPacket<RegistryFriendlyByteBuf> {
	ServerPlayPacket handleClient(Consumer<Runnable> enqueueWork);
}
