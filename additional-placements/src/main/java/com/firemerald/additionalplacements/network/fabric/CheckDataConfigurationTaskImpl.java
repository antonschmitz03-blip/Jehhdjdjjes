package com.firemerald.additionalplacements.network.fabric;

import com.firemerald.additionalplacements.network.CheckDataConfigurationTask;
import com.firemerald.additionalplacements.network.client.fabric.CheckDataClientPacketImpl;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.network.protocol.Packet;

import java.util.function.Consumer;

public class CheckDataConfigurationTaskImpl extends CheckDataConfigurationTask {
    private final PacketSender packetSender;

    protected CheckDataConfigurationTaskImpl(PacketSender packetSender) {
        this.packetSender = packetSender;
    }

    @Override
    public void start(Consumer<Packet<?>> send) {
        new CheckDataClientPacketImpl().send(packetSender);
    }
}
