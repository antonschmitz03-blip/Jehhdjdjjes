package io.github.antonschmitz.waystones;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

/** {@code config/simplewaystones.json}. Written with the defaults on first start. */
public final class WaystoneConfig {
    /** Teleports shorter than this are free; each further step of this many blocks costs one level. */
    public int blocksPerLevel = 500;
    /** Most levels the distance part of the cost can reach. */
    public int maxDistanceLevels = 3;
    /** Extra levels for teleporting into another dimension. */
    public int dimensionChangeLevels = 1;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static WaystoneConfig instance = new WaystoneConfig();

    public static WaystoneConfig get() {
        return instance;
    }

    static void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve(SimpleWaystones.MOD_ID + ".json");
        WaystoneConfig loaded = null;
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                loaded = GSON.fromJson(reader, WaystoneConfig.class);
            } catch (IOException | RuntimeException e) {
                SimpleWaystones.LOGGER.error("Could not read {}, using the defaults", path, e);
            }
        }
        instance = loaded != null ? loaded : new WaystoneConfig();
        instance.blocksPerLevel = Math.max(1, instance.blocksPerLevel);
        instance.maxDistanceLevels = Math.max(0, instance.maxDistanceLevels);
        instance.dimensionChangeLevels = Math.max(0, instance.dimensionChangeLevels);
        try (Writer writer = Files.newBufferedWriter(path)) {
            GSON.toJson(instance, writer);
        } catch (IOException e) {
            SimpleWaystones.LOGGER.error("Could not write {}", path, e);
        }
    }
}
