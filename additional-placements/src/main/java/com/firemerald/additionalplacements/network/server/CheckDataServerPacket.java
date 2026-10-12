package com.firemerald.additionalplacements.network.server;

import com.firemerald.additionalplacements.util.BufUtils;
import java.util.*;
import java.util.function.Consumer;

import com.firemerald.additionalplacements.network.CheckDataConfigurationTask;
import com.firemerald.additionalplacements.network.client.ClientConfigurationPacket;
import net.minecraft.server.network.ConfigurationTask;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import com.firemerald.additionalplacements.generation.Registration;
import com.firemerald.additionalplacements.network.client.ConfigurationCheckFailedPacket;
import com.firemerald.additionalplacements.util.MessageTree;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

public abstract class CheckDataServerPacket implements ServerConfigurationPacket {
		public static CheckDataServerPacket of(Map<Identifier, CompoundTag> serverData) {
		return com.firemerald.additionalplacements.network.server.fabric.CheckDataServerPacketImpl.of(serverData);
	}

	private final Map<Identifier, Pair<CompoundTag, List<MessageTree>>> serverData;

	public CheckDataServerPacket(Map<Identifier, CompoundTag> serverData) {
		this.serverData = new HashMap<>();
		Registration.forEach((id, type) -> {
			CompoundTag clientTag = type.getClientCheckData();
			List<MessageTree> clientErrors = new ArrayList<>();
			CompoundTag serverTag = serverData.get(id);
			type.checkServerData(serverTag, clientErrors::add);
			if (clientTag != null || !clientErrors.isEmpty()) this.serverData.put(id, Pair.of(clientTag, clientErrors));
		});
	}

	public CheckDataServerPacket(FriendlyByteBuf buf) {
		serverData = BufUtils.readMap(buf, FriendlyByteBuf::readIdentifier, buf2 -> {
			CompoundTag clientTag = buf2.readNbt();
			List<MessageTree> clientErrors = BufUtils.readList(buf2, MessageTree::new);
			return Pair.of(clientTag, clientErrors);
		});
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		BufUtils.writeMap(buf, serverData, FriendlyByteBuf::writeIdentifier, (buf2, data) -> {
			buf2.writeNbt(data.getLeft());
			BufUtils.writeCollection(buf2, data.getRight(), MessageTree::write);
		});
	}

	@Override
	public @Nullable ClientConfigurationPacket handleServer(Consumer<Runnable> enqueueWork, Consumer<Component> disconnect, Consumer<ConfigurationTask.Type> finishTask) {
		List<Triple<Identifier, List<MessageTree>, List<MessageTree>>> compiledErrors = new ArrayList<>();
		Registration.forEach((id, type) -> {
			Pair<CompoundTag, List<MessageTree>> clientData = this.serverData.get(id);
			CompoundTag clientTag;
			List<MessageTree> clientErrors;
			if (clientData != null) {
				clientTag = clientData.getLeft();
				clientErrors = clientData.getRight();
			} else {
				clientTag = null;
				clientErrors = Collections.emptyList();
			}
			List<MessageTree> serverErrors = new ArrayList<>();
			type.checkClientData(clientTag, serverErrors::add);
			if (!clientErrors.isEmpty() || !serverErrors.isEmpty()) compiledErrors.add(Triple.of(id, clientErrors, serverErrors));
		});
		ClientConfigurationPacket rep;
		if (!compiledErrors.isEmpty()) {
			//if it turns out this CAN prevent the above packet from being sent, move disconnect to client-side. It did not prevent it in testing.
			disconnect.accept(Component.translatable("msg.additionalplacements.disconnected"));
			rep = ConfigurationCheckFailedPacket.of(compiledErrors);
		} else rep = null;
		finishTask.accept(CheckDataConfigurationTask.TYPE); //finish task, very important!
		return rep;
	}
}