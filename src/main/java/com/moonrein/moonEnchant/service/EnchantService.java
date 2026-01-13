package com.moonrein.moonEnchant.service;

import com.moonrein.moonEnchant.enchant.EnchantRegistry;
import com.moonrein.moonEnchant.model.AttributeModifierSpec;
import com.moonrein.moonEnchant.model.EffectSpec;
import com.moonrein.moonEnchant.model.EnchantDefinition;
import com.moonrein.moonEnchant.model.EnchantTrigger;
import com.moonrein.moonEnchant.model.StackRule;
import com.moonrein.moonEnchant.util.ItemEnchantStorage;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

public class EnchantService {
    private final EnchantRegistry registry;
    private final ItemEnchantStorage storage;
    private final Map<UUID, PlayerEnchantState> playerState = new HashMap<>();
    private final Map<UUID, Boolean> debugEnabled = new HashMap<>();

    public EnchantService(EnchantRegistry registry, ItemEnchantStorage storage) {
        this.registry = registry;
        this.storage = storage;
    }

    public void refreshPlayer(Player player) {
        PlayerEnchantState state = playerState.computeIfAbsent(player.getUniqueId(), id -> new PlayerEnchantState());
        state.setEquippedEnchantLevels(scanEquippedEnchantments(player));
        removeExistingModifiers(player);
        applyAttributeModifiers(player, state.getEquippedEnchantLevels());
    }

    public void removePlayer(Player player) {
        playerState.remove(player.getUniqueId());
        debugEnabled.remove(player.getUniqueId());
        removeExistingModifiers(player);
    }

    public void tickPassiveEffects() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerEnchantState state = playerState.computeIfAbsent(player.getUniqueId(), id -> new PlayerEnchantState());
            applyTrigger(player, state, EnchantTrigger.PASSIVE, null);
            for (String id : state.getEquippedEnchantLevels().keySet()) {
                registry.getById(id).ifPresent(state::coolHeat);
            }
        }
    }

    public void handleHit(Player player, LivingEntity target) {
        PlayerEnchantState state = playerState.computeIfAbsent(player.getUniqueId(), id -> new PlayerEnchantState());
        applyTrigger(player, state, EnchantTrigger.ON_HIT, target);
    }

    public void handleTakeDamage(Player player, LivingEntity source) {
        PlayerEnchantState state = playerState.computeIfAbsent(player.getUniqueId(), id -> new PlayerEnchantState());
        applyTrigger(player, state, EnchantTrigger.ON_TAKE_DAMAGE, source);
    }

    public void toggleDebug(Player player) {
        boolean next = !debugEnabled.getOrDefault(player.getUniqueId(), false);
        debugEnabled.put(player.getUniqueId(), next);
        player.sendMessage("MoonEnchant debug: " + (next ? "ON" : "OFF"));
    }

    public Optional<EnchantDefinition> getDefinition(String id) {
        return registry.getById(id);
    }

    public Map<String, Integer> getItemEnchantments(ItemStack itemStack) {
        return storage.getEnchantments(itemStack);
    }

    public void setItemEnchantments(ItemStack itemStack, Map<String, Integer> enchantments) {
        storage.setEnchantments(itemStack, enchantments);
    }

    private void applyTrigger(Player player, PlayerEnchantState state, EnchantTrigger trigger, LivingEntity target) {
        Map<String, List<EnchantLevelSlot>> enchantLevels = state.getEquippedEnchantLevels();
        for (Map.Entry<String, List<EnchantLevelSlot>> entry : enchantLevels.entrySet()) {
            EnchantDefinition definition = registry.getById(entry.getKey()).orElse(null);
            if (definition == null) {
                continue;
            }
            for (EffectSpec effect : definition.getEffects()) {
                if (effect.getTrigger() != trigger) {
                    continue;
                }
                if (isOnCooldown(state, definition.getId(), effect)) {
                    debug(player, definition, trigger, "cooldown");
                    continue;
                }
                double chance = applyHeatScaling(state, definition, effect.getChance());
                if (ThreadLocalRandom.current().nextDouble() > chance) {
                    debug(player, definition, trigger, "chance");
                    continue;
                }
                LivingEntity recipient = trigger == EnchantTrigger.ON_HIT && target != null ? target : player;
                PotionEffect potionEffect = new PotionEffect(effect.getType(), effect.getDurationTicks(),
                    effect.getAmplifier(), effect.isAmbient(), effect.hasParticles(), effect.hasIcon());
                recipient.addPotionEffect(potionEffect, true);
                state.setCooldown(definition.getId(), effect.getType().getName().toLowerCase(Locale.ROOT), effect.getCooldownTicks());
                state.addHeat(definition);
                debug(player, definition, trigger, "applied");
            }
        }
    }

    private void applyAttributeModifiers(Player player, Map<String, List<EnchantLevelSlot>> enchantLevels) {
        for (Map.Entry<String, List<EnchantLevelSlot>> entry : enchantLevels.entrySet()) {
            EnchantDefinition definition = registry.getById(entry.getKey()).orElse(null);
            if (definition == null) {
                continue;
            }
            for (AttributeModifierSpec spec : definition.getAttributeModifiers()) {
                int effectiveLevel = resolveLevel(spec.getStackRule(), entry.getValue());
                if (effectiveLevel <= 0) {
                    continue;
                }
                AttributeInstance instance = player.getAttribute(spec.getAttribute());
                if (instance == null) {
                    continue;
                }
                double amount = spec.getAmount() * effectiveLevel;
                AttributeModifier modifier = new AttributeModifier(spec.getUuid(), spec.getName(), amount, spec.getOperation(), spec.getSlot());
                instance.addModifier(modifier);
            }
        }
    }

    private void removeExistingModifiers(Player player) {
        for (Attribute attribute : Attribute.values()) {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null) {
                continue;
            }
            List<AttributeModifier> toRemove = new ArrayList<>();
            for (AttributeModifier modifier : instance.getModifiers()) {
                if (modifier.getName() != null && modifier.getName().startsWith("moonEnchant:")) {
                    toRemove.add(modifier);
                }
            }
            for (AttributeModifier modifier : toRemove) {
                instance.removeModifier(modifier);
            }
        }
    }

    private int resolveLevel(StackRule rule, List<EnchantLevelSlot> levels) {
        if (levels.isEmpty()) {
            return 0;
        }
        return switch (rule) {
            case STACK_PER_SLOT -> levels.stream().mapToInt(EnchantLevelSlot::level).sum();
            case NO_STACK, MAX_LEVEL -> levels.stream().mapToInt(EnchantLevelSlot::level).max().orElse(0);
        };
    }

    private Map<String, List<EnchantLevelSlot>> scanEquippedEnchantments(Player player) {
        Map<String, List<EnchantLevelSlot>> result = new HashMap<>();
        Map<EquipmentSlot, ItemStack> items = new EnumMap<>(EquipmentSlot.class);
        items.put(EquipmentSlot.HAND, player.getInventory().getItemInMainHand());
        items.put(EquipmentSlot.OFF_HAND, player.getInventory().getItemInOffHand());
        items.put(EquipmentSlot.HEAD, player.getInventory().getHelmet());
        items.put(EquipmentSlot.CHEST, player.getInventory().getChestplate());
        items.put(EquipmentSlot.LEGS, player.getInventory().getLeggings());
        items.put(EquipmentSlot.FEET, player.getInventory().getBoots());

        for (Map.Entry<EquipmentSlot, ItemStack> entry : items.entrySet()) {
            Map<String, Integer> enchants = storage.getEnchantments(entry.getValue());
            for (Map.Entry<String, Integer> enchantEntry : enchants.entrySet()) {
                result.computeIfAbsent(enchantEntry.getKey(), key -> new ArrayList<>())
                    .add(new EnchantLevelSlot(enchantEntry.getValue(), entry.getKey()));
            }
        }
        return result;
    }

    private boolean isOnCooldown(PlayerEnchantState state, String enchantId, EffectSpec effect) {
        String key = enchantId + ":" + effect.getType().getName().toLowerCase(Locale.ROOT);
        return state.isOnCooldown(key);
    }

    private double applyHeatScaling(PlayerEnchantState state, EnchantDefinition definition, double baseChance) {
        double max = definition.getHeatMax();
        if (max <= 0) {
            return baseChance;
        }
        double heat = state.getHeat(definition.getId());
        double factor = Math.max(0.2, 1.0 - (heat / max));
        return baseChance * factor;
    }

    private void debug(Player player, EnchantDefinition definition, EnchantTrigger trigger, String reason) {
        if (!debugEnabled.getOrDefault(player.getUniqueId(), false)) {
            return;
        }
        player.sendActionBar("§7[CE] " + definition.getName() + " §8" + trigger + " §7" + reason);
    }

    
}
