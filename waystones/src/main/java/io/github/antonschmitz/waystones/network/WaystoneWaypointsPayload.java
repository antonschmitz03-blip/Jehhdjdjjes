package io.github.antonschmitz.waystones.network;

import io.github.antonschmitz.waystones.SimpleWaystones;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Every waystone the player has discovered in {@code dimension}, for the map waypoints. Sent whenever that list
 * changes and when the player enters a dimension; the client makes its waypoints match it.
 */
public record WaystoneWaypointsPayload(String dimension, List<Entry> waystones) implements CustomPacketPayload {
    public static final Type<WaystoneWaypointsPayload> TYPE = new Type<>(SimpleWaystones.id("waypoints"));

    /** {@code name} is empty for an unnamed waystone, which the client shows as "<biome> Waystone". */
    public record Entry(BlockPos pos, String name, String biome) {
        static final StreamCodec<FriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Entry::pos,
                ByteBufCodecs.STRING_UTF8, Entry::name,
                ByteBufCodecs.STRING_UTF8, Entry::biome,
                Entry::new);
    }

    public static final StreamCodec<FriendlyByteBuf, WaystoneWaypointsPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, WaystoneWaypointsPayload::dimension,
            Entry.STREAM_CODEC.apply(ByteBufCodecs.list()), WaystoneWaypointsPayload::waystones,
            WaystoneWaypointsPayload::new);

    @Override
    public Type<WaystoneWaypointsPayload> type() {
        return TYPE;
    }
}
