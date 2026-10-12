package com.firemerald.additionalplacements.network.server;

import com.firemerald.additionalplacements.network.client.ClientConfigurationPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.ConfigurationTask;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public interface ServerConfigurationPacket extends ServerPacket<FriendlyByteBuf> {
    @Nullable ClientConfigurationPacket handleServer(Consumer<Runnable> enqueueWork, Consumer<Component> disconnect, Consumer<ConfigurationTask.Type> finishTask);
}
