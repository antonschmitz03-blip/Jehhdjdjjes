package com.firemerald.additionalplacements.network.client.fabric;

import com.firemerald.additionalplacements.network.APPacket;
import com.firemerald.additionalplacements.network.client.ConfigurationCheckFailedPacket;
import com.firemerald.additionalplacements.util.MessageTree;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.apache.commons.lang3.tuple.Triple;

import java.util.List;

public class ConfigurationCheckFailedPacketImpl extends ConfigurationCheckFailedPacket implements ClientConfigurationPacketImpl {
    public static final CustomPacketPayload.Type<ConfigurationCheckFailedPacketImpl> TYPE = APPacket.type("configuration_check_failed");

    public static ConfigurationCheckFailedPacket of(List<Triple<Identifier, List<MessageTree>, List<MessageTree>>> compiledErrors) {
        return new ConfigurationCheckFailedPacketImpl(compiledErrors);
    }

    public ConfigurationCheckFailedPacketImpl(List<Triple<Identifier, List<MessageTree>, List<MessageTree>>> compiledErrors) {
        super(compiledErrors);
    }

    public ConfigurationCheckFailedPacketImpl(FriendlyByteBuf buf) {
        super(buf);
    }

    @Override
    public Type<ConfigurationCheckFailedPacketImpl> type() {
        return TYPE;
    }
}
