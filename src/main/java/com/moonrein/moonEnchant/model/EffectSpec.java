package com.moonrein.moonEnchant.model;

import java.util.Objects;
import org.bukkit.potion.PotionEffectType;

public class EffectSpec {
    private final EnchantTrigger trigger;
    private final PotionEffectType type;
    private final int amplifier;
    private final int durationTicks;
    private final double chance;
    private final long cooldownTicks;
    private final boolean ambient;
    private final boolean particles;
    private final boolean icon;

    public EffectSpec(EnchantTrigger trigger, PotionEffectType type, int amplifier, int durationTicks,
                      double chance, long cooldownTicks, boolean ambient, boolean particles, boolean icon) {
        this.trigger = Objects.requireNonNull(trigger, "trigger");
        this.type = Objects.requireNonNull(type, "type");
        this.amplifier = amplifier;
        this.durationTicks = durationTicks;
        this.chance = chance;
        this.cooldownTicks = cooldownTicks;
        this.ambient = ambient;
        this.particles = particles;
        this.icon = icon;
    }

    public EnchantTrigger getTrigger() {
        return trigger;
    }

    public PotionEffectType getType() {
        return type;
    }

    public int getAmplifier() {
        return amplifier;
    }

    public int getDurationTicks() {
        return durationTicks;
    }

    public double getChance() {
        return chance;
    }

    public long getCooldownTicks() {
        return cooldownTicks;
    }

    public boolean isAmbient() {
        return ambient;
    }

    public boolean hasParticles() {
        return particles;
    }

    public boolean hasIcon() {
        return icon;
    }
}
