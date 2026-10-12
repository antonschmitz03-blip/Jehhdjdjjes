package io.github.antonschmitz.newgameplus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

/** Applies a stored import to the first player who joins the new world without saved data. */
public final class Importer {
    private Importer() {
    }

    /** Called right before the server sends the joining player their state. */
    public static void onFirstJoin(MinecraftServer server, ServerPlayer player) {
        Path pending = NewGamePlus.pendingFile(server);
        if (!Files.isRegularFile(pending) || server.getPlayerList().loadPlayerData(player.nameAndId()).isPresent()) {
            return;
        }
        try {
            ImportData data = ImportData.fromTag(NbtIo.readCompressed(pending, NbtAccounter.unlimitedHeap()));
            apply(player, data);
            Files.move(pending, pending.resolveSibling("newgameplus_applied.dat"), StandardCopyOption.REPLACE_EXISTING);
            NewGamePlus.LOGGER.info("New Game Plus: imported {} from \"{}\"", player.getPlainTextName(), data.sourceName());
        } catch (IOException | RuntimeException e) {
            NewGamePlus.LOGGER.error("New Game Plus: could not import the player", e);
        }
    }

    /** Overwrites the carried parts of the player with the imported ones; position and the rest stay. */
    public static void apply(ServerPlayer player, ImportData data) {
        try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(NewGamePlus.LOGGER)) {
            TagValueOutput output = TagValueOutput.createWithContext(reporter, player.registryAccess());
            player.saveWithoutId(output);
            CompoundTag merged = PlayerTransfer.merge(output.buildResult(), data);
            player.load(TagValueInput.create(reporter, player.registryAccess(), merged));
        }
    }
}
