package io.github.snow1026.snowlib.inventory

import io.github.snow1026.snowlib.inventory.pagination.PaginationManager
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack

/**
 * KotlinSnowInventory
 *
 * <p>Kotlin-friendly wrapper around [SnowInventory].
 * Provides DSL-style extensions for easier GUI building.</p>
 *
 * Example usage:
 * ```
 * val inv = KotlinSnowInventory("My Menu", 3)
 *
 * inv.slot(1, 1) {
 *     item(myItemStack)
 *     onClick {
 *         it.whoClicked.sendMessage("Clicked!")
 *     }
 * }
 *
 * inv.paginator().autoPaginate(items, slots)
 *
 * inv.openFor(player)
 * ```
 */
class KotlinSnowInventory( title: String, rows: Int ) {
    private val handle: SnowInventory = SnowInventory(title, rows)

    /** Open for player */
    fun openFor(player: Player) = handle.openFor(player)

    /** Get pagination manager */
    fun paginator(): PaginationManager = handle.paginator()

    /** Set raw slot item */
    fun setItemAt(rawSlot: Int, item: ItemStack?) = handle.setItemAt(rawSlot, item)

    /** Clear items */
    fun clearItems() = handle.clearItems()

    /** Kotlin DSL for a slot */
    fun slot(row: Int, col: Int, builder: KotlinSlotBuilder.() -> Unit) {
        val raw = (row - 1) * 9 + (col - 1)
        KotlinSlotBuilder(handle, raw).apply(builder)
    }

    /** Expose underlying Java handle */
    fun unwrap(): SnowInventory = handle
}

/**
 * KotlinSlotBuilder
 *
 * Kotlin DSL wrapper for [SnowSlotBuilder].
 */
class KotlinSlotBuilder(
    private val parent: SnowInventory,
    private val rawSlot: Int
) {
    fun item(stack: ItemStack?): KotlinSlotBuilder {
        parent.setItemAt(rawSlot, stack)
        return this
    }

    fun onClick(handler: (InventoryClickEvent) -> Unit): KotlinSlotBuilder {
        parent.registerHandler(rawSlot) { e -> handler(e) }
        return this
    }
}
