package io.github.snow1026.snowlib.inventory.pagination;

import io.github.snow1026.snowlib.inventory.SnowInventory;
import io.github.snow1026.snowlib.utils.InventoryUtils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * PaginationManager
 *
 * <p>Manages multiple pages of content inside a {@link SnowInventory}.
 * Provides next/prev navigation and numeric indicators.</p>
 */
public class PaginationManager {
    private final SnowInventory parent;
    private final List<Consumer<SnowInventory>> pages = new ArrayList<>();
    private int currentPage = 0;

    private PaginationTheme theme = PaginationTheme.defaultTheme();

    public PaginationManager(SnowInventory parent) {
        this.parent = parent;
    }

    public int getTotalPages() {
        return pages.size();
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public PaginationManager theme(PaginationTheme theme) {
        this.theme = theme;
        return this;
    }

    public PaginationManager addPage(Consumer<SnowInventory> page) {
        pages.add(page);
        return this;
    }

    public void showPage(int page, Player p) {
        if (page < 0 || page >= pages.size()) return;
        currentPage = page;
        parent.clearItems();
        pages.get(page).accept(parent);
        renderPageIndicator();
        parent.openFor(p);
    }

    public void next(Player p) {
        if (pages.isEmpty()) return;
        int nxt = Math.min(currentPage + 1, pages.size() - 1);
        showPage(nxt, p);
    }

    public void prev(Player p) {
        if (pages.isEmpty()) return;
        int prev = Math.max(0, currentPage - 1);
        showPage(prev, p);
    }

    public void autoPaginate(List<ItemStack> items, List<Integer> contentSlots) {
        pages.clear();
        if (contentSlots == null || contentSlots.isEmpty()) return;
        int perPage = contentSlots.size();
        int totalPages = (int) Math.ceil((double) items.size() / perPage);
        if (totalPages == 0) totalPages = 1;
        for (int p = 0; p < totalPages; p++) {
            final int pageIndex = p;
            pages.add(inv -> {
                for (int slot : contentSlots) inv.setItemAt(slot, null);
                int start = pageIndex * perPage;
                int end = Math.min(start + perPage, items.size());
                int idx = 0;
                for (int i = start; i < end; i++) {
                    ItemStack it = items.get(i);
                    int rawSlot = contentSlots.get(idx++);
                    inv.setItemAt(rawSlot, InventoryUtils.cloneItem(it));
                }
            });
        }
        currentPage = 0;
        renderPageIndicator();
    }

    private void renderPageIndicator() {
        int lastRow = parent.getRows();

        // prev button
        parent.slot(lastRow, 1)
                .onClick(e -> prev((Player) e.getWhoClicked()));

        // next button
        parent.slot(lastRow, 9)
                .item(theme.makeNextButton())
                .onClick(e -> next((Player) e.getWhoClicked()));

        // numeric indicators
        int maxNumberSlots = 7;
        int total = pages.size();
        if (total == 0) {
            for (int i = 0; i < maxNumberSlots; i++) {
                int col = 2 + i;
                parent.remove(lastRow, col);
            }
            return;
        }
        int half = maxNumberSlots / 2;
        int start = Math.max(0, Math.min(currentPage - half, Math.max(0, total - maxNumberSlots)));
        for (int i = 0; i < maxNumberSlots; i++) {
            int col = 2 + i;
            int page = start + i;
            if (page < total) {
                final int thisPage = page;
                boolean active = (thisPage == currentPage);
                parent.slot(lastRow, col)
                        .item(theme.makeNumberItem(thisPage + 1, active))
                        .onClick(e -> showPage(thisPage, (Player) e.getWhoClicked()));
            } else {
                parent.remove(lastRow, col);
            }
        }
    }
}
