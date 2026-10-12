package io.github.antonschmitz.newgameplus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NewGamePlus implements ModInitializer {
    public static final String MOD_ID = "newgameplus";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /** Set by the Create World screen just before the new world starts; picked up by the starting server. */
    public static volatile ImportData armed;

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTING.register(NewGamePlus::storeArmedImport);
        if (Boolean.getBoolean("newgameplus.selfTest")) {
            ServerLifecycleEvents.SERVER_STARTED.register(SelfTest::run);
        }
    }

    /** Remembers the import inside the new world, so it is applied on first join even after a restart. */
    private static void storeArmedImport(MinecraftServer server) {
        ImportData data = armed;
        armed = null;
        if (data == null) {
            return;
        }
        try {
            NbtIo.writeCompressed(data.toTag(), pendingFile(server));
            LOGGER.info("New Game Plus: importing the player from \"{}\" on first join", data.sourceName());
        } catch (IOException e) {
            LOGGER.error("New Game Plus: could not store the player import", e);
        }
    }

    public static Path pendingFile(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("newgameplus_pending.dat");
    }

    static boolean hasPending(MinecraftServer server) {
        return Files.isRegularFile(pendingFile(server));
    }
}
