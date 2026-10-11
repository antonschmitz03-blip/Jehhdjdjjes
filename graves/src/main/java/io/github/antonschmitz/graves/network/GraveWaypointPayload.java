package io.github.antonschmitz.graves.network;

import io.github.antonschmitz.graves.SimpleGraves;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Tells the owner's client that a grave was placed ({@code added}) or removed, for the map waypoint. */
public record GraveWaypointPayload(boolean added, BlockPos pos, String dimension) implements CustomPacketPayload {
    public static final Type<GraveWaypointPayload> TYPE = new Type<>(SimpleGraves.id("grave_waypoint"));
    public static final StreamCodec<FriendlyByteBuf, GraveWaypointPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, GraveWaypointPayload::added,
            BlockPos.STREAM_CODEC, GraveWaypointPayload::pos,
            ByteBufCodecs.STRING_UTF8, GraveWaypointPayload::dimension,
            GraveWaypointPayload::new);

    @Override
    public Type<GraveWaypointPayload> type() {
        return TYPE;
    }
}
