package com.moonrein.moonEnchant.config;

import com.moonrein.moonEnchant.model.EnchantRarity;
import com.moonrein.moonEnchant.model.EnchantTableRequirement;
import java.io.File;
import java.util.EnumMap;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public class EnchantSettingsLoader {
    private static final EnchantTableRequirement DEFAULT_REQUIREMENT =
        new EnchantTableRequirement(true, 1, 30, 0);

    public EnchantSettings load(File file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        boolean tableEnabled = config.getBoolean("enchanting-table.enabled", true);
        ConfigurationSection tiers = config.getConfigurationSection("enchanting-table.tiers");
        Map<EnchantRarity, EnchantTableRequirement> requirements = new EnumMap<>(EnchantRarity.class);
        for (EnchantRarity rarity : EnchantRarity.values()) {
            EnchantTableRequirement requirement = readRequirement(tableEnabled, tiers, rarity);
            requirements.put(rarity, requirement);
        }
        return new EnchantSettings(tableEnabled, requirements);
    }

    private EnchantTableRequirement readRequirement(boolean tableEnabled, ConfigurationSection tiers, EnchantRarity rarity) {
        if (tiers == null) {
            return new EnchantTableRequirement(tableEnabled, DEFAULT_REQUIREMENT.minLevel(),
                DEFAULT_REQUIREMENT.maxLevel(), DEFAULT_REQUIREMENT.minBookshelves());
        }
        ConfigurationSection tier = tiers.getConfigurationSection(rarity.name().toLowerCase());
        if (tier == null) {
            return new EnchantTableRequirement(tableEnabled, DEFAULT_REQUIREMENT.minLevel(),
                DEFAULT_REQUIREMENT.maxLevel(), DEFAULT_REQUIREMENT.minBookshelves());
        }
        boolean enabled = tableEnabled && tier.getBoolean("enabled", true);
        int minLevel = tier.getInt("min-level", DEFAULT_REQUIREMENT.minLevel());
        int maxLevel = tier.getInt("max-level", DEFAULT_REQUIREMENT.maxLevel());
        int minBookshelves = tier.getInt("min-bookshelves", DEFAULT_REQUIREMENT.minBookshelves());
        return new EnchantTableRequirement(enabled, minLevel, maxLevel, minBookshelves);
    }
}
