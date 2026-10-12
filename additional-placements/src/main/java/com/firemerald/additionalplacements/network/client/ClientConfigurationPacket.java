package com.firemerald.additionalplacements.network.client;

import com.firemerald.additionalplacements.network.server.ServerConfigurationPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public interface ClientConfigurationPacket extends ClientPacket<FriendlyByteBuf> {
    @Nullable ServerConfigurationPacket handleClient(Consumer<Runnable> enqueueWork, Consumer<Component> disconnect);
}
