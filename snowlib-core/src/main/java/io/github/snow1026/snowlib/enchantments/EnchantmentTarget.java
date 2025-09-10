package io.github.snow1026.snowlib.enchantments;

import org.bukkit.inventory.EquipmentSlot;

public enum EnchantmentTarget {

    ARMOR(org.bukkit.enchantments.EnchantmentTarget.ARMOR, new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}),
    ARMOR_FEET(org.bukkit.enchantments.EnchantmentTarget.ARMOR_FEET, new EquipmentSlot[]{EquipmentSlot.FEET}),
    ARMOR_LEGS(org.bukkit.enchantments.EnchantmentTarget.ARMOR_LEGS, new EquipmentSlot[]{EquipmentSlot.LEGS}),
    ARMOR_TORSO(org.bukkit.enchantments.EnchantmentTarget.ARMOR_TORSO, new EquipmentSlot[]{EquipmentSlot.CHEST}),
    ARMOR_HEAD(org.bukkit.enchantments.EnchantmentTarget.ARMOR_HEAD, new EquipmentSlot[]{EquipmentSlot.HEAD}),
    WEAPON(org.bukkit.enchantments.EnchantmentTarget.WEAPON, new EquipmentSlot[]{EquipmentSlot.HAND, EquipmentSlot.OFF_HAND}),
    TOOL(org.bukkit.enchantments.EnchantmentTarget.TOOL, new EquipmentSlot[]{EquipmentSlot.HAND}),
    BOW(org.bukkit.enchantments.EnchantmentTarget.BOW, new EquipmentSlot[]{EquipmentSlot.HAND}),
    FISHING_ROD(org.bukkit.enchantments.EnchantmentTarget.FISHING_ROD, new EquipmentSlot[]{EquipmentSlot.HAND}),
    BREAKABLE(org.bukkit.enchantments.EnchantmentTarget.BREAKABLE, EquipmentSlot.values()),
    WEARABLE(org.bukkit.enchantments.EnchantmentTarget.WEARABLE, EquipmentSlot.values()),
    TRIDENT(org.bukkit.enchantments.EnchantmentTarget.TRIDENT, new EquipmentSlot[]{EquipmentSlot.HAND}),
    CROSSBOW(org.bukkit.enchantments.EnchantmentTarget.CROSSBOW, new EquipmentSlot[]{EquipmentSlot.HAND}),
    VANISHABLE(org.bukkit.enchantments.EnchantmentTarget.VANISHABLE, EquipmentSlot.values());

    private final org.bukkit.enchantments.EnchantmentTarget bukkitTarget;
    private final EquipmentSlot[] slots;

    /**
     * Bukkit EnchantmentTarget + 해당 슬롯들
     */
    EnchantmentTarget(org.bukkit.enchantments.EnchantmentTarget bukkitTarget, EquipmentSlot[] slots) {
        this.bukkitTarget = bukkitTarget;
        this.slots = slots;
    }

    public org.bukkit.enchantments.EnchantmentTarget getBukkitTarget() {
        return bukkitTarget;
    }

    public EquipmentSlot[] getApplicableSlots() {
        return slots;
    }
}
