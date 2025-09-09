package io.github.snow1026.snowlib.inventory.pagination;

import org.bukkit.inventory.ItemStack;

/**
 * PaginationTheme
 *
 * <p>Defines how pagination buttons and indicators are rendered.</p>
 */
public interface PaginationTheme {

    ItemStack makePrevButton();

    ItemStack makeNextButton();

    ItemStack makeNumberItem(int number, boolean active);

    static PaginationTheme defaultTheme() {
        return new DefaultPaginationTheme();
    }
}
