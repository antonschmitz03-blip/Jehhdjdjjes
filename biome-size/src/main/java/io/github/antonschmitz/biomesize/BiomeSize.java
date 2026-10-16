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
    public static final int MIN_PERCENT = 10;
    public static final int MAX_PERCENT = 400;
    public static final int DEFAULT_PERCENT = 100;

    /** Sizes of the world the server is currently running, read by NoisesMixin while noises are created. */
    private static volatile int activeBiomePercent = DEFAULT_PERCENT;
    private static volatile int activeTerrainPercent = DEFAULT_PERCENT;

    @Override
    public void onInitialize() {
        // Fires before the levels (and their RandomStates) are created.
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            Path file = server.getWorldPath(LevelResource.ROOT).resolve(FILE_NAME);
            Settings settings = read(file);
            activeBiomePercent = settings.biomePercent();
            activeTerrainPercent = settings.terrainPercent();
            if (!settings.isDefault()) {
                LOGGER.info("Biome Size: biome size {}%, terrain scale {}%", settings.biomePercent(), settings.terrainPercent());
            }
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            activeBiomePercent = DEFAULT_PERCENT;
            activeTerrainPercent = DEFAULT_PERCENT;
        });
    }

    public static int activeBiomePercent() {
        return activeBiomePercent;
    }

    public static int activeTerrainPercent() {
        return activeTerrainPercent;
    }

    public static int clamp(int percent) {
        return Math.max(MIN_PERCENT, Math.min(MAX_PERCENT, percent));
    }

    public record Settings(int biomePercent, int terrainPercent) {
        public static final Settings DEFAULT = new Settings(DEFAULT_PERCENT, DEFAULT_PERCENT);

        public boolean isDefault() {
            return biomePercent == DEFAULT_PERCENT && terrainPercent == DEFAULT_PERCENT;
        }
    }

    static Settings read(Path file) {
        if (!Files.isRegularFile(file)) {
            return Settings.DEFAULT;
        }
        try {
            JsonObject json = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            int biome = json.has("biome_size") ? json.get("biome_size").getAsInt() : DEFAULT_PERCENT;
            int terrain = json.has("terrain_scale") ? json.get("terrain_scale").getAsInt() : DEFAULT_PERCENT;
            return new Settings(clamp(biome), clamp(terrain));
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Biome Size: could not read {}, using vanilla sizes", file, e);
            return Settings.DEFAULT;
        }
    }

    public static void write(Path worldDir, Settings settings) {
        if (settings.isDefault()) {
            return;
        }
        JsonObject json = new JsonObject();
        json.addProperty("biome_size", settings.biomePercent());
        json.addProperty("terrain_scale", settings.terrainPercent());
        try {
            Files.writeString(worldDir.resolve(FILE_NAME), json.toString());
        } catch (IOException e) {
            LOGGER.error("Biome Size: could not save the sizes for the new world", e);
        }
    }
}
