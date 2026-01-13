package com.moonrein.moonEnchant.util;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class ItemEnchantStorage {
    private final NamespacedKey key;

    public ItemEnchantStorage(NamespacedKey key) {
        this.key = key;
    }

    public Map<String, Integer> getEnchantments(ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta()) {
            return Collections.emptyMap();
        }
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return Collections.emptyMap();
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        String raw = container.get(key, PersistentDataType.STRING);
        if (raw == null || raw.isBlank()) {
            return Collections.emptyMap();
        }
        Map<String, Integer> result = new LinkedHashMap<>();
        for (String entry : raw.split(";")) {
            String[] parts = entry.split(":");
            if (parts.length != 2) {
                continue;
            }
            try {
                int level = Integer.parseInt(parts[1]);
                result.put(parts[0], level);
            } catch (NumberFormatException ignored) {
                continue;
            }
        }
        return result;
    }

    public void setEnchantments(ItemStack itemStack, Map<String, Integer> enchantments) {
        if (itemStack == null) {
            return;
        }
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        if (enchantments.isEmpty()) {
            container.remove(key);
        } else {
            String value = enchantments.entrySet().stream()
                .map(entry -> entry.getKey() + ":" + entry.getValue())
                .collect(Collectors.joining(";"));
            container.set(key, PersistentDataType.STRING, value);
        }
        itemStack.setItemMeta(meta);
    }
}
