package io.github.snow1026.snowlib.inventory;

import io.github.snow1026.snowlib.SnowLibrary;
import io.github.snow1026.snowlib.utils.InventoryUtils;
import io.github.snow1026.snowlib.utils.Mm;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * SnowInventory
 *
 * <p>
 * A utility class which simplifies creating interactive Bukkit/Spigot inventories (GUIs).
 * Features:
 * <ul>
 *   <li>Slot-based DSL via {@link SlotBuilder} for setting items and handlers.</li>
 *   <li>Slot locking / read-only slots.</li>
 *   <li>Dynamic lore suppliers (automatically refreshed on open).</li>
 *   <li>Pagination support via {@link PaginationManager} with previous/next and numeric indicators.</li>
 *   <li>Common convenience methods (fill, pattern, mirror, swap, copy layout, etc.).</li>
 *   <li>Async helpers and scheduled tasks integrated with the plugin instance.</li>
 * </ul>
 * </p>
 *
 * <p>Usage synopsis:</p>
 * <pre>{@code
 * SnowInventory inv = SnowInventory.make(3, "&aMy GUI", "my_gui");
 * inv.slot(2,5, b -> b.item(new ItemStack(Material.DIAMOND)).onClick(e -> { ... }));
 * inv.openFor(player);
 * }</pre>
 *
 * <p>Note: this class registers itself as a Bukkit {@link Listener} on construction;
 * to stop listening and to remove a registered ID call {@link #unregister()}.</p>
 *
 * @author Snow
 * @since 1.0
 */
public class SnowInventory implements Listener {

    /* ---------- Registry ---------- */

    /** Map of inventories registered by ID. */
    private static final Map<String, SnowInventory> INSTANCES_BY_ID = new HashMap<>();

    /* ---------- Core state ---------- */

    private Inventory inventory;
    private String id;
    private String title;
    private int rows;

    /* ---------- Events / callbacks ---------- */

    private Consumer<InventoryOpenEvent> openEvent;
    private Consumer<InventoryCloseEvent> closeEvent;
    private Consumer<InventoryClickEvent> clickEvent;
    private Consumer<InventoryClickEvent> clickInEvent;
    private Consumer<InventoryClickEvent> clickOutEvent;
    private Consumer<InventoryDragEvent> dragEvent;

    /* ---------- Per-slot handlers ---------- */

    private final Map<Integer, Consumer<InventoryClickEvent>> slotClickHandlers = new HashMap<>();
    private final Map<Integer, Consumer<InventoryClickEvent>> slotLeftClickHandlers = new HashMap<>();
    private final Map<Integer, Consumer<InventoryClickEvent>> slotRightClickHandlers = new HashMap<>();
    private final Map<Integer, Consumer<InventoryClickEvent>> slotShiftClickHandlers = new HashMap<>();
    private final Map<Integer, Consumer<InventoryClickEvent>> slotDoubleClickHandlers = new HashMap<>();
    private final Map<Integer, Consumer<InventoryClickEvent>> slotNumberKeyHandlers = new HashMap<>();
    private final Map<Integer, Consumer<InventoryDragEvent>> slotDragHandlers = new HashMap<>();

    /* ---------- Slot state ---------- */

    private final Set<Integer> lockedSlots = new HashSet<>();
    private final Set<Integer> readOnlySlots = new HashSet<>();
    private final Map<Integer, Supplier<List<String>>> dynamicLoreSuppliers = new HashMap<>();

    /* ---------- Pagination ---------- */

    private final PaginationManager pagination = new PaginationManager(this);

    /* ---------- Options ---------- */

    private boolean defaultCancelOnClick = true;
    private boolean preventTakeFromGui = true;

    /* ---------- Construction ---------- */

    /**
     * Constructs and registers this {@code SnowInventory} as an event listener.
     * Ensure {@link SnowLibrary#instance} is set to your plugin instance before creating.
     */
    public SnowInventory() {
        Bukkit.getPluginManager().registerEvents(this, SnowLibrary.instance);
    }

    /* ======================================================================
       Registration / factory
       ====================================================================== */

    /**
     * Register inventory with rows and title.
     *
     * @param rows  cabinet rows (1..6)
     * @param title display title (will be colorized via {@link Mm#mm(String)})
     * @return this instance
     * @throws IllegalArgumentException if rows outside 1..6
     */
    public SnowInventory register(int rows, String title) {
        return register(rows, title, null);
    }

    /**
     * Register inventory with rows, title and optional id.
     *
     * @param rows  rows (1..6)
     * @param title title
     * @param id    optional id for later lookup via {@link #getById(String)}
     * @return this instance
     */
    public SnowInventory register(int rows, String title, String id) {
        if (rows < 1 || rows > 6) throw new IllegalArgumentException("rows must be 1..6");
        this.rows = rows;
        this.title = title == null ? "" : title;
        this.id = id;
        this.inventory = Bukkit.createInventory(null, rows * 9, Mm.mm(this.title));
        if (id != null) INSTANCES_BY_ID.put(id, this);
        return this;
    }

    /**
     * Factory helper.
     *
     * @param rows  rows
     * @param title title
     * @param id    id (optional)
     * @return new {@link SnowInventory} instance already registered
     */
    public static SnowInventory make(int rows, String title, String id) {
        SnowInventory si = new SnowInventory();
        si.register(rows, title, id);
        return si;
    }

    /**
     * Lookup registered inventory by ID.
     *
     * @param id id
     * @return Optional of {@link SnowInventory}
     */
    public static Optional<SnowInventory> getById(String id) {
        return Optional.ofNullable(INSTANCES_BY_ID.get(id));
    }

    /**
     * Unregister event listeners and remove id registration (if present).
     *
     * @return this instance
     */
    public SnowInventory unregister() {
        HandlerList.unregisterAll(this);
        if (id != null) INSTANCES_BY_ID.remove(id);
        return this;
    }

    /* ======================================================================
       Accessors / options
       ====================================================================== */

    /**
     * Returns underlying Bukkit inventory.
     *
     * @return inventory
     */
    public Inventory getInventory() {
        return inventory;
    }

    /**
     * Returns registered id (may be null).
     *
     * @return id or null
     */
    public String getId() {
        return id;
    }

    /**
     * Returns number of rows for this inventory.
     *
     * @return rows
     */
    public int getRows() {
        return rows;
    }

    /**
     * Returns the display title text (raw, not colorized).
     *
     * @return title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Returns the pagination manager for this inventory.
     *
     * @return pagination manager
     */
    public PaginationManager pagination() {
        return pagination;
    }

    /**
     * Get whether clicks inside this GUI are cancelled by default.
     *
     * @return true if cancelled by default
     */
    public boolean isDefaultCancelOnClick() {
        return defaultCancelOnClick;
    }

    /**
     * Set whether clicks inside this GUI are cancelled by default.
     *
     * @param v boolean
     * @return this instance
     */
    public SnowInventory setDefaultCancelOnClick(boolean v) {
        this.defaultCancelOnClick = v;
        return this;
    }

    /**
     * Get whether taking items from GUI is prevented when clicked.
     *
     * @return true if prevented
     */
    public boolean isPreventTakeFromGui() {
        return preventTakeFromGui;
    }

    /**
     * Set prevention of taking items from GUI.
     *
     * @param v boolean
     * @return this instance
     */
    public SnowInventory setPreventTakeFromGui(boolean v) {
        this.preventTakeFromGui = v;
        return this;
    }

    /* ======================================================================
       Event chaining setters
       ====================================================================== */

    /**
     * Set inventory open event handler.
     *
     * @param h handler
     * @return this instance
     */
    public SnowInventory onOpen(Consumer<InventoryOpenEvent> h) {
        this.openEvent = h;
        return this;
    }

    /**
     * Set inventory close event handler.
     *
     * @param h handler
     * @return this instance
     */
    public SnowInventory onClose(Consumer<InventoryCloseEvent> h) {
        this.closeEvent = h;
        return this;
    }

    /**
     * Set a general click event handler (fires for every click).
     *
     * @param h handler
     * @return this instance
     */
    public SnowInventory onClick(Consumer<InventoryClickEvent> h) {
        this.clickEvent = h;
        return this;
    }

    /**
     * Set event handler that runs only when clicked inside this inventory.
     *
     * @param h handler
     * @return this instance
     */
    public SnowInventory onClickIn(Consumer<InventoryClickEvent> h) {
        this.clickInEvent = h;
        return this;
    }

    /**
     * Set event handler that runs only when clicked outside this inventory.
     *
     * @param h handler
     * @return this instance
     */
    public SnowInventory onClickOut(Consumer<InventoryClickEvent> h) {
        this.clickOutEvent = h;
        return this;
    }

    /**
     * Set drag event handler.
     *
     * @param h handler
     * @return this instance
     */
    public SnowInventory onDrag(Consumer<InventoryDragEvent> h) {
        this.dragEvent = h;
        return this;
    }

    /* ======================================================================
       Validation / index helpers
       ====================================================================== */

    /**
     * Ensure inventory is registered.
     *
     * @throws IllegalStateException if not registered
     */
    private void checkRegistered() {
        if (inventory == null) throw new IllegalStateException("Call register(...) before using this inventory.");
    }

    /**
     * Convert 1-based (row,col) into raw index (0-based).
     *
     * @param row  1-based row (1..rows)
     * @param col  1-based col (1..9)
     * @return raw index (0..size-1)
     * @throws IllegalArgumentException when row/col out of range
     */
    public int slotIndex(int row, int col) {
        checkRegistered();
        if (row < 1 || row > rows) throw new IllegalArgumentException("row must be 1.." + rows);
        if (col < 1 || col > 9) throw new IllegalArgumentException("col must be 1..9");
        return (row - 1) * 9 + (col - 1);
    }

    /**
     * Returns inventory size (rows * 9).
     *
     * @return size
     */
    public int getSize() {
        checkRegistered();
        return inventory.getSize();
    }

    /**
     * Set an item by raw index. Clones the item before storing.
     *
     * @param idx  raw index
     * @param item item to set (may be null)
     */
    public void setItemAt(int idx, ItemStack item) {
        checkRegistered();
        inventory.setItem(idx, InventoryUtils.cloneItem(item));
    }

    /**
     * Get an item at raw index (returns direct reference from inventory).
     *
     * @param idx raw index
     * @return item or null
     */
    public ItemStack getItemAt(int idx) {
        checkRegistered();
        return inventory.getItem(idx);
    }

    /**
     * Clear items only (keeps handlers and dynamic lore suppliers).
     */
    public void clearItems() {
        checkRegistered();
        for (int i = 0; i < inventory.getSize(); i++) inventory.setItem(i, null);
    }

    /* ======================================================================
       Slot manipulation / DSL entry
       ====================================================================== */

    /**
     * DSL entry: configure a slot by (row, col) using the {@link SlotBuilder}.
     *
     * @param row     1-based row
     * @param col     1-based col
     * @param builder builder consumer
     */
    public void slot(int row, int col, Consumer<SlotBuilder> builder) {
        builder.accept(new SlotBuilder(slotIndex(row, col)));
    }

    /**
     * Modify slot item by operator (raw row/col).
     *
     * @param row row
     * @param col col
     * @param mod unary operator applied to existing item
     * @return this instance
     */
    public SnowInventory slotModify(int row, int col, UnaryOperator<ItemStack> mod) {
        int idx = slotIndex(row, col);
        inventory.setItem(idx, mod.apply(inventory.getItem(idx)));
        return this;
    }

    /**
     * Set an item by 1-based row and col.
     *
     * @param row  row
     * @param col  col
     * @param item item
     */
    public void item(int row, int col, ItemStack item) {
        inventory.setItem(slotIndex(row, col), InventoryUtils.cloneItem(item));
    }

    /**
     * Add item to the first empty slot (like Inventory.addItem but simpler).
     *
     * @param item item
     * @return this instance
     */
    public SnowInventory add(ItemStack item) {
        checkRegistered();
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, InventoryUtils.cloneItem(item));
                return this;
            }
        }
        // if full, put in last slot
        inventory.setItem(inventory.getSize() - 1, InventoryUtils.cloneItem(item));
        return this;
    }

    /**
     * Remove item at given row/col and clear handlers attached to that slot.
     *
     * @param row row
     * @param col col
     */
    public void remove(int row, int col) {
        int idx = slotIndex(row, col);
        inventory.setItem(idx, null);
        clearSlotHandlers(idx);
    }

    /**
     * Completely clear inventory items and all handlers, dynamic lores and states.
     *
     * @return this instance
     */
    public SnowInventory clear() {
        inventory.clear();
        clearAllSlotHandlers();
        dynamicLoreSuppliers.clear();
        lockedSlots.clear();
        readOnlySlots.clear();
        return this;
    }

    /**
     * Clear handlers for a single raw slot index.
     *
     * @param idx raw index
     */
    private void clearSlotHandlers(int idx) {
        slotClickHandlers.remove(idx);
        slotLeftClickHandlers.remove(idx);
        slotRightClickHandlers.remove(idx);
        slotShiftClickHandlers.remove(idx);
        slotDoubleClickHandlers.remove(idx);
        slotNumberKeyHandlers.remove(idx);
        slotDragHandlers.remove(idx);
        dynamicLoreSuppliers.remove(idx);
    }

    /**
     * Clear all slot handlers.
     */
    private void clearAllSlotHandlers() {
        slotClickHandlers.clear();
        slotLeftClickHandlers.clear();
        slotRightClickHandlers.clear();
        slotShiftClickHandlers.clear();
        slotDoubleClickHandlers.clear();
        slotNumberKeyHandlers.clear();
        slotDragHandlers.clear();
    }

    /* ======================================================================
       Slot locking / read-only
       ====================================================================== */

    /**
     * Mark slot as locked (cannot be moved into/out of).
     *
     * @param row row
     * @param col col
     * @return this instance
     */
    public SnowInventory lockSlot(int row, int col) {
        lockedSlots.add(slotIndex(row, col));
        return this;
    }

    /**
     * Unlock previously locked slot.
     *
     * @param row row
     * @param col col
     * @return this instance
     */
    public SnowInventory unlockSlot(int row, int col) {
        lockedSlots.remove(slotIndex(row, col));
        return this;
    }

    /**
     * Check whether a slot is locked.
     *
     * @param row row
     * @param col col
     * @return true when locked
     */
    public boolean isLocked(int row, int col) {
        return lockedSlots.contains(slotIndex(row, col));
    }

    /**
     * Set slot read-only (prevents pickup actions but may allow click handlers).
     *
     * @param row row
     * @param col col
     * @return this instance
     */
    public SnowInventory setReadOnlySlot(int row, int col) {
        readOnlySlots.add(slotIndex(row, col));
        return this;
    }

    /**
     * Unset read-only slot.
     *
     * @param row row
     * @param col col
     * @return this instance
     */
    public SnowInventory unsetReadOnlySlot(int row, int col) {
        readOnlySlots.remove(slotIndex(row, col));
        return this;
    }

    /**
     * Check if a slot is read-only.
     *
     * @param row row
     * @param col col
     * @return boolean
     */
    public boolean isReadOnlySlot(int row, int col) {
        return readOnlySlots.contains(slotIndex(row, col));
    }

    /* ======================================================================
       Convenience / patterns
       ====================================================================== */

    /**
     * Fill border slots (top row, bottom row, leftmost and rightmost columns)
     * with the given item (cloned).
     *
     * @param item item
     * @return this instance
     */
    public SnowInventory fillBorder(ItemStack item) {
        for (int r = 1; r <= rows; r++)
            for (int c = 1; c <= 9; c++)
                if (r == 1 || r == rows || c == 1 || c == 9)
                    item(r, c, InventoryUtils.cloneItem(item));
        return this;
    }

    /**
     * Fill a whole row.
     *
     * @param row 1-based row
     * @param item item
     * @return this
     */
    public SnowInventory fillRow(int row, ItemStack item) {
        for (int c = 1; c <= 9; c++) item(row, c, InventoryUtils.cloneItem(item));
        return this;
    }

    /**
     * Fill a whole column.
     *
     * @param col 1-based column (1..9)
     * @param item item
     * @return this
     */
    public SnowInventory fillCol(int col, ItemStack item) {
        for (int r = 1; r <= rows; r++) item(r, col, InventoryUtils.cloneItem(item));
        return this;
    }

    /**
     * Fill all slots.
     *
     * @param item item
     * @return this
     */
    public SnowInventory fillAll(ItemStack item) {
        for (int i = 0; i < inventory.getSize(); i++) inventory.setItem(i, InventoryUtils.cloneItem(item));
        return this;
    }

    /**
     * Apply ASCII-art like pattern. Each string in shape is one row (0-based)
     * and characters map to items in map.
     *
     * @param shape lines representing rows, each up to 9 chars
     * @param map mapping characters to ItemStack prototypes
     * @return this
     */
    public SnowInventory pattern(String[] shape, Map<Character, ItemStack> map) {
        for (int r = 0; r < shape.length && r < rows; r++) {
            char[] chs = shape[r].toCharArray();
            for (int c = 0; c < chs.length && c < 9; c++) {
                ItemStack it = map.get(chs[c]);
                if (it != null) item(r + 1, c + 1, InventoryUtils.cloneItem(it));
            }
        }
        return this;
    }

    /* ======================================================================
       Inventory management
       ====================================================================== */

    /**
     * Open this inventory for player.
     *
     * @param p player
     */
    public void openFor(Player p) {
        checkRegistered();
        p.openInventory(inventory);
    }

    /**
     * Open for collection of players.
     *
     * @param players players
     * @return this
     */
    public SnowInventory openFor(Collection<Player> players) {
        for (Player p : players) p.openInventory(inventory);
        return this;
    }

    /**
     * Reopen inventory to player (close then open).
     *
     * @param p player
     * @return this
     */
    public SnowInventory reopen(Player p) {
        p.closeInventory();
        openFor(p);
        return this;
    }

    /**
     * Close this inventory for all viewers.
     *
     * @return this
     */
    public SnowInventory closeAll() {
        for (HumanEntity he : new ArrayList<>(inventory.getViewers())) he.closeInventory();
        return this;
    }

    /**
     * Update viewers' client inventory (calls Player#updateInventory()).
     *
     * @return this
     */
    public SnowInventory updateViewers() {
        for (HumanEntity he : new ArrayList<>(inventory.getViewers()))
            if (he instanceof Player) ((Player) he).updateInventory();
        return this;
    }

    /* ======================================================================
       Scheduling / async helpers
       ====================================================================== */

    /**
     * Run task later on main thread.
     *
     * @param r          runnable
     * @param delayTicks delay
     * @return this
     */
    public SnowInventory updateLater(Runnable r, long delayTicks) {
        new BukkitRunnable() {
            public void run() {
                r.run();
            }
        }.runTaskLater(SnowLibrary.instance, delayTicks);
        return this;
    }

    /**
     * Schedule repeating task on main thread.
     *
     * @param r      runnable
     * @param delay  delay ticks
     * @param period period ticks
     * @return this
     */
    public SnowInventory scheduleRepeating(Runnable r, long delay, long period) {
        new BukkitRunnable() {
            public void run() {
                r.run();
            }
        }.runTaskTimer(SnowLibrary.instance, delay, period);
        return this;
    }

    /**
     * Run asynchronous action using Bukkit async scheduler.
     *
     * @param r runnable
     */
    public void runAsync(Runnable r) {
        Bukkit.getScheduler().runTaskAsynchronously(SnowLibrary.instance, r);
    }

    /**
     * Supply asynchronously then deliver result to main thread consumer.
     *
     * @param supplier supplier producing T (async)
     * @param mainConsumer consumer on main thread
     * @param <T> result type
     */
    public <T> void supplyAsyncToMain(Supplier<T> supplier, Consumer<T> mainConsumer) {
        CompletableFuture.supplyAsync(supplier)
                .thenAccept(result -> Bukkit.getScheduler().runTask(SnowLibrary.instance, () -> mainConsumer.accept(result)));
    }

    /* ======================================================================
       SlotBuilder DSL (inner class)
       ====================================================================== */

    /**
     * SlotBuilder provides fluent methods to configure a slot (item, name, lore,
     * click handlers, locking etc.).
     */
    public class SlotBuilder {
        private final int index;

        /**
         * Construct builder for a raw index.
         *
         * @param index raw index
         */
        public SlotBuilder(int index) {
            this.index = index;
        }

        /**
         * Set item at this slot (clone stored).
         *
         * @param it item
         * @return this builder
         */
        public SlotBuilder item(ItemStack it) {
            inventory.setItem(index, InventoryUtils.cloneItem(it));
            return this;
        }

        /**
         * Modify current item using operator and store result.
         *
         * @param mod operator
         * @return this builder
         */
        public SlotBuilder modify(UnaryOperator<ItemStack> mod) {
            inventory.setItem(index, mod.apply(inventory.getItem(index)));
            return this;
        }

        /**
         * Remove item and clear slot handlers.
         *
         * @return this builder
         */
        public SlotBuilder remove() {
            inventory.setItem(index, null);
            clearSlotHandlers(index);
            return this;
        }

        /**
         * Set display name (colorized with Mm).
         *
         * @param name display name
         */
        public void name(String name) {
            ItemStack it = inventory.getItem(index);
            if (it == null) it = new ItemStack(Material.STONE);
            ItemMeta meta = it.getItemMeta();
            if (meta != null) {
                meta.displayName(Mm.mm(name));
                it.setItemMeta(meta);
            }
            inventory.setItem(index, it);
        }

        /**
         * Set lore (list of lines; colorized).
         *
         * @param lore lore
         */
        public void lore(List<String> lore) {
            ItemStack it = inventory.getItem(index);
            if (it == null) it = new ItemStack(Material.STONE);
            ItemMeta meta = it.getItemMeta();
            if (meta != null) {
                meta.lore(Mm.mm(lore));
                it.setItemMeta(meta);
            }
            inventory.setItem(index, it);
        }

        /**
         * Set amount of the item (clamped 1..maxStack).
         *
         * @param a amount
         */
        public void amount(int a) {
            ItemStack it = inventory.getItem(index);
            if (it != null) it.setAmount(Math.max(1, Math.min(it.getMaxStackSize(), a)));
        }

        /**
         * Add or remove a harmless glow (applies enchantment then hides enchant).
         *
         * @param glow true to add glow, false to remove
         */
        public void addGlow(boolean glow) {
            ItemStack it = inventory.getItem(index);
            if (it == null) it = new ItemStack(Material.STONE);
            ItemMeta meta = it.getItemMeta();
            if (meta != null) {
                if (glow) meta.addEnchant(org.bukkit.enchantments.Enchantment.LURE, 1, true);
                else meta.getEnchants().keySet().forEach(meta::removeEnchant);
                it.setItemMeta(meta);
            }
            inventory.setItem(index, it);
        }

        /**
         * Set dynamic lore supplier. Supplier will be polled on open and update.
         *
         * @param sup supplier returning lore lines
         * @return this builder
         */
        public SlotBuilder setDynamicLore(Supplier<List<String>> sup) {
            dynamicLoreSuppliers.put(index, sup);
            List<String> lore = sup.get();
            if (lore != null) lore(lore);
            return this;
        }

        /**
         * Register general click handler for this raw slot.
         *
         * @param h handler
         */
        public void onClick(Consumer<InventoryClickEvent> h) {
            slotClickHandlers.put(index, h);
        }

        /**
         * Register left-click handler.
         *
         * @param h handler
         * @return builder
         */
        public SlotBuilder onLeftClick(Consumer<InventoryClickEvent> h) {
            slotLeftClickHandlers.put(index, h);
            return this;
        }

        /**
         * Register right-click handler.
         *
         * @param h handler
         * @return builder
         */
        public SlotBuilder onRightClick(Consumer<InventoryClickEvent> h) {
            slotRightClickHandlers.put(index, h);
            return this;
        }

        /**
         * Register shift-click handler.
         *
         * @param h handler
         * @return builder
         */
        public SlotBuilder onShiftClick(Consumer<InventoryClickEvent> h) {
            slotShiftClickHandlers.put(index, h);
            return this;
        }

        /**
         * Register double-click handler.
         *
         * @param h handler
         * @return builder
         */
        public SlotBuilder onDoubleClick(Consumer<InventoryClickEvent> h) {
            slotDoubleClickHandlers.put(index, h);
            return this;
        }

        /**
         * Register number-key handler (press 1..9 over slot).
         *
         * @param h handler
         * @return builder
         */
        public SlotBuilder onNumberKey(Consumer<InventoryClickEvent> h) {
            slotNumberKeyHandlers.put(index, h);
            return this;
        }

        /**
         * Register drag handler for this raw slot.
         *
         * @param h handler
         * @return builder
         */
        public SlotBuilder onDrag(Consumer<InventoryDragEvent> h) {
            slotDragHandlers.put(index, h);
            return this;
        }

        /**
         * Mark this slot locked.
         *
         * @return this builder
         */
        public SlotBuilder lock() {
            lockedSlots.add(index);
            return this;
        }

        /**
         * Unlock this slot.
         *
         * @return this builder
         */
        public SlotBuilder unlock() {
            lockedSlots.remove(index);
            return this;
        }

        /**
         * Make this slot read-only.
         *
         * @return this builder
         */
        public SlotBuilder setReadOnly() {
            readOnlySlots.add(index);
            return this;
        }

        /**
         * Remove read-only from this slot.
         *
         * @return this builder
         */
        public SlotBuilder unsetReadOnly() {
            readOnlySlots.remove(index);
            return this;
        }

        /**
         * Convenience: set item + name + lore + amount + glow in one call.
         *
         * @param it     item prototype
         * @param name   name or null
         * @param lore   lore or null
         * @param amount amount (>0 to set)
         * @param glow   whether to add glow
         */
        public void set(ItemStack it, String name, List<String> lore, int amount, boolean glow) {
            inventory.setItem(index, InventoryUtils.cloneItem(it));
            if (name != null) name(name);
            if (lore != null) lore(lore);
            if (amount > 0) amount(amount);
            addGlow(glow);
        }
    }

    /* ======================================================================
       Pagination-related high-level helpers
       (delegates to internal PaginationManager)
       ====================================================================== */

    /**
     * Auto-paginate a list of ItemStack using contentSlots as the set of
     * raw slot indices that should be filled with page content.
     * <p>
     * Convenience: delegates to {@link PaginationManager#autoPaginate(List, List)}
     *
     * @param pagesContent list of itemstacks (items to paginate)
     * @param contentSlots raw slot indices for content area (order determines placement)
     * @return this instance
     */
    public SnowInventory autoPaginate(List<ItemStack> pagesContent, List<Integer> contentSlots) {
        pagination.autoPaginate(pagesContent, contentSlots);
        return this;
    }

    /**
     * Show a specific page number to a player (0-based).
     *
     * @param page page index (0-based)
     * @param p    player
     */
    public void showPageToPlayer(int page, Player p) {
        pagination.showPage(page, p);
    }

    /* ======================================================================
       Dynamic lore refresh
       ====================================================================== */

    /**
     * Refresh dynamic lores from suppliers for all registered dynamic slot suppliers.
     */
    private void refreshDynamicLores() {
        for (Map.Entry<Integer, Supplier<List<String>>> e : dynamicLoreSuppliers.entrySet()) {
            int idx = e.getKey();
            Supplier<List<String>> sup = e.getValue();
            ItemStack it = inventory.getItem(idx);
            if (it == null) continue;
            ItemMeta meta = it.getItemMeta();
            List<String> lore = sup.get();
            if (meta != null && lore != null) {
                meta.lore(Mm.mm(lore));
                it.setItemMeta(meta);
                inventory.setItem(idx, it);
            }
        }
    }

    /* ======================================================================
       Debug helper
       ====================================================================== */

    /**
     * Print a simple representation of the layout to the plugin logger.
     */
    public void debugPrintLayout() {
        checkRegistered();
        SnowLibrary.instance.getLogger().info("SnowInventory Layout: title=" + title + " rows=" + rows + " size=" + inventory.getSize());
        for (int r = 1; r <= rows; r++) {
            StringBuilder sb = new StringBuilder();
            sb.append("Row ").append(r).append(": ");
            for (int c = 1; c <= 9; c++) {
                int idx = slotIndex(r, c);
                ItemStack it = inventory.getItem(idx);
                if (lockedSlots.contains(idx)) sb.append("[L]");
                else if (readOnlySlots.contains(idx)) sb.append("[R]");
                else if (it == null) sb.append("[.]");
                else sb.append("[O]");
            }
            SnowLibrary.instance.getLogger().info(sb.toString());
        }
    }

    /* ======================================================================
       Event listeners
       ====================================================================== */

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent e) {
        if (!isOurTopInventory(e.getView().getTopInventory())) return;
        if (openEvent != null) openEvent.accept(e);
        refreshDynamicLores();
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!isOurTopInventory(e.getView().getTopInventory())) return;
        if (closeEvent != null) closeEvent.accept(e);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        if (!isOurTopInventory(e.getView().getTopInventory())) return;
        if (dragEvent != null) dragEvent.accept(e);
        e.setCancelled(true);
        for (int raw : e.getRawSlots()) {
            Consumer<InventoryDragEvent> handler = slotDragHandlers.get(raw);
            if (handler != null) handler.accept(e);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (clickEvent != null) clickEvent.accept(e);

        boolean topIsOurs = isOurTopInventory(e.getView().getTopInventory());
        if (!topIsOurs) {
            if (clickOutEvent != null) clickOutEvent.accept(e);
            return;
        }
        if (clickInEvent != null) clickInEvent.accept(e);

        if (defaultCancelOnClick) e.setCancelled(true);

        int rawSlot = e.getRawSlot();

        if (lockedSlots.contains(rawSlot)) {
            e.setCancelled(true);
            return;
        }
        if (readOnlySlots.contains(rawSlot)) e.setCancelled(true);

        if (preventTakeFromGui && e.getClickedInventory() != null && e.getClickedInventory().equals(inventory)) {
            if (e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY ||
                    e.getAction() == InventoryAction.PICKUP_ALL ||
                    e.getAction() == InventoryAction.PICKUP_ONE ||
                    e.getAction() == InventoryAction.PICKUP_HALF ||
                    e.getAction() == InventoryAction.PICKUP_SOME) {
                e.setCancelled(true);
            }
        }

        // general handler
        Consumer<InventoryClickEvent> general = slotClickHandlers.get(rawSlot);
        if (general != null) general.accept(e);

        // specialized dispatch
        ClickType ct = e.getClick();
        if (ct == ClickType.LEFT || ct == ClickType.SHIFT_LEFT) {
            Consumer<InventoryClickEvent> left = slotLeftClickHandlers.get(rawSlot);
            if (left != null) left.accept(e);
        }
        if (ct == ClickType.RIGHT || ct == ClickType.SHIFT_RIGHT) {
            Consumer<InventoryClickEvent> right = slotRightClickHandlers.get(rawSlot);
            if (right != null) right.accept(e);
        }
        if (ct == ClickType.SHIFT_LEFT || ct == ClickType.SHIFT_RIGHT || e.isShiftClick()) {
            Consumer<InventoryClickEvent> shift = slotShiftClickHandlers.get(rawSlot);
            if (shift != null) shift.accept(e);
        }
        if (ct == ClickType.DOUBLE_CLICK) {
            Consumer<InventoryClickEvent> dbl = slotDoubleClickHandlers.get(rawSlot);
            if (dbl != null) dbl.accept(e);
        }
        if (ct == ClickType.NUMBER_KEY) {
            Consumer<InventoryClickEvent> num = slotNumberKeyHandlers.get(rawSlot);
            if (num != null) num.accept(e);
        }
    }

    /**
     * Simple comparison helper whether the top inventory is this inventory.
     *
     * @param top top inventory
     * @return true when equals
     */
    private boolean isOurTopInventory(Inventory top) {
        return top != null && top.equals(inventory);
    }
}
