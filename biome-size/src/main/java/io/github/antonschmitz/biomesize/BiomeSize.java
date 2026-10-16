package io.github.antonschmitz.biomesize;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BiomeSize implements ModInitializer {
    public static final String MOD_ID = "biomesize";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    /** Per-world settings file, written next to level.dat when the world is created. */
    public static final String FILE_NAME = "biomesize.json";
    public static final int DEFAULT_PERCENT = 100;
    public static final int MIN_SIZE = 10;
    public static final int MAX_SIZE = 400;
    public static final int MIN_DRAMATIC = 0;
    public static final int MAX_DRAMATIC = 300;
    public static final int MIN_CAVE = 25;
    public static final int MAX_CAVE = 300;

    /** Settings of the world the server is currently running, read by NoisesMixin while noises are created. */
    private static volatile Settings active = Settings.DEFAULT;

    @Override
    public void onInitialize() {
        // Fires before the levels (and their RandomStates) are created.
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            Path file = server.getWorldPath(LevelResource.ROOT).resolve(FILE_NAME);
            active = read(file);
            if (!active.isDefault()) {
                LOGGER.info("Biome Size: biome size {}%, terrain scale {}%, dramatic terrain {}%, cave size {}%",
                        active.biomePercent(), active.terrainPercent(), active.dramaticPercent(), active.cavePercent());
            }
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> active = Settings.DEFAULT);
    }

    public static Settings active() {
        return active;
    }

    public static int clamp(int percent, int min, int max) {
        return Math.max(min, Math.min(max, percent));
    }

    public record Settings(int biomePercent, int terrainPercent, int dramaticPercent, int cavePercent) {
        public static final Settings DEFAULT = new Settings(DEFAULT_PERCENT, DEFAULT_PERCENT, DEFAULT_PERCENT, DEFAULT_PERCENT);

        public boolean isDefault() {
            return this.equals(DEFAULT);
        }
    }

    static Settings read(Path file) {
        if (!Files.isRegularFile(file)) {
            return Settings.DEFAULT;
        }
        try {
            JsonObject json = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            return new Settings(
                    clamp(intOr(json, "biome_size"), MIN_SIZE, MAX_SIZE),
                    clamp(intOr(json, "terrain_scale"), MIN_SIZE, MAX_SIZE),
                    clamp(intOr(json, "dramatic_terrain"), MIN_DRAMATIC, MAX_DRAMATIC),
                    clamp(intOr(json, "cave_size"), MIN_CAVE, MAX_CAVE));
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Biome Size: could not read {}, using vanilla sizes", file, e);
            return Settings.DEFAULT;
        }
    }

    private static int intOr(JsonObject json, String key) {
        return json.has(key) ? json.get(key).getAsInt() : DEFAULT_PERCENT;
    }

    public static void write(Path worldDir, Settings settings) {
        if (settings.isDefault()) {
            return;
        }
        JsonObject json = new JsonObject();
        json.addProperty("biome_size", settings.biomePercent());
        json.addProperty("terrain_scale", settings.terrainPercent());
        json.addProperty("dramatic_terrain", settings.dramaticPercent());
        json.addProperty("cave_size", settings.cavePercent());
        try {
            Files.writeString(worldDir.resolve(FILE_NAME), json.toString());
        } catch (IOException e) {
            LOGGER.error("Biome Size: could not save the sizes for the new world", e);
        }
    }
}
