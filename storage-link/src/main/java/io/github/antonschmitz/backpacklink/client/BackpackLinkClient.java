package io.github.antonschmitz.backpacklink.client;

import com.tom.storagemod.screen.AbstractStorageTerminalScreen;
import io.github.antonschmitz.backpacklink.network.BackpackViewPayload;
import io.github.antonschmitz.backpacklink.network.ToggleBackpackPayload;
import java.util.List;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class BackpackLinkClient implements ClientModInitializer {
    private static boolean enabled = true;
    private static List<BackpackViewPayload.Entry> entries = List.of();

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(BackpackViewPayload.TYPE, (payload, context) -> {
            enabled = payload.enabled();
            entries = payload.entries();
            if (Minecraft.getInstance().screen instanceof BackpackButtonHolder holder && holder.backpacklink$button() != null) {
                holder.backpacklink$button().setState(enabled);
            }
        });
        ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
            if (!enabled || !(Minecraft.getInstance().screen instanceof AbstractStorageTerminalScreen<?>)) {
                return;
            }
            for (BackpackViewPayload.Entry entry : entries) {
                if (ItemStack.isSameItemSameComponents(entry.stack(), stack)) {
                    lines.add(Component.translatable("tooltip.backpacklink.in_backpack", entry.count()).withStyle(ChatFormatting.GOLD));
                    return;
                }
            }
        });
    }

    public static boolean enabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
        if (ClientPlayNetworking.canSend(ToggleBackpackPayload.TYPE)) {
            ClientPlayNetworking.send(new ToggleBackpackPayload(value));
        }
    }
}
