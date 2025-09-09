package io.github.snow1026.snowlib.inventory;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

/**
 * SnowSlotBuilder
 *
 * <p>Fluent builder for configuring a single slot:
 * item, metadata, click handler.</p>
 */
public record SnowSlotBuilder(SnowInventory parent, int rawSlot) {

    /**
     * Set item in this slot.
     */
    public SnowSlotBuilder item(ItemStack item) {
        parent.setItemAt(rawSlot, item);
        return this;
    }

    /**
     * Add click handler.
     */
    public SnowSlotBuilder onClick(Consumer<InventoryClickEvent> handler) {
        parent.registerHandler(rawSlot, handler);
        return this;
    }
}
