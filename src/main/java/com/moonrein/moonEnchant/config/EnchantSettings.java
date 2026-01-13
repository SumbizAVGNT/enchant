package com.moonrein.moonEnchant.config;

import com.moonrein.moonEnchant.model.EnchantRarity;
import com.moonrein.moonEnchant.model.EnchantTableRequirement;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public class EnchantSettings {
    private final boolean enchantingTableEnabled;
    private final Map<EnchantRarity, EnchantTableRequirement> tableRequirements;

    public EnchantSettings(boolean enchantingTableEnabled, Map<EnchantRarity, EnchantTableRequirement> tableRequirements) {
        this.enchantingTableEnabled = enchantingTableEnabled;
        this.tableRequirements = new EnumMap<>(Objects.requireNonNull(tableRequirements, "tableRequirements"));
    }

    public boolean isEnchantingTableEnabled() {
        return enchantingTableEnabled;
    }

    public EnchantTableRequirement getTableRequirement(EnchantRarity rarity) {
        EnchantTableRequirement requirement = tableRequirements.get(rarity);
        if (requirement != null) {
            return requirement;
        }
        return new EnchantTableRequirement(enchantingTableEnabled, 1, 30, 0);
    }
}
