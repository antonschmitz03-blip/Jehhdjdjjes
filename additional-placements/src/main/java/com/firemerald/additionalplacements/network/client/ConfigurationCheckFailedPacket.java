package com.firemerald.additionalplacements.network.client;

import com.firemerald.additionalplacements.util.BufUtils;
import java.util.List;
import java.util.function.Consumer;

import com.firemerald.additionalplacements.network.server.ServerConfigurationPacket;
import com.firemerald.additionalplacements.util.PlatformUtils;
import org.apache.commons.lang3.tuple.Triple;

import com.firemerald.additionalplacements.AdditionalPlacementsMod;
import com.firemerald.additionalplacements.client.gui.screen.ConnectionErrorsScreen;
import com.firemerald.additionalplacements.util.MessageTree;
import com.mojang.realmsclient.RealmsMainScreen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

public abstract class ConfigurationCheckFailedPacket implements ClientConfigurationPacket {
		public static ConfigurationCheckFailedPacket of(List<Triple<Identifier, List<MessageTree>, List<MessageTree>>> compiledErrors) {
		return com.firemerald.additionalplacements.network.client.fabric.ConfigurationCheckFailedPacketImpl.of(compiledErrors);
	}

	private final List<Triple<Identifier, List<MessageTree>, List<MessageTree>>> compiledErrors;

	public ConfigurationCheckFailedPacket(List<Triple<Identifier, List<MessageTree>, List<MessageTree>>> compiledErrors) {
		this.compiledErrors = compiledErrors;
	}

	public ConfigurationCheckFailedPacket(FriendlyByteBuf buf) {
		compiledErrors = BufUtils.readList(buf, buf2 -> {
			Identifier id = buf2.readIdentifier();
			List<MessageTree> clientErrors = BufUtils.readList(buf2, MessageTree::new);
			List<MessageTree> serverErrors = BufUtils.readList(buf2, MessageTree::new);
			return Triple.of(id, clientErrors, serverErrors);
		});
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		BufUtils.writeCollection(buf, compiledErrors, (buf2, data) -> {
			buf2.writeIdentifier(data.getLeft());
			BufUtils.writeCollection(buf2, data.getMiddle(), MessageTree::write);
			BufUtils.writeCollection(buf2, data.getRight(), MessageTree::write);
		});
	}

	@Override
	public @Nullable ServerConfigurationPacket handleClient(Consumer<Runnable> enqueueWork, Consumer<Component> disconnect) {
		//PlatformUtils.checkIsClient(); check disabled for performance
		MessageTree rootError = new MessageTree(Component.translatable("msg.additionalplacements.errors.type"));
		compiledErrors.forEach(data -> {
			MessageTree typeError = new MessageTree(Component.literal(data.getLeft().toString())); //TODO friendly name?
			List<MessageTree> clientErrors = data.getMiddle();
			if (!clientErrors.isEmpty()) {
				MessageTree clientError = new MessageTree(Component.translatable("msg.additionalplacements.errors.client"), clientErrors);
				typeError.children.add(clientError);
			}
			List<MessageTree> serverErrors = data.getRight();
			if (!serverErrors.isEmpty()) {
				MessageTree serverError = new MessageTree(Component.translatable("msg.additionalplacements.errors.server"), serverErrors);
				typeError.children.add(serverError);
			}
			rootError.children.add(typeError);
		});
		rootError.output(AdditionalPlacementsMod.LOGGER::warn);
		//TODO move disconnect to a point that works?
		Minecraft.getInstance().execute(new HandleErrors(rootError));
		return null;
	}

	record HandleErrors(MessageTree rootError) implements Runnable {
		static {
			PlatformUtils.checkIsClient();
		}

		@Override
		public void run() {
			Minecraft minecraft = Minecraft.getInstance();
			Screen desScreen = new TitleScreen();
			if (!minecraft.isLocalServer()) {
				if (minecraft.getCurrentServer().isRealm()) {
					desScreen = new RealmsMainScreen(desScreen);
				} else {
					desScreen = new JoinMultiplayerScreen(desScreen);
				}
			}
			minecraft.disconnect(new ConnectionErrorsScreen(rootError, desScreen), true);
		}
	}
}