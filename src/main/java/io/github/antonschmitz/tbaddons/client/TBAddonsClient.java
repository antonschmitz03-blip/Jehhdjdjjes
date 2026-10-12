package io.github.antonschmitz.tbaddons.client;

import io.github.antonschmitz.backpacklink.client.BackpackLinkClient;
import io.github.antonschmitz.tbaddons.TBAddons;
import net.fabricmc.api.ClientModInitializer;

public class TBAddonsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        if (TBAddons.isStorageLinkEnabled()) {
            new BackpackLinkClient().onInitializeClient();
        }
    }
}
