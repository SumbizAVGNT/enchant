package com.moonrein.moonEnchant.enchant;

import com.moonrein.moonEnchant.model.AttributeModifierSpec;
import com.moonrein.moonEnchant.model.EffectSpec;
import com.moonrein.moonEnchant.model.EnchantDefinition;
import com.moonrein.moonEnchant.model.EnchantRarity;
import com.moonrein.moonEnchant.model.EnchantTrigger;
import com.moonrein.moonEnchant.model.StackRule;
import com.moonrein.moonEnchant.util.AttributeModifierFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.potion.PotionEffectType;

public class EnchantConfigLoader {

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
        EnchantRarity rarity = EnchantRarity.valueOf(config.getString("rarity", "COMMON").toUpperCase());
        int maxLevel = config.getInt("max-level", 1);
        int weight = config.getInt("weight", 1);
        int enchantSlotCost = config.getInt("enchant-slot-cost", 1);
        Set<EquipmentSlot> slots = EnumSet.noneOf(EquipmentSlot.class);
        for (String slot : config.getStringList("supported-slots")) {
            slots.add(EquipmentSlot.valueOf(slot.toUpperCase()));
        }
        Set<String> conflicts = new HashSet<>(config.getStringList("conflicts"));
        List<AttributeModifierSpec> attributes = loadAttributes(config.getConfigurationSection("attributes"), id);
        List<EffectSpec> effects = loadEffects(config.getConfigurationSection("effects"));
        double heatPerProc = config.getDouble("heat.per-proc", 0.0);
        double heatDecay = config.getDouble("heat.decay-per-second", 0.0);
        double heatMax = config.getDouble("heat.max", 0.0);
        return new EnchantDefinition(id, name, description, rarity, maxLevel, weight, slots, conflicts,
            enchantSlotCost, attributes, effects, heatPerProc, heatDecay, heatMax);
    }

    private List<AttributeModifierSpec> loadAttributes(ConfigurationSection section, String id) {
        List<AttributeModifierSpec> result = new ArrayList<>();
        if (section == null) {
            return result;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(key);
            if (entry == null) {
                continue;
            }
            Attribute attribute = Attribute.valueOf(entry.getString("attribute", "GENERIC_ATTACK_DAMAGE").toUpperCase());
            double amount = entry.getDouble("amount", 0.0);
            AttributeModifier.Operation operation = AttributeModifier.Operation.valueOf(
                entry.getString("operation", "ADD_NUMBER").toUpperCase());
            EquipmentSlot slot = EquipmentSlot.valueOf(entry.getString("slot", "HAND").toUpperCase());
            StackRule stackRule = StackRule.valueOf(entry.getString("stack-rule", "MAX_LEVEL").toUpperCase());
            AttributeModifierSpec spec = AttributeModifierFactory.create(id, key, attribute, amount, operation, slot, stackRule);
            result.add(spec);
        }
        return result;
    }

    private List<EffectSpec> loadEffects(ConfigurationSection section) {
        List<EffectSpec> result = new ArrayList<>();
        if (section == null) {
            return result;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(key);
            if (entry == null) {
                continue;
            }
            EnchantTrigger trigger = EnchantTrigger.valueOf(entry.getString("trigger", "PASSIVE").toUpperCase());
            PotionEffectType type = PotionEffectType.getByName(entry.getString("type", "SPEED"));
            if (type == null) {
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
}
