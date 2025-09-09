package io.github.snow1026.snowlib.inventory;

import io.github.snow1026.snowlib.inventory.pagination.PaginationManager;
import io.github.snow1026.snowlib.utils.Mm;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * SnowInventory
 *
 * <p>Wrapper around Bukkit Inventory API with builder-style slot management,
 * click handlers, and pagination support.</p>
 */
public class SnowInventory implements Listener {

    private final int rows;
    private final Inventory handle;

    private final Map<Integer, Consumer<InventoryClickEvent>> clickHandlers = new HashMap<>();

    private PaginationManager pagination;

    /**
     * Create a new SnowInventory.
     *
     * @param title inventory title
     * @param rows number of rows (1–6)
     */
    public SnowInventory(String title, int rows) {
        this.rows = rows;
        this.handle = Bukkit.createInventory(null, rows * 9, Mm.mm(title));
    }

    /**
     * Open this inventory for a player.
     *
     * @param player player
     */
    public void openFor(Player player) {
        player.openInventory(handle);
    }

    /**
     * Set an item directly at a raw slot.
     *
     * @param rawSlot slot index (0–rows*9-1)
     * @param item item to set (nullable = clears slot)
     */
    public void setItemAt(int rawSlot, ItemStack item) {
        handle.setItem(rawSlot, item);
    }

    /**
     * Clear all items in the inventory (handlers remain).
     */
    public void clearItems() {
        handle.clear();
    }

    /**
     * Builder for a specific slot.
     *
     * @param row row number (1-based)
     * @param col column number (1–9)
     * @return builder for this slot
     */
    public SnowSlotBuilder slot(int row, int col) {
        int raw = (row - 1) * 9 + (col - 1);
        return new SnowSlotBuilder(this, raw);
    }

    /**
     * Remove item and handler from a slot.
     *
     * @param row row number (1-based)
     * @param col column number (1–9)
     */
    public void remove(int row, int col) {
        int raw = (row - 1) * 9 + (col - 1);
        handle.setItem(raw, null);
        clickHandlers.remove(raw);
    }

    /**
     * Register a click handler.
     */
    protected void registerHandler(int rawSlot, Consumer<InventoryClickEvent> handler) {
        clickHandlers.put(rawSlot, handler);
    }

    /**
     * Get number of rows.
     */
    public int getRows() {
        return rows;
    }

    /**
     * Access underlying Bukkit inventory.
     */
    public Inventory getHandle() {
        return handle;
    }

    /**
     * Pagination manager accessor (lazy).
     */
    public PaginationManager paginator() {
        if (pagination == null) {
            pagination = new PaginationManager(this);
        }
        return pagination;
    }

    // --- Bukkit events ---

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getClickedInventory() == null) return;
        if (!e.getClickedInventory().equals(handle)) return;
        int raw = e.getSlot();
        Consumer<InventoryClickEvent> handler = clickHandlers.get(raw);
        if (handler != null) {
            handler.accept(e);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (e.getInventory().equals(handle)) {
            clickHandlers.clear();
        }
    }
}
