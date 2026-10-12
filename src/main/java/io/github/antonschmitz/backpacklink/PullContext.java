package io.github.antonschmitz.backpacklink;

import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * The player a terminal is currently pulling items for. Tom's terminals are shared block entities, so the
 * player is only known around the menu and crafting calls that act for one player; pulls outside of them
 * never touch a backpack.
 */
public final class PullContext {
    private static final ThreadLocal<ServerPlayer> PLAYER = new ThreadLocal<>();

    private PullContext() {
    }

    public static <T> T with(Player player, Supplier<T> body) {
        ServerPlayer previous = PLAYER.get();
        PLAYER.set(player instanceof ServerPlayer sp ? sp : null);
        try {
            return body.get();
        } finally {
            PLAYER.set(previous);
        }
    }

    @Nullable
    public static ServerPlayer player() {
        return PLAYER.get();
    }
}
