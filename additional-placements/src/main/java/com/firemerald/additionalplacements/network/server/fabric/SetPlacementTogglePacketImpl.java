package com.firemerald.additionalplacements.network.server.fabric;

import com.firemerald.additionalplacements.network.APPacket;
import com.firemerald.additionalplacements.network.server.SetPlacementTogglePacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public class SetPlacementTogglePacketImpl extends SetPlacementTogglePacket implements ServerPlayPacketImpl {
    public static final CustomPacketPayload.Type<SetPlacementTogglePacketImpl> TYPE = APPacket.type("set_placement_toggle");

    public static SetPlacementTogglePacket of(boolean state) {
        return new SetPlacementTogglePacketImpl(state);
    }

    public SetPlacementTogglePacketImpl(boolean state) {
        super(state);
    }

    public SetPlacementTogglePacketImpl(FriendlyByteBuf buf) {
        super(buf);
    }

    @Override
    public Type<SetPlacementTogglePacketImpl> type() {
        return TYPE;
    }
}
