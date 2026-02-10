package net.mervyn.mattercannonscaling.config;

import net.fabricmc.loader.api.FabricLoader;
import net.mervyn.mattercannonscaling.MatterCannonScaling;
import net.minecraft.util.Identifier;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class ScalingConfig {

    public float damageMultiplier = 1.0f;
    public float damageAdditive = 0.0f;
    public List<ScalingEntry> scalingEntries = new ArrayList<>();

    // Internal cache, not serialized
    public transient List<CachedEntry> cachedEntries = new ArrayList<>();

    public record ScalingEntry(String attribute, String operation, float valueMultiplier) {
        @Override
        public String toString() {
            return attribute + "," + operation + "," + valueMultiplier;
        }

        public static ScalingEntry fromString(String str) {
            String[] parts = str.split(",");
            if (parts.length >= 3) {
                try {
                    return new ScalingEntry(parts[0].trim(), parts[1].trim(), Float.parseFloat(parts[2].trim()));
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        }
    }

    public record CachedEntry(Identifier attributeId, Operation op, float multiplier) {
    }

    public enum Operation {
        ADD, MULTIPLY
    }

    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir()
            .resolve("mattercannon-scaling.properties");
    private static ScalingConfig INSTANCE;

    /**
     * Gets the singleton instance of the ScalingConfig.
     * Thread-safe lazy loading.
     */
    public static synchronized ScalingConfig get() {
        if (INSTANCE == null) {
            load();
        }
        return INSTANCE;
    }

    /**
     * Loads the configuration from the disk.
     */
    public static void load() {
        INSTANCE = new ScalingConfig();
        Properties props = new Properties();

        if (Files.exists(CONFIG_PATH)) {
            try (InputStream in = Files.newInputStream(CONFIG_PATH)) {
                props.load(in);

                INSTANCE.damageMultiplier = parseSafe(props.getProperty("damage_multiplier"), 1.0f);
                INSTANCE.damageAdditive = parseSafe(props.getProperty("damage_additive"), 0.0f);

                String entries = props.getProperty("scaling_entries");
                if (entries != null && !entries.isEmpty()) {
                    for (String entryStr : entries.split(";")) {
                        if (entryStr.trim().isEmpty())
                            continue;
                        try {
                            ScalingEntry entry = ScalingEntry.fromString(entryStr);
                            if (entry != null)
                                INSTANCE.scalingEntries.add(entry);
                        } catch (Exception e) {
                            MatterCannonScaling.LOGGER.error("Failed to parse scaling entry: " + entryStr, e);
                        }
                    }
                }
            } catch (IOException e) {
                MatterCannonScaling.LOGGER.error("Failed to load config", e);
            }
        }

        // Default scaling entry if none configured
        if (INSTANCE.scalingEntries.isEmpty()) {
            INSTANCE.scalingEntries.add(new ScalingEntry("ranged_weapon:damage", "ADD", 1.0f));
        }

        INSTANCE.cacheAttributes();

        // Save back to ensure comments/defaults are written
        save();
    }

    private static float parseSafe(String value, float defaultValue) {
        if (value == null)
            return defaultValue;
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            MatterCannonScaling.LOGGER.warn("Invalid number format in config: '{}'. Using default: {}", value,
                    defaultValue);
            return defaultValue;
        }
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
        Properties props = new Properties();
        props.setProperty("damage_multiplier", String.valueOf(INSTANCE.damageMultiplier));
        props.setProperty("damage_additive", String.valueOf(INSTANCE.damageAdditive));

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < INSTANCE.scalingEntries.size(); i++) {
            sb.append(INSTANCE.scalingEntries.get(i).toString());
            if (i < INSTANCE.scalingEntries.size() - 1) {
                sb.append(";");
            }
        }
        props.setProperty("scaling_entries", sb.toString());

        try (OutputStream out = Files.newOutputStream(CONFIG_PATH)) {
            props.store(out, "Matter Cannon Scaling Configuration\n" +
                    "Formula: (BaseDamage * damage_multiplier) + damage_additive + attribute_bonuses\n" +
                    "damage_multiplier: Multiplier for the base penetration damage (default 1.0)\n" +
                    "damage_additive: Flat damage added after multiplier (default 0.0)\n" +
                    "scaling_entries: Semicolon-separated list of entries in format: attribute,operation,value\n" +
                    "  attribute: The attribute ID (e.g. ranged_weapon:damage, minecraft:generic.attack_damage)\n" +
                    "  operation: ADD or MULTIPLY\n" +
                    "  value: The multiplier for the attribute value\n" +
                    "  Example: ranged_weapon:damage,ADD,1.0;minecraft:generic.attack_damage,MULTIPLY,0.5");
        } catch (IOException e) {
            MatterCannonScaling.LOGGER.error("Failed to save config", e);
        }
    }
}
