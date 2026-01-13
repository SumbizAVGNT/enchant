package com.moonrein.moonEnchant.model;

import java.util.List;
import java.util.OptionalDouble;

public record EnchantLevelConfig(OptionalDouble chance, List<String> effects) {
    public EnchantLevelConfig {
        effects = List.copyOf(effects);
    }
}
