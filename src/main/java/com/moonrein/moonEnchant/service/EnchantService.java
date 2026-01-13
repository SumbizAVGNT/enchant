package com.moonrein.moonEnchant.service;

import com.moonrein.moonEnchant.enchant.EnchantRegistry;
import com.moonrein.moonEnchant.model.AttributeModifierSpec;
import com.moonrein.moonEnchant.model.EffectRecipient;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
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
import org.bukkit.plugin.java.JavaPlugin;

public class EnchantService {
    private final JavaPlugin plugin;
    private final EnchantRegistry registry;
    private final ItemEnchantStorage storage;
    private final ExecutorService executor;
    private final AtomicBoolean passiveTickRunning = new AtomicBoolean(false);
    private final Map<UUID, PlayerEnchantState> playerState = new HashMap<>();
    private final Map<UUID, Boolean> debugEnabled = new HashMap<>();

    public EnchantService(JavaPlugin plugin, EnchantRegistry registry, ItemEnchantStorage storage, ExecutorService executor) {
        this.plugin = plugin;
        this.registry = registry;
        this.storage = storage;
        this.executor = executor;
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
        if (!passiveTickRunning.compareAndSet(false, true)) {
            return;
        }
        List<PlayerSnapshot> snapshots = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            snapshots.add(snapshotPlayer(player));
        }
        executor.execute(() -> {
            List<TriggerComputation> results = new ArrayList<>();
            for (PlayerSnapshot snapshot : snapshots) {
                results.add(computeTrigger(snapshot, EnchantTrigger.PASSIVE, null));
            }
            if (!plugin.isEnabled()) {
                passiveTickRunning.set(false);
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    for (TriggerComputation result : results) {
                        applyTriggerResult(result);
                        coolHeat(result.playerId());
                    }
                } finally {
                    passiveTickRunning.set(false);
                }
            });
        });
    }

    public void handleHit(Player player, LivingEntity target) {
        scheduleTrigger(player, target, EnchantTrigger.ON_HIT);
    }

    public void handleTakeDamage(Player player, LivingEntity source) {
        scheduleTrigger(player, source, EnchantTrigger.ON_TAKE_DAMAGE);
    }

    public void handleMine(Player player) {
        scheduleTrigger(player, null, EnchantTrigger.ON_MINE);
    }

    public void handleFish(Player player) {
        scheduleTrigger(player, null, EnchantTrigger.ON_FISH);
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

    private void scheduleTrigger(Player player, LivingEntity target, EnchantTrigger trigger) {
        PlayerSnapshot snapshot = snapshotPlayer(player);
        UUID targetId = target != null ? target.getUniqueId() : null;
        executor.execute(() -> {
            TriggerComputation result = computeTrigger(snapshot, trigger, targetId);
            if (!plugin.isEnabled()) {
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> applyTriggerResult(result));
        });
    }

    private TriggerComputation computeTrigger(PlayerSnapshot snapshot, EnchantTrigger trigger, UUID targetId) {
        List<EffectApplication> applications = new ArrayList<>();
        List<DebugRecord> debugRecords = snapshot.debugEnabled() ? new ArrayList<>() : List.of();
        for (Map.Entry<String, List<EnchantLevelSlot>> entry : snapshot.enchantLevels().entrySet()) {
            EnchantDefinition definition = registry.getById(entry.getKey()).orElse(null);
            if (definition == null) {
                continue;
            }
            for (EffectSpec effect : definition.getEffects()) {
                if (effect.getTrigger() != trigger) {
                    continue;
                }
                if (isOnCooldown(snapshot, definition.getId(), effect)) {
                    if (snapshot.debugEnabled()) {
                        debugRecords.add(new DebugRecord(definition, trigger, "cooldown"));
                    }
                    continue;
                }
                double chance = applyHeatScaling(snapshot, definition, effect.getChance());
                if (ThreadLocalRandom.current().nextDouble() > chance) {
                    if (snapshot.debugEnabled()) {
                        debugRecords.add(new DebugRecord(definition, trigger, "chance"));
                    }
                    continue;
                }
                applications.add(new EffectApplication(definition, effect));
                if (snapshot.debugEnabled()) {
                    debugRecords.add(new DebugRecord(definition, trigger, "applied"));
                }
            }
        }
        return new TriggerComputation(snapshot.playerId(), targetId, trigger, applications, debugRecords);
    }

    private void applyTriggerResult(TriggerComputation result) {
        Player player = Bukkit.getPlayer(result.playerId());
        if (player == null) {
            return;
        }
        PlayerEnchantState state = playerState.get(result.playerId());
        if (state == null) {
            return;
        }
        LivingEntity target = null;
        if (result.targetId() != null) {
            var entity = Bukkit.getEntity(result.targetId());
            if (entity instanceof LivingEntity living) {
                target = living;
            }
        }
        for (EffectApplication application : result.applications()) {
            EffectSpec effect = application.effect();
            LivingEntity recipient = effect.getRecipient() == EffectRecipient.TARGET ? target : player;
            if (recipient == null) {
                continue;
            }
            PotionEffect potionEffect = new PotionEffect(effect.getType(), effect.getDurationTicks(),
                effect.getAmplifier(), effect.isAmbient(), effect.hasParticles(), effect.hasIcon());
            recipient.addPotionEffect(potionEffect, true);
            state.setCooldown(application.definition().getId(),
                effect.getType().getName().toLowerCase(Locale.ROOT),
                effect.getCooldownTicks());
            state.addHeat(application.definition());
        }
        if (debugEnabled.getOrDefault(player.getUniqueId(), false)) {
            for (DebugRecord record : result.debugRecords()) {
                debug(player, record.definition(), record.trigger(), record.reason());
            }
        }
    }

    private void coolHeat(UUID playerId) {
        PlayerEnchantState state = playerState.get(playerId);
        if (state == null) {
            return;
        }
        for (String id : state.getEquippedEnchantLevels().keySet()) {
            registry.getById(id).ifPresent(state::coolHeat);
        }
    }

    private boolean isOnCooldown(PlayerSnapshot snapshot, String enchantId, EffectSpec effect) {
        String key = enchantId + ":" + effect.getType().getName().toLowerCase(Locale.ROOT);
        Long until = snapshot.cooldowns().get(key);
        return until != null && until > System.currentTimeMillis();
    }

    private double applyHeatScaling(PlayerSnapshot snapshot, EnchantDefinition definition, double baseChance) {
        double max = definition.getHeatMax();
        if (max <= 0) {
            return baseChance;
        }
        double heat = snapshot.heat().getOrDefault(definition.getId(), 0.0);
        double factor = Math.max(0.2, 1.0 - (heat / max));
        return baseChance * factor;
    }

    private void debug(Player player, EnchantDefinition definition, EnchantTrigger trigger, String reason) {
        if (!debugEnabled.getOrDefault(player.getUniqueId(), false)) {
            return;
        }
        player.sendActionBar("§7[CE] " + definition.getName() + " §8" + trigger + " §7" + reason);
    }

    private PlayerSnapshot snapshotPlayer(Player player) {
        PlayerEnchantState state = playerState.computeIfAbsent(player.getUniqueId(), id -> new PlayerEnchantState());
        Map<String, List<EnchantLevelSlot>> enchantLevels = new HashMap<>();
        for (Map.Entry<String, List<EnchantLevelSlot>> entry : state.getEquippedEnchantLevels().entrySet()) {
            enchantLevels.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return new PlayerSnapshot(
            player.getUniqueId(),
            enchantLevels,
            state.getCooldownSnapshot(),
            state.getHeatSnapshot(),
            debugEnabled.getOrDefault(player.getUniqueId(), false)
        );
    }

    private record PlayerSnapshot(
        UUID playerId,
        Map<String, List<EnchantLevelSlot>> enchantLevels,
        Map<String, Long> cooldowns,
        Map<String, Double> heat,
        boolean debugEnabled
    ) {
    }

    private record EffectApplication(EnchantDefinition definition, EffectSpec effect) {
    }

    private record DebugRecord(EnchantDefinition definition, EnchantTrigger trigger, String reason) {
    }

    private record TriggerComputation(
        UUID playerId,
        UUID targetId,
        EnchantTrigger trigger,
        List<EffectApplication> applications,
        List<DebugRecord> debugRecords
    ) {
    }
}
