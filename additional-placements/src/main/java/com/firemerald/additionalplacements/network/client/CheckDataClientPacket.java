package com.firemerald.additionalplacements.network.client;

import com.firemerald.additionalplacements.util.BufUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import com.firemerald.additionalplacements.network.server.ServerConfigurationPacket;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import com.firemerald.additionalplacements.generation.Registration;
import com.firemerald.additionalplacements.network.server.CheckDataServerPacket;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;

public abstract class CheckDataClientPacket implements ClientConfigurationPacket {
		public static CheckDataClientPacket of() {
		return com.firemerald.additionalplacements.network.client.fabric.CheckDataClientPacketImpl.of();
	}

	private final Map<Identifier, CompoundTag> data;

	public CheckDataClientPacket() {
		data = new HashMap<>();
		Registration.forEach((id, type) -> {
			CompoundTag tag = type.getServerCheckData();
			if (tag != null) data.put(id, tag);
		});
	}

	public CheckDataClientPacket(FriendlyByteBuf buf) {
		data = BufUtils.readMap(buf, FriendlyByteBuf::readIdentifier, buffer -> buffer.readNbt());
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		BufUtils.writeMap(buf, data, FriendlyByteBuf::writeIdentifier, (buffer, tag) -> buffer.writeNbt(tag));
	}

	@Override
	public @Nullable ServerConfigurationPacket handleClient(Consumer<Runnable> enqueueWork, Consumer<Component> disconnect) {
		//PlatformUtils.checkIsClient(); check disabled for performance
		return CheckDataServerPacket.of(data);
	}
}