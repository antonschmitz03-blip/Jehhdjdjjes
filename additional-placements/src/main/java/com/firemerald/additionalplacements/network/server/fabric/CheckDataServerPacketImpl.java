package com.firemerald.additionalplacements.network.server.fabric;

import com.firemerald.additionalplacements.network.APPacket;
import com.firemerald.additionalplacements.network.server.CheckDataServerPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.Map;

public class CheckDataServerPacketImpl extends CheckDataServerPacket implements ServerConfigurationPacketImpl {
    public static final CustomPacketPayload.Type<CheckDataServerPacketImpl> TYPE = APPacket.type("check_data_server");

    public static CheckDataServerPacket of(Map<Identifier, CompoundTag> serverData) {
        return new CheckDataServerPacketImpl(serverData);
    }

    public CheckDataServerPacketImpl(Map<Identifier, CompoundTag> serverData) {
        super(serverData);
    }

    public CheckDataServerPacketImpl(FriendlyByteBuf buf) {
        super(buf);
    }

    @Override
    public Type<CheckDataServerPacketImpl> type() {
        return TYPE;
    }
}
