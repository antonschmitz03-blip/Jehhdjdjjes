package io.github.antonschmitz.newgameplus.mixin;

import io.github.antonschmitz.newgameplus.Importer;
import net.minecraft.network.Connection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The player is loaded and placed at spawn here, but nothing has been sent to them yet. */
@Mixin(PlayerList.class)
public abstract class PlayerListMixin {
    @Shadow
    @Final
    private MinecraftServer server;

    @Inject(method = "placeNewPlayer", at = @At("HEAD"))
    private void newgameplus$importPlayer(Connection connection, ServerPlayer player, CommonListenerCookie cookie, CallbackInfo ci) {
        Importer.onFirstJoin(server, player);
    }
}
