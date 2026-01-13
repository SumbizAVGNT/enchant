package com.moonrein.moonEnchant.enchant;

import com.moonrein.moonEnchant.model.EnchantDefinition;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class EnchantRegistry {
    private final Map<String, EnchantDefinition> enchantments = new LinkedHashMap<>();

    public void clear() {
        enchantments.clear();
    }

    public void register(EnchantDefinition definition) {
        enchantments.put(definition.getId().toLowerCase(), definition);
    }

    public Optional<EnchantDefinition> getById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(enchantments.get(id.toLowerCase()));
    }

    public Collection<EnchantDefinition> getAll() {
        return enchantments.values();
    }
}
