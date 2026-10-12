package io.github.antonschmitz.backpacklink.network;

import io.github.antonschmitz.backpacklink.BackpackLink;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** The terminal's backpack button was clicked. */
public record ToggleBackpackPayload(boolean enabled) implements CustomPacketPayload {
    public static final Type<ToggleBackpackPayload> TYPE = new Type<>(BackpackLink.id("toggle_backpack"));
    public static final StreamCodec<FriendlyByteBuf, ToggleBackpackPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, ToggleBackpackPayload::enabled,
            ToggleBackpackPayload::new);

    @Override
    public Type<ToggleBackpackPayload> type() {
        return TYPE;
    }
}
