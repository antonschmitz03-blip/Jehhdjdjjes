package com.firemerald.additionalplacements.network.server;

import com.firemerald.additionalplacements.client.APClientData;
import com.firemerald.additionalplacements.common.IAPServerPlayer;

import com.firemerald.additionalplacements.network.client.ClientPlayPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Consumer;

public abstract class SetPlacementTogglePacket implements ServerPlayPacket {
		public static SetPlacementTogglePacket of(boolean state) {
		return com.firemerald.additionalplacements.network.server.fabric.SetPlacementTogglePacketImpl.of(state);
	}

	private final boolean state;

	public SetPlacementTogglePacket(boolean state) {
		this.state = state;
	}

	public SetPlacementTogglePacket(FriendlyByteBuf buf) {
		this.state = buf.readBoolean();
	}

	@Override
	public void write(RegistryFriendlyByteBuf buf) {
		buf.writeBoolean(state);
	}

	@Override
	public ClientPlayPacket handleServer(ServerPlayer player, Consumer<Runnable> enqueueWork) {
		if (player instanceof IAPServerPlayer iapServerPlayer) iapServerPlayer.additionalplacements$setPlacementEnabled(state);
		return null;
	}

	@Override
	public void sendToServer() {
		ServerPlayPacket.super.sendToServer();
		APClientData.lastSynchronizedTime = System.currentTimeMillis();
	}
}