package io.github.snow1026.snowlib.inventory.pagination;

import io.github.snow1026.snowlib.utils.InventoryUtils;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

/**
 * DefaultPaginationTheme
 *
 * <p>Uses arrows for navigation and paper for page numbers.</p>
 */
public class DefaultPaginationTheme implements PaginationTheme {

    @Override
    public ItemStack makePrevButton() {
        return InventoryUtils.makeButton(Material.ARROW, "Previous");
    }

    @Override
    public ItemStack makeNextButton() {
        return InventoryUtils.makeButton(Material.ARROW, "Next");
    }

    @Override
    public ItemStack makeNumberItem(int number, boolean active) {
        ItemStack it = InventoryUtils.makeButton(Material.PAPER, "Page " + number);
        if (active) {
            it.addUnsafeEnchantment(Enchantment.LURE, 1);
        }
        return it;
    }
}
