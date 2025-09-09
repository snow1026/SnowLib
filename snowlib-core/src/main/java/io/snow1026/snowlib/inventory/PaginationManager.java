package io.snow1026.snowlib.inventory;

import io.snow1026.snowlib.utils.InventoryUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * PaginationManager
 *
 * <p>
 * Manages pages for a {@link SnowInventory}. It creates page consumers which
 * populate the inventory with content for each page. It also renders a numeric
 * page indicator and previous/next buttons in the last row of the inventory.
 * </p>
 *
 * <p>Design:</p>
 * <ul>
 *   <li>Content area is a list of raw slot indices provided by the caller.</li>
 *   <li>Pagination UI uses the last row:
 *       slot (lastRow,1) = Prev, slots (lastRow,2..8) = numeric indicators,
 *       slot (lastRow,9) = Next.</li>
 * </ul>
 */
public class PaginationManager {

    private final SnowInventory parent;
    private final List<Consumer<SnowInventory>> pages = new ArrayList<>();
    private int currentPage = 0;

    /**
     * Create a pagination manager for a parent SnowInventory.
     *
     * @param parent parent inventory
     */
    public PaginationManager(SnowInventory parent) {
        this.parent = parent;
    }

    /**
     * Returns number of pages.
     *
     * @return total pages
     */
    public int getTotalPages() {
        return pages.size();
    }

    /**
     * Returns current page index.
     *
     * @return current page (0-based)
     */
    public int getCurrentPage() {
        return currentPage;
    }

    /**
     * Add a page consumer explicitly.
     *
     * @param page consumer that accepts SnowInventory and populates the content
     * @return this
     */
    public PaginationManager addPage(Consumer<SnowInventory> page) {
        pages.add(page);
        return this;
    }

    /**
     * Show a page to a player. Renders content then pagination UI.
     *
     * @param page page index (0-based)
     * @param p player
     */
    public void showPage(int page, Player p) {
        if (page < 0 || page >= pages.size()) return;
        currentPage = page;
        // clear content area (do not clear handlers)
        parent.clearItems();
        // call page consumer
        pages.get(page).accept(parent);
        // render numeric page indicator
        renderPageIndicator();
        // open for player
        parent.openFor(p);
    }

    /**
     * Next page (wraps if necessary).
     *
     * @param p player
     */
    public void next(Player p) {
        if (pages.isEmpty()) return;
        int nxt = Math.min(currentPage + 1, pages.size() - 1);
        showPage(nxt, p);
    }

    /**
     * Previous page.
     *
     * @param p player
     */
    public void prev(Player p) {
        if (pages.isEmpty()) return;
        int prev = Math.max(0, currentPage - 1);
        showPage(prev, p);
    }

    /**
     * Automatic pagination helper.
     *
     * <p>
     * Given a list of items and a list of raw slot indices that act as "content slots",
     * this method divides the items into pages and builds page consumers.
     * </p>
     *
     * @param items content items
     * @param contentSlots raw slot indices to fill per page (order matters)
     */
    public void autoPaginate(List<ItemStack> items, List<Integer> contentSlots) {
        pages.clear();
        if (contentSlots == null || contentSlots.isEmpty()) return;
        int perPage = contentSlots.size();
        int totalPages = (int) Math.ceil((double) items.size() / perPage);
        if (totalPages == 0) totalPages = 1;
        for (int p = 0; p < totalPages; p++) {
            final int pageIndex = p;
            pages.add(inv -> {
                // clear content slots first
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
        // after building pages, if any viewers exist show first page to first viewer for preview
        currentPage = 0;
        // attach pagination UI click handlers (prev/next/number)
        renderPageIndicator();
    }

    /**
     * Render prev/next and numeric indicators in last row of parent inventory.
     * Prev at (lastRow,1), numbers at (lastRow,2..8), Next at (lastRow,9).
     */
    private void renderPageIndicator() {
        int lastRow = parent.getRows();
        // prev button (col 1)
        parent.slot(lastRow, 1, b -> {
            b.set(InventoryUtils.makeButton(Material.ARROW, "&aPrev"), "Prev", Collections.singletonList("&7Previous page"), 1, false);
            b.onClick(e -> {
                Player p = (Player) e.getWhoClicked();
                prev(p);
            });
        });

        // next button (col 9)
        parent.slot(lastRow, 9, b -> {
            b.set(InventoryUtils.makeButton(Material.ARROW, "&aNext"), "Next", Collections.singletonList("&7Next page"), 1, false);
            b.onClick(e -> {
                Player p = (Player) e.getWhoClicked();
                next(p);
            });
        });

        // numeric slots: cols 2..8 (7 slots)
        int maxNumberSlots = 7;
        int total = pages.size();
        if (total == 0) {
            // clear numeric area
            for (int i = 0; i < maxNumberSlots; i++) {
                int col = 2 + i;
                parent.remove(lastRow, col);
            }
            return;
        }

        // sliding window of page numbers centered on current page
        int half = maxNumberSlots / 2;
        int start = Math.max(0, Math.min(currentPage - half, Math.max(0, total - maxNumberSlots)));
        for (int i = 0; i < maxNumberSlots; i++) {
            int col = 2 + i;
            int page = start + i;
            if (page < total) {
                final int thisPage = page;
                boolean active = (thisPage == currentPage);
                parent.slot(lastRow, col, b -> {
                    ItemStack numberItem = InventoryUtils.makeNumberPaper(thisPage + 1, active);
                    b.set(numberItem, (active ? "&e> " : "") + (thisPage + 1) + (active ? " <" : ""), Collections.singletonList("&7Click to jump to page " + (thisPage + 1)), 1, active);
                    b.onClick(e -> {
                        Player p = (Player) e.getWhoClicked();
                        showPage(thisPage, p);
                    });
                });
            } else {
                // empty
                parent.remove(lastRow, col);
            }
        }
    }
}
