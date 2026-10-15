package io.github.antonschmitz.happyghastplus;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Sent by a rider's client when they press the searchlight key. */
public record SearchlightTogglePayload() implements CustomPacketPayload {
    public static final SearchlightTogglePayload INSTANCE = new SearchlightTogglePayload();
    public static final Type<SearchlightTogglePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(HappyGhastPlus.MOD_ID, "toggle_searchlight"));
    public static final StreamCodec<ByteBuf, SearchlightTogglePayload> CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
