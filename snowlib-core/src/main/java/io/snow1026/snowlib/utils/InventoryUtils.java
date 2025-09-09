package io.snow1026.snowlib.utils;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;

/**
 * InventoryUtils
 *
 * <p>Small helper utilities used by SnowInventory and PaginationManager:
 * cloning ItemStack, creating simple button items, and number markers.</p>
 */
public final class InventoryUtils {

    private InventoryUtils() { /* static only */ }

    /**
     * Clone item safely (null-safe).
     *
     * @param item item or null
     * @return cloned item or null
     */
    public static ItemStack cloneItem(ItemStack item) {
        return item == null ? null : item.clone();
    }

    /**
     * Make a basic button ItemStack with a display name.
     *
     * @param material material
     * @param name     display name (color codes allowed)
     * @return itemstack
     */
    public static ItemStack makeButton(Material material, String name) {
        ItemStack it = new ItemStack(material);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.displayName(io.snow1026.snowlib.utils.Mm.mm(name));
            meta.lore(Collections.emptyList());
            it.setItemMeta(meta);
        }
        return it;
    }

    /**
     * Make a PAPER item representing a page number.
     *
     * @param number page number (1-based)
     * @param active whether current page (will be glowing)
     * @return itemstack
     */
    public static ItemStack makeNumberPaper(int number, boolean active) {
        ItemStack it = new ItemStack(Material.PAPER);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.displayName(io.snow1026.snowlib.utils.Mm.mm("&bPage &r" + number));
            meta.lore(Collections.singletonList(io.snow1026.snowlib.utils.Mm.mm("&7Click to go to page " + number)));
            if (active) meta.addEnchant(org.bukkit.enchantments.Enchantment.LURE, 1, true);
            it.setItemMeta(meta);
        }
        return it;
    }
}
