package com.moonrein.moonEnchant.model;

import java.util.Objects;
import java.util.UUID;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlot;

public class AttributeModifierSpec {
    private final Attribute attribute;
    private final double amount;
    private final AttributeModifier.Operation operation;
    private final EquipmentSlot slot;
    private final StackRule stackRule;
    private final UUID uuid;
    private final String name;

    public AttributeModifierSpec(Attribute attribute, double amount, AttributeModifier.Operation operation,
                                 EquipmentSlot slot, StackRule stackRule, UUID uuid, String name) {
        this.attribute = Objects.requireNonNull(attribute, "attribute");
        this.amount = amount;
        this.operation = Objects.requireNonNull(operation, "operation");
        this.slot = Objects.requireNonNull(slot, "slot");
        this.stackRule = Objects.requireNonNull(stackRule, "stackRule");
        this.uuid = Objects.requireNonNull(uuid, "uuid");
        this.name = Objects.requireNonNull(name, "name");
    }

    public Attribute getAttribute() {
        return attribute;
    }

    public double getAmount() {
        return amount;
    }

    public AttributeModifier.Operation getOperation() {
        return operation;
    }

    public EquipmentSlot getSlot() {
        return slot;
    }

    public StackRule getStackRule() {
        return stackRule;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }
}
