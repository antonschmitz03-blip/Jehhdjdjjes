package io.github.antonschmitz.newgameplus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.datafix.DataFixers;

/** Reads the singleplayer player out of a world folder without changing anything in it. */
public final class SourceReader {
    private SourceReader() {
    }

    /**
     * @param worldDir the world folder
     * @param preferred whose player to read if the world doesn't say (the local user)
     * @return the player data upgraded to the current game version, or empty if the world has none
     */
    public static Optional<CompoundTag> readPlayer(Path worldDir, UUID preferred) throws IOException {
        CompoundTag data = NbtIo.readCompressed(worldDir.resolve("level.dat"), NbtAccounter.unlimitedHeap())
                .getCompoundOrEmpty("Data");
        int worldVersion = NbtUtils.getDataVersion(data, -1);

        // Worlds from before the player moved out of level.dat.
        Optional<CompoundTag> legacy = data.getCompound("Player");
        if (legacy.isPresent()) {
            return Optional.of(fix(legacy.get(), worldVersion));
        }

        Path playerDir = worldDir.resolve("playerdata");
        Optional<UUID> owner = data.read("singleplayer_uuid", UUIDUtil.CODEC);
        for (UUID id : new UUID[]{owner.orElse(null), preferred}) {
            if (id != null && Files.isRegularFile(playerDir.resolve(id + ".dat"))) {
                return Optional.of(fix(read(playerDir.resolve(id + ".dat")), worldVersion));
            }
        }
        // Fall back to whoever played there last.
        if (Files.isDirectory(playerDir)) {
            try (Stream<Path> files = Files.list(playerDir)) {
                Optional<Path> newest = files.filter(p -> p.getFileName().toString().endsWith(".dat"))
                        .max(Comparator.comparingLong(p -> p.toFile().lastModified()));
                if (newest.isPresent()) {
                    return Optional.of(fix(read(newest.get()), worldVersion));
                }
            }
        }
        return Optional.empty();
    }

    private static CompoundTag read(Path file) throws IOException {
        return NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
    }

    private static CompoundTag fix(CompoundTag player, int worldVersion) {
        int version = NbtUtils.getDataVersion(player, worldVersion);
        if (version < 0) {
            return player;
        }
        return DataFixTypes.PLAYER.updateToCurrentVersion(DataFixers.getDataFixer(), player, version);
    }
}
