package io.github.antonschmitz.waystones;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import org.jspecify.annotations.Nullable;

/**
 * Every waystone in the world and which ones each player has discovered. Stored as
 * {@code simplewaystones.json} in the world folder and saved on every change.
 */
public final class WaystoneData {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static @Nullable WaystoneData instance;

    private final transient @Nullable Path file;
    private final transient @Nullable MinecraftServer server;
    private final Map<String, Waystone> waystones = new LinkedHashMap<>();
    /** Player UUID to the keys of the waystones they discovered. */
    private final Map<UUID, Set<String>> discovered = new HashMap<>();

    public static final class Waystone {
        String dimension;
        int x;
        int y;
        int z;
        /** Name given with an anvil or a name tag; null shows "<biome> Waystone". */
        @Nullable String name;
        String biome;
        UUID owner;
        String ownerName;

        Waystone(String dimension, BlockPos pos, @Nullable String name, String biome, UUID owner, String ownerName) {
            this.dimension = dimension;
            this.x = pos.getX();
            this.y = pos.getY();
            this.z = pos.getZ();
            this.name = name;
            this.biome = biome;
            this.owner = owner;
            this.ownerName = ownerName;
        }

        public String key() {
            return WaystoneData.key(dimension, x, y, z);
        }

        public String dimension() {
            return dimension;
        }

        public BlockPos pos() {
            return new BlockPos(x, y, z);
        }

        public UUID owner() {
            return owner;
        }

        public String ownerName() {
            return ownerName;
        }

        public Component displayName() {
            if (name != null && !name.isBlank()) {
                return Component.literal(name);
            }
            return Component.translatable("simplewaystones.default_name", Component.translatable(biome));
        }

        /** For sorting the list: the given name, or the biome's translation key. */
        String sortName() {
            return name != null ? name.toLowerCase() : biome;
        }
    }

    private WaystoneData(@Nullable Path file, @Nullable MinecraftServer server) {
        this.file = file;
        this.server = server;
    }

    public static WaystoneData get() {
        if (instance == null) {
            // Only reachable before the server started; keep an in-memory store so nothing crashes.
            instance = new WaystoneData(null, null);
        }
        return instance;
    }

    static void load(MinecraftServer server) {
        Path path = server.getWorldPath(LevelResource.ROOT).resolve(SimpleWaystones.MOD_ID + ".json");
        WaystoneData data = new WaystoneData(path, server);
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                Stored stored = GSON.fromJson(reader, Stored.class);
                if (stored != null) {
                    if (stored.waystones != null) {
                        for (Waystone waystone : stored.waystones) {
                            data.waystones.put(waystone.key(), waystone);
                        }
                    }
                    if (stored.discovered != null) {
                        stored.discovered.forEach((player, keys) -> data.discovered.put(player, new LinkedHashSet<>(keys)));
                    }
                }
            } catch (IOException | RuntimeException e) {
                SimpleWaystones.LOGGER.error("Could not read {}", path, e);
            }
        }
        instance = data;
    }

    static void unload() {
        instance = null;
    }

    /** What actually goes into the file. */
    private static final class Stored {
        List<Waystone> waystones;
        Map<UUID, List<String>> discovered;
    }

    void save() {
        if (file == null) {
            return;
        }
        Stored stored = new Stored();
        stored.waystones = new ArrayList<>(waystones.values());
        stored.discovered = new HashMap<>();
        discovered.forEach((player, keys) -> stored.discovered.put(player, new ArrayList<>(keys)));
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            try (Writer writer = Files.newBufferedWriter(temp)) {
                GSON.toJson(stored, writer);
            }
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            SimpleWaystones.LOGGER.error("Could not save {}", file, e);
        }
    }

    static String key(String dimension, int x, int y, int z) {
        return dimension + "|" + x + "|" + y + "|" + z;
    }

    static String dimensionId(Level level) {
        return level.dimension().identifier().toString();
    }

    public @Nullable Waystone at(ServerLevel level, BlockPos pos) {
        return waystones.get(key(dimensionId(level), pos.getX(), pos.getY(), pos.getZ()));
    }

    public @Nullable Waystone byKey(String key) {
        return waystones.get(key);
    }

    Waystone add(ServerLevel level, BlockPos pos, @Nullable String name, String biome, UUID owner, String ownerName) {
        Waystone waystone = new Waystone(dimensionId(level), pos, name, biome, owner, ownerName);
        waystones.put(waystone.key(), waystone);
        save();
        return waystone;
    }

    void remove(ServerLevel level, BlockPos pos) {
        removeKey(key(dimensionId(level), pos.getX(), pos.getY(), pos.getZ()));
    }

    void removeKey(String key) {
        if (waystones.remove(key) != null) {
            List<UUID> affected = discoverers(key);
            discovered.values().forEach(keys -> keys.remove(key));
            save();
            syncWaypoints(affected);
        }
    }

    void rename(Waystone waystone, String name) {
        waystone.name = name;
        save();
        syncWaypoints(discoverers(waystone.key()));
    }

    private List<UUID> discoverers(String key) {
        List<UUID> players = new ArrayList<>();
        discovered.forEach((player, keys) -> {
            if (keys.contains(key)) {
                players.add(player);
            }
        });
        return players;
    }

    private void syncWaypoints(List<UUID> players) {
        if (server != null) {
            WaypointSync.sendToDiscoverers(server, players);
        }
    }

    public boolean hasDiscovered(UUID player, Waystone waystone) {
        return discovered.getOrDefault(player, Set.of()).contains(waystone.key());
    }

    /** Returns false when the player already knew it. */
    boolean discover(UUID player, Waystone waystone) {
        boolean added = discovered.computeIfAbsent(player, p -> new LinkedHashSet<>()).add(waystone.key());
        if (added) {
            save();
            syncWaypoints(List.of(player));
        }
        return added;
    }

    /** The player's discovered waystones, sorted by name. */
    public List<Waystone> discovered(UUID player) {
        List<Waystone> list = new ArrayList<>();
        for (String key : discovered.getOrDefault(player, Set.of())) {
            Waystone waystone = waystones.get(key);
            if (waystone != null) {
                list.add(waystone);
            }
        }
        list.sort(Comparator.comparing(Waystone::sortName).thenComparing(Waystone::key));
        return list;
    }

    public int size() {
        return waystones.size();
    }
}
