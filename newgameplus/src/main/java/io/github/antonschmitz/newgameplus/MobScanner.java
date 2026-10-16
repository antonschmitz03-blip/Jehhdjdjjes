package io.github.antonschmitz.newgameplus;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.level.chunk.storage.RegionFileVersion;

/** Finds the mobs a player owns (tamed pets, ridden happy ghasts...) in a world folder, read-only. */
public final class MobScanner {
    public static final int MAX_MOBS = 64;
    private static final Pattern REGION_NAME = Pattern.compile("r\\.(-?\\d+)\\.(-?\\d+)\\.mca");
    /** Item and equipment data can hold arbitrary player ids (skulls...), so owners are never looked up inside. */
    private static final Set<String> SKIP_KEYS = Set.of("Items", "equipment", "components", "Inventory", "saddle",
            "body_armor_item", "drop_chances", "Brain");

    private MobScanner() {
    }

    /** All saved mobs owned by {@code owner}, in every dimension's entity storage. */
    public static List<CompoundTag> findOwned(Path worldDir, int[] owner) throws IOException {
        List<CompoundTag> found = new ArrayList<>();
        List<Path> regions;
        try (Stream<Path> files = Files.walk(worldDir, 7)) {
            regions = files.filter(p -> Files.isRegularFile(p) && p.getParent() != null
                            && p.getParent().getFileName().toString().equals("entities")
                            && REGION_NAME.matcher(p.getFileName().toString()).matches())
                    .sorted().toList();
        }
        for (Path region : regions) {
            Matcher name = REGION_NAME.matcher(region.getFileName().toString());
            name.matches();
            int regionX = Integer.parseInt(name.group(1));
            int regionZ = Integer.parseInt(name.group(2));
            try {
                scanRegion(region, regionX, regionZ, owner, found);
            } catch (IOException | RuntimeException e) {
                NewGamePlus.LOGGER.warn("New Game Plus: could not read {}: {}", region, e.toString());
            }
            if (found.size() >= MAX_MOBS) {
                break;
            }
        }
        return found.size() > MAX_MOBS ? new ArrayList<>(found.subList(0, MAX_MOBS)) : found;
    }

    private static void scanRegion(Path file, int regionX, int regionZ, int[] owner, List<CompoundTag> found) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "r")) {
            byte[] header = new byte[4096];
            if (raf.length() < 8192) {
                return;
            }
            raf.readFully(header);
            for (int index = 0; index < 1024; index++) {
                int location = ((header[index * 4] & 0xFF) << 24) | ((header[index * 4 + 1] & 0xFF) << 16)
                        | ((header[index * 4 + 2] & 0xFF) << 8) | (header[index * 4 + 3] & 0xFF);
                int sector = location >>> 8;
                if (sector == 0) {
                    continue;
                }
                try {
                    CompoundTag chunk = readChunk(raf, file, sector, regionX * 32 + index % 32, regionZ * 32 + index / 32);
                    if (chunk != null) {
                        collect(chunk, owner, found);
                    }
                } catch (IOException | RuntimeException e) {
                    NewGamePlus.LOGGER.warn("New Game Plus: skipped an unreadable chunk in {}: {}", file.getFileName(), e.toString());
                }
                if (found.size() >= MAX_MOBS) {
                    return;
                }
            }
        }
    }

    private static CompoundTag readChunk(RandomAccessFile raf, Path file, int sector, int chunkX, int chunkZ) throws IOException {
        raf.seek(sector * 4096L);
        int length = raf.readInt();
        int type = raf.readUnsignedByte();
        InputStream raw;
        if ((type & 0x80) != 0) {
            Path external = file.resolveSibling("c." + chunkX + "." + chunkZ + ".mcc");
            if (!Files.isRegularFile(external)) {
                return null;
            }
            raw = new ByteArrayInputStream(Files.readAllBytes(external));
            type &= 0x7F;
        } else {
            if (length < 1 || length > 16 * 1024 * 1024) {
                return null;
            }
            byte[] data = new byte[length - 1];
            raf.readFully(data);
            raw = new ByteArrayInputStream(data);
        }
        RegionFileVersion version = RegionFileVersion.fromId(type);
        if (version == null) {
            return null;
        }
        try (DataInputStream in = new DataInputStream(version.wrap(raw))) {
            return NbtIo.read(in, NbtAccounter.unlimitedHeap());
        }
    }

    private static void collect(CompoundTag chunk, int[] owner, List<CompoundTag> found) {
        int dataVersion = NbtUtils.getDataVersion(chunk, -1);
        CompoundTag fixed = dataVersion < 0 ? chunk
                : DataFixTypes.ENTITY_CHUNK.updateToCurrentVersion(DataFixers.getDataFixer(), chunk, dataVersion);
        ListTag entities = fixed.getListOrEmpty("Entities");
        for (int i = 0; i < entities.size(); i++) {
            CompoundTag entity = entities.getCompoundOrEmpty(i);
            String id = entity.getStringOr("id", "");
            if (!id.isEmpty() && !id.equals("minecraft:player") && ownedBy(entity, owner)) {
                found.add(entity);
            }
        }
    }

    public static boolean ownedBy(CompoundTag entity, int[] owner) {
        return rewrite(entity, owner, null);
    }

    /** Points every owner entry of {@code from} at {@code to} instead (e.g. if the player's id differs). */
    public static void retarget(CompoundTag entity, int[] from, int[] to) {
        if (!Arrays.equals(from, to)) {
            rewrite(entity, from, to);
        }
    }

    /** Walks the entity data looking for an "owner" entry equal to {@code from}; replaces it when {@code to != null}. */
    private static boolean rewrite(CompoundTag tag, int[] from, int[] to) {
        boolean found = false;
        for (String key : List.copyOf(tag.keySet())) {
            if (SKIP_KEYS.contains(key)) {
                continue;
            }
            Tag value = tag.get(key);
            if (value instanceof IntArrayTag array) {
                if (key.toLowerCase(Locale.ROOT).contains("owner") && Arrays.equals(array.getAsIntArray(), from)) {
                    found = true;
                    if (to != null) {
                        tag.put(key, new IntArrayTag(to.clone()));
                    }
                }
            } else if (value instanceof CompoundTag child) {
                found |= rewrite(child, from, to);
            } else if (value instanceof ListTag list) {
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i) instanceof CompoundTag child) {
                        found |= rewrite(child, from, to);
                    }
                }
            }
        }
        return found;
    }
}
