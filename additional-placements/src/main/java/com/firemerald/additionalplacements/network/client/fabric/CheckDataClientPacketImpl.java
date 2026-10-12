package com.firemerald.additionalplacements.network.client.fabric;

import com.firemerald.additionalplacements.network.APPacket;
import com.firemerald.additionalplacements.network.client.CheckDataClientPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public class CheckDataClientPacketImpl extends CheckDataClientPacket implements ClientConfigurationPacketImpl {
    public static final CustomPacketPayload.Type<CheckDataClientPacketImpl> TYPE = APPacket.type("check_data");

    public static CheckDataClientPacket of() {
        return new CheckDataClientPacketImpl();
    }

    public CheckDataClientPacketImpl() {
        super();
    }

    public CheckDataClientPacketImpl(FriendlyByteBuf buf) {
        super(buf);
    }

    @Override
    public Type<CheckDataClientPacketImpl> type() {
        return TYPE;
    }
}
