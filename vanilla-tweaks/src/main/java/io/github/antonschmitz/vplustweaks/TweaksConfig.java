package io.github.antonschmitz.vplustweaks;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * config/vplustweaks.json: one on/off switch per feature, all on by default. The file is created on first launch
 * and new switches are added to it when the mod gets new features. Changes apply after a restart. In multiplayer,
 * set the switches on both the server and the clients (the server decides what happens, clients only show it).
 */
public final class TweaksConfig {
    /** Feature name -> description of what it switches. Order is the order in the file. */
    private static final Map<String, Boolean> DEFAULTS = new LinkedHashMap<>();
    private static final Logger LOGGER = LoggerFactory.getLogger("vplustweaks");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final String DOUBLE_DOORS = "double_doors";
    public static final String NO_CROP_TRAMPLING = "no_crop_trampling";
    public static final String NO_ANVIL_LIMIT = "no_anvil_too_expensive_limit";
    public static final String CHEAPER_ENCHANTING_TABLE = "cheaper_enchanting_table";
    public static final String CHEAPER_INFUSER = "cheaper_enchanting_infuser";
    public static final String FREE_ENCHANTMENT_COMBINING = "free_enchantment_combining";
    public static final String SAFE_PETS = "safe_tamed_pets";
    public static final String BEST_HORSES = "max_stat_horses";
    public static final String FAST_LADDERS = "fast_ladder_placement";
    public static final String FLOATING_LADDERS = "floating_ladders";
    public static final String NO_PODZOL_FROM_SAPLINGS = "no_podzol_from_large_trees";
    /** Recipe switches are named in the recipes' "fabric:load_conditions". */
    public static final String RECIPES_GUNPOWDER = "recipe_gunpowder";
    public static final String RECIPES_LEATHER = "recipe_leather_from_rotten_flesh";
    public static final String RECIPES_SADDLE = "recipe_saddle";
    public static final String RECIPES_NAME_TAG = "recipe_name_tag";
    public static final String RECIPES_HORSE_ARMOR = "recipe_horse_armor";
    public static final String RECIPES_CHAINMAIL = "recipe_chainmail_armor";
    public static final String RECIPES_STICKS = "recipe_sticks_from_logs";
    public static final String RECIPES_UNDYE = "recipe_undye";
    public static final String RECIPES_TRIDENT = "recipe_trident";

    static {
        for (String key : new String[] {
                DOUBLE_DOORS, NO_CROP_TRAMPLING, NO_ANVIL_LIMIT, CHEAPER_ENCHANTING_TABLE, CHEAPER_INFUSER,
                FREE_ENCHANTMENT_COMBINING, SAFE_PETS, BEST_HORSES, FAST_LADDERS, FLOATING_LADDERS,
                NO_PODZOL_FROM_SAPLINGS,
                RECIPES_GUNPOWDER, RECIPES_LEATHER, RECIPES_SADDLE, RECIPES_NAME_TAG, RECIPES_HORSE_ARMOR,
                RECIPES_CHAINMAIL, RECIPES_STICKS, RECIPES_UNDYE, RECIPES_TRIDENT}) {
            DEFAULTS.put(key, true);
        }
    }

    private static volatile Map<String, Boolean> values;

    private TweaksConfig() {}

    public static boolean enabled(String feature) {
        Map<String, Boolean> loaded = values;
        if (loaded == null) {
            loaded = load();
        }
        return loaded.getOrDefault(feature, true);
    }

    private static synchronized Map<String, Boolean> load() {
        if (values != null) {
            return values;
        }
        Path file = FabricLoader.getInstance().getConfigDir().resolve("vplustweaks.json");
        Map<String, Boolean> loaded = new LinkedHashMap<>(DEFAULTS);
        boolean complete = false;
        if (Files.exists(file)) {
            try {
                JsonElement root = JsonParser.parseString(Files.readString(file));
                if (root.isJsonObject()) {
                    JsonObject object = root.getAsJsonObject();
                    complete = true;
                    for (String key : DEFAULTS.keySet()) {
                        JsonElement value = object.get(key);
                        if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean()) {
                            loaded.put(key, value.getAsBoolean());
                        } else {
                            complete = false; // new or broken switch: it is added back with its default below
                        }
                    }
                }
            } catch (IOException | RuntimeException e) {
                LOGGER.warn("Could not read {}, using the defaults: {}", file, e.toString());
                values = loaded;
                return loaded;
            }
        }
        if (!complete) {
            try {
                Files.createDirectories(file.getParent());
                Files.writeString(file, GSON.toJson(loaded) + System.lineSeparator());
            } catch (IOException e) {
                LOGGER.warn("Could not write {}: {}", file, e.toString());
            }
        }
        values = loaded;
        loaded.forEach((key, on) -> {
            if (!on) {
                LOGGER.info("vplustweaks: {} is switched off in the config", key);
            }
        });
        return loaded;
    }
}
