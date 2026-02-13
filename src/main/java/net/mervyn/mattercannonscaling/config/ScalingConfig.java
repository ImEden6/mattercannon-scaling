package net.mervyn.mattercannonscaling.config;

import net.fabricmc.loader.api.FabricLoader;
import net.mervyn.mattercannonscaling.MatterCannonScaling;
import net.minecraft.util.Identifier;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ScalingConfig {

    public String _comment = "Matter Cannon Scaling Configuration\n" +
            "Formula: (BaseDamage * damage_multiplier) + damage_additive + attribute_bonuses\n" +
            "damageMultiplier: Multiplier for the base penetration damage (default 1.0)\n" +
            "damageAdditive: Flat damage added after multiplier (default 0.0)\n" +
            "scalingEntries: List of attribute scaling rules.\n" +
            "  - attribute: The attribute ID (e.g. ranged_weapon:damage, minecraft:generic.attack_damage)\n" +
            "  - operation: ADD (flat bonus) or MULTIPLY (adds % of base damage)\n" +
            "  - valueMultiplier: The multiplier for the attribute value";

    public float damageMultiplier = 1.0f;
    public float damageAdditive = 0.0f;
    public List<ScalingEntry> scalingEntries = new ArrayList<>();

    // Internal cache, not serialized
    public transient List<CachedEntry> cachedEntries = new ArrayList<>();

    public record ScalingEntry(String attribute, String operation, float valueMultiplier) {
    }

    public record CachedEntry(Identifier attributeId, Operation op, float multiplier) {
    }

    public enum Operation {
        ADD, MULTIPLY
    }

    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir()
            .resolve("mattercannon-scaling.json");
    private static ScalingConfig INSTANCE;

    private static final com.google.gson.Gson GSON = new com.google.gson.GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    public static synchronized ScalingConfig get() {
        if (INSTANCE == null) {
            load();
        }
        return INSTANCE;
    }

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                INSTANCE = GSON.fromJson(reader, ScalingConfig.class);
            } catch (IOException e) {
                MatterCannonScaling.LOGGER.error("Failed to load config", e);
            }
        }

        if (INSTANCE == null) {
            INSTANCE = new ScalingConfig();
        }

        // Default scaling entry if none configured
        if (INSTANCE.scalingEntries.isEmpty()) {
            INSTANCE.scalingEntries.add(new ScalingEntry("ranged_weapon:damage", "ADD", 1.0f));
        }

        // Ensure comment is always set to latest default when loaded/saved
        INSTANCE._comment = "Matter Cannon Scaling Configuration\n" +
                "Formula: (BaseDamage * damage_multiplier) + damage_additive + attribute_bonuses\n" +
                "damageMultiplier: Multiplier for the base penetration damage (default 1.0)\n" +
                "damageAdditive: Flat damage added after multiplier (default 0.0)\n" +
                "scalingEntries: List of attribute scaling rules.\n" +
                "  - attribute: The attribute ID (e.g. ranged_weapon:damage, minecraft:generic.attack_damage)\n" +
                "  - operation: ADD (flat bonus) or MULTIPLY (adds % of base damage)\n" +
                "  - valueMultiplier: The multiplier for the attribute value";

        INSTANCE.cacheAttributes();

        // Save back to ensure file exists and has defaults/comments
        save();
    }

    private void cacheAttributes() {
        cachedEntries.clear();
        for (ScalingEntry entry : scalingEntries) {
            cacheEntry(entry);
        }
    }

    private void cacheEntry(ScalingEntry entry) {
        try {
            Identifier id = new Identifier(entry.attribute);
            Operation op = Operation.valueOf(entry.operation.toUpperCase());
            cachedEntries.add(new CachedEntry(id, op, entry.valueMultiplier));
        } catch (IllegalArgumentException e) {
            MatterCannonScaling.LOGGER
                    .warn("Invalid operation in config: " + entry.operation + ". Defaulting to ADD.");
            try {
                Identifier id = new Identifier(entry.attribute);
                cachedEntries.add(new CachedEntry(id, Operation.ADD, entry.valueMultiplier));
            } catch (Exception ex) {
                MatterCannonScaling.LOGGER.warn("Invalid attribute ID: " + entry.attribute);
            }
        } catch (Exception e) {
            MatterCannonScaling.LOGGER.warn("Invalid attribute ID in config: '" + entry.attribute + "'. Ignoring.");
        }
    }

    public static void save() {
        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
            GSON.toJson(INSTANCE, writer);
        } catch (IOException e) {
            MatterCannonScaling.LOGGER.error("Failed to save config", e);
        }
    }
}
