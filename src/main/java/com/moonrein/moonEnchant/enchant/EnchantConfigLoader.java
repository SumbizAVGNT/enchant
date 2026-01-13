package com.moonrein.moonEnchant.enchant;

import com.moonrein.moonEnchant.config.EnchantSettings;
import com.moonrein.moonEnchant.model.AttributeModifierSpec;
import com.moonrein.moonEnchant.model.EffectSpec;
import com.moonrein.moonEnchant.model.EnchantDefinition;
import com.moonrein.moonEnchant.model.EnchantRarity;
import com.moonrein.moonEnchant.model.EnchantTableRequirement;
import com.moonrein.moonEnchant.model.EnchantTrigger;
import com.moonrein.moonEnchant.model.StackRule;
import com.moonrein.moonEnchant.util.AttributeModifierFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.potion.PotionEffectType;

public class EnchantConfigLoader {
    private final EnchantSettings settings;
    private final Logger logger;

    public EnchantConfigLoader(EnchantSettings settings, Logger logger) {
        this.settings = settings;
        this.logger = logger;
    }

    public List<EnchantDefinition> loadAll(File folder) {
        List<EnchantDefinition> result = new ArrayList<>();
        if (!folder.exists() && !folder.mkdirs()) {
            return result;
        }
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return result;
        }
        for (File file : files) {
            result.add(load(file));
        }
        return result;
    }

    private EnchantDefinition load(File file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        String id = config.getString("id", file.getName().replace(".yml", ""));
        String name = config.getString("name", id);
        List<String> description = config.getStringList("description");
        EnchantRarity rarity = readEnum(file, "rarity", config.getString("rarity", "COMMON"),
            EnchantRarity.class, EnchantRarity.COMMON);
        int maxLevel = readPositiveInt(file, "max-level", config.getInt("max-level", 1), 1);
        int weight = readPositiveInt(file, "weight", config.getInt("weight", 1), 1);
        int enchantSlotCost = readPositiveInt(file, "enchant-slot-cost", config.getInt("enchant-slot-cost", 1), 1);
        Set<EquipmentSlot> slots = EnumSet.noneOf(EquipmentSlot.class);
        for (String slot : config.getStringList("supported-slots")) {
            EquipmentSlot resolved = readEnum(file, "supported-slots", slot, EquipmentSlot.class, null);
            if (resolved != null) {
                slots.add(resolved);
            }
        }
        Set<String> conflicts = new HashSet<>(config.getStringList("conflicts"));
        List<AttributeModifierSpec> attributes = loadAttributes(file, config.getConfigurationSection("attributes"), id);
        List<EffectSpec> effects = loadEffects(file, config.getConfigurationSection("effects"));
        double heatPerProc = config.getDouble("heat.per-proc", 0.0);
        double heatDecay = config.getDouble("heat.decay-per-second", 0.0);
        double heatMax = config.getDouble("heat.max", 0.0);
        EnchantTableRequirement tableRequirement = loadTableRequirement(config, rarity);
        return new EnchantDefinition(id, name, description, rarity, maxLevel, weight, slots, conflicts,
            enchantSlotCost, attributes, effects, heatPerProc, heatDecay, heatMax, tableRequirement);
    }

    private List<AttributeModifierSpec> loadAttributes(File file, ConfigurationSection section, String id) {
        List<AttributeModifierSpec> result = new ArrayList<>();
        if (section == null) {
            return result;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(key);
            if (entry == null) {
                continue;
            }
            Attribute attribute = readEnum(file, "attributes." + key + ".attribute",
                entry.getString("attribute", "GENERIC_ATTACK_DAMAGE"), Attribute.class, Attribute.GENERIC_ATTACK_DAMAGE);
            double amount = entry.getDouble("amount", 0.0);
            AttributeModifier.Operation operation = readEnum(file, "attributes." + key + ".operation",
                entry.getString("operation", "ADD_NUMBER"), AttributeModifier.Operation.class,
                AttributeModifier.Operation.ADD_NUMBER);
            EquipmentSlot slot = readEnum(file, "attributes." + key + ".slot",
                entry.getString("slot", "HAND"), EquipmentSlot.class, EquipmentSlot.HAND);
            StackRule stackRule = readEnum(file, "attributes." + key + ".stack-rule",
                entry.getString("stack-rule", "MAX_LEVEL"), StackRule.class, StackRule.MAX_LEVEL);
            AttributeModifierSpec spec = AttributeModifierFactory.create(id, key, attribute, amount, operation, slot, stackRule);
            result.add(spec);
        }
        return result;
    }

    private List<EffectSpec> loadEffects(File file, ConfigurationSection section) {
        List<EffectSpec> result = new ArrayList<>();
        if (section == null) {
            return result;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(key);
            if (entry == null) {
                continue;
            }
            EnchantTrigger trigger = readEnum(file, "effects." + key + ".trigger",
                entry.getString("trigger", "PASSIVE"), EnchantTrigger.class, EnchantTrigger.PASSIVE);
            String typeName = entry.getString("type", "SPEED");
            PotionEffectType type = PotionEffectType.getByName(typeName);
            if (type == null) {
                warn(file, "effects." + key + ".type", "Unknown potion effect: " + typeName);
                continue;
            }
            int amplifier = entry.getInt("amplifier", 0);
            int duration = entry.getInt("duration", 40);
            double chance = entry.getDouble("chance", 1.0);
            long cooldown = entry.getLong("cooldown", 0L);
            boolean ambient = entry.getBoolean("ambient", true);
            boolean particles = entry.getBoolean("particles", true);
            boolean icon = entry.getBoolean("icon", true);
            result.add(new EffectSpec(trigger, type, amplifier, duration, chance, cooldown, ambient, particles, icon));
        }
        return result;
    }

    private EnchantTableRequirement loadTableRequirement(YamlConfiguration config, EnchantRarity rarity) {
        EnchantTableRequirement base = settings.getTableRequirement(rarity);
        ConfigurationSection section = config.getConfigurationSection("table");
        if (section == null) {
            return base;
        }
        boolean enabled = settings.isEnchantingTableEnabled() && section.getBoolean("enabled", base.enabled());
        int minLevel = section.getInt("min-level", base.minLevel());
        int maxLevel = section.getInt("max-level", base.maxLevel());
        int minBookshelves = section.getInt("min-bookshelves", base.minBookshelves());
        return new EnchantTableRequirement(enabled, minLevel, maxLevel, minBookshelves);
    }

    private int readPositiveInt(File file, String path, int value, int fallback) {
        if (value < 1) {
            warn(file, path, "Expected a positive integer, got: " + value);
            return fallback;
        }
        return value;
    }

    private <T extends Enum<T>> T readEnum(File file, String path, String value, Class<T> type, T fallback) {
        if (value == null) {
            warn(file, path, "Missing value, expected one of " + enumOptions(type));
            return fallback;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            warn(file, path, "Invalid value '" + value + "', expected one of " + enumOptions(type));
            return fallback;
        }
    }

    private String enumOptions(Class<? extends Enum<?>> type) {
        Enum<?>[] values = type.getEnumConstants();
        if (values == null) {
            return "[]";
        }
        List<String> names = new ArrayList<>();
        for (Enum<?> value : values) {
            names.add(value.name());
        }
        return names.toString();
    }

    private void warn(File file, String path, String message) {
        if (logger == null) {
            return;
        }
        logger.warning("Enchant config issue in " + file.getName() + " -> " + path + ": " + message);
    }
}
