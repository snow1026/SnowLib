package io.github.snow1026.snowlib.utils;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * InventoryUtils
 *
 * <p>Small helper utilities for inventory manipulation.</p>
 */
public final class InventoryUtils {
    private InventoryUtils() {}

    public static ItemStack cloneItem(ItemStack item) {
        return item == null ? null : item.clone();
    }

    public static ItemStack makeButton(Material material, String name) {
        ItemStack it = new ItemStack(material);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.displayName(Mm.mm(name));
            it.setItemMeta(meta);
        }
        return it;
    }

    public static ItemStack makeButton(Material material, Component name) {
        ItemStack it = new ItemStack(material);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.displayName(name);
            it.setItemMeta(meta);
        }
        return it;
    }
}
