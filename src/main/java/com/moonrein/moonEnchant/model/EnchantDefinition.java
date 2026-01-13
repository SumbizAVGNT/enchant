package com.moonrein.moonEnchant.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.bukkit.inventory.EquipmentSlot;

public class EnchantDefinition {
    private final String id;
    private final String name;
    private final List<String> description;
    private final EnchantRarity rarity;
    private final int maxLevel;
    private final int weight;
    private final Set<EquipmentSlot> supportedSlots;
    private final Set<String> conflicts;
    private final int enchantSlotCost;
    private final List<AttributeModifierSpec> attributeModifiers;
    private final List<EffectSpec> effects;
    private final double heatPerProc;
    private final double heatDecayPerSecond;
    private final double heatMax;

    public EnchantDefinition(String id, String name, List<String> description, EnchantRarity rarity,
                              int maxLevel, int weight, Set<EquipmentSlot> supportedSlots,
                              Set<String> conflicts, int enchantSlotCost,
                              List<AttributeModifierSpec> attributeModifiers, List<EffectSpec> effects,
                              double heatPerProc, double heatDecayPerSecond, double heatMax) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.description = List.copyOf(description);
        this.rarity = Objects.requireNonNull(rarity, "rarity");
        this.maxLevel = maxLevel;
        this.weight = weight;
        this.supportedSlots = Set.copyOf(supportedSlots);
        this.conflicts = Set.copyOf(conflicts);
        this.enchantSlotCost = enchantSlotCost;
        this.attributeModifiers = List.copyOf(attributeModifiers);
        this.effects = List.copyOf(effects);
        this.heatPerProc = heatPerProc;
        this.heatDecayPerSecond = heatDecayPerSecond;
        this.heatMax = heatMax;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<String> getDescription() {
        return Collections.unmodifiableList(description);
    }

    public EnchantRarity getRarity() {
        return rarity;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public int getWeight() {
        return weight;
    }

    public Set<EquipmentSlot> getSupportedSlots() {
        return Collections.unmodifiableSet(supportedSlots);
    }

    public Set<String> getConflicts() {
        return Collections.unmodifiableSet(conflicts);
    }

    public int getEnchantSlotCost() {
        return enchantSlotCost;
    }

    public List<AttributeModifierSpec> getAttributeModifiers() {
        return Collections.unmodifiableList(attributeModifiers);
    }

    public List<EffectSpec> getEffects() {
        return Collections.unmodifiableList(effects);
    }

    public double getHeatPerProc() {
        return heatPerProc;
    }

    public double getHeatDecayPerSecond() {
        return heatDecayPerSecond;
    }

    public double getHeatMax() {
        return heatMax;
    }
}
