package io.github.antonschmitz.backpacklink.network;

import io.github.antonschmitz.backpacklink.BackpackLink;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

/** Sent while a terminal is open: the toggle state and what the player's backpacks hold, for the tooltip. */
public record BackpackViewPayload(boolean enabled, List<Entry> entries) implements CustomPacketPayload {
    public static final Type<BackpackViewPayload> TYPE = new Type<>(BackpackLink.id("backpack_view"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BackpackViewPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, BackpackViewPayload::enabled,
            Entry.STREAM_CODEC.apply(ByteBufCodecs.list()), BackpackViewPayload::entries,
            BackpackViewPayload::new);

    @Override
    public Type<BackpackViewPayload> type() {
        return TYPE;
    }

    /** {@code stack} is a count 1 template; {@code count} can be larger than a stack. */
    public record Entry(ItemStack stack, long count) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                ItemStack.STREAM_CODEC, Entry::stack,
                ByteBufCodecs.VAR_LONG, Entry::count,
                Entry::new);
    }
}
