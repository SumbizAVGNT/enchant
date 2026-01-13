package com.moonrein.moonEnchant.service;

import com.moonrein.moonEnchant.model.EnchantDefinition;
import java.util.HashMap;
import java.util.Map;

public class PlayerEnchantState {
    private final Map<String, Long> cooldowns = new HashMap<>();
    private final Map<String, Double> heat = new HashMap<>();
    private Map<String, java.util.List<EnchantLevelSlot>> equippedEnchantLevels = new HashMap<>();

    public Map<String, java.util.List<EnchantLevelSlot>> getEquippedEnchantLevels() {
        return equippedEnchantLevels;
    }

    public void setEquippedEnchantLevels(Map<String, java.util.List<EnchantLevelSlot>> equippedEnchantLevels) {
        this.equippedEnchantLevels = equippedEnchantLevels;
    }

    public Map<String, Long> getCooldownSnapshot() {
        return new HashMap<>(cooldowns);
    }

    public Map<String, Double> getHeatSnapshot() {
        return new HashMap<>(heat);
    }

    public boolean isOnCooldown(String key) {
        Long until = cooldowns.get(key);
        return until != null && until > System.currentTimeMillis();
    }

    public void setCooldown(String enchantId, String effectKey, long cooldownTicks) {
        if (cooldownTicks <= 0) {
            return;
        }
        long until = System.currentTimeMillis() + (cooldownTicks * 50L);
        cooldowns.put(enchantId + ":" + effectKey, until);
    }

    public double getHeat(String enchantId) {
        return heat.getOrDefault(enchantId, 0.0);
    }

    public void addHeat(EnchantDefinition definition) {
        if (definition.getHeatPerProc() <= 0) {
            return;
        }
        heat.merge(definition.getId(), definition.getHeatPerProc(), Double::sum);
    }

    public void coolHeat(EnchantDefinition definition) {
        double decayPerSecond = definition.getHeatDecayPerSecond();
        if (decayPerSecond <= 0) {
            return;
        }
        heat.computeIfPresent(definition.getId(), (key, value) -> {
            double next = value - (decayPerSecond / 20.0);
            return next <= 0 ? 0.0 : next;
        });
    }
}
