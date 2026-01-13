package com.moonrein.moonEnchant.util;

import com.moonrein.moonEnchant.model.AttributeModifierSpec;
import com.moonrein.moonEnchant.model.StackRule;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlot;

public final class AttributeModifierFactory {
    private AttributeModifierFactory() {
    }

    public static AttributeModifierSpec create(String enchantId, String key, Attribute attribute,
                                               double amount, AttributeModifier.Operation operation,
                                               EquipmentSlot slot, StackRule stackRule) {
        String name = "moonEnchant:" + enchantId + ":" + key;
        UUID uuid = UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
        return new AttributeModifierSpec(attribute, amount, operation, slot, stackRule, uuid, name);
    }
}
