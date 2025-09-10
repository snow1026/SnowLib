package io.github.snow1026.snowlib.components;

import io.github.snow1026.snowlib.enchantments.EnchantmentTarget;
import io.github.snow1026.snowlib.enchantments.SnowEnchantment;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

import java.util.function.Function;
import java.util.function.Predicate;

public class EnchantmentComponent extends Component<SnowEnchantment>{
    public NamespacedKey key;
    public String name = "Unnamed";
    public int maxLevel = 1;

    public boolean curse = false;
    public boolean treasure = false;
    public boolean lootable = true;
    public boolean tradeable = true;
    public int weight = 1;
    public int anvilCost = 1;
    public String description = "";
    public EnchantmentTarget targetSlot = EnchantmentTarget.BREAKABLE;
    public SnowEnchantment.Rarity rarity = SnowEnchantment.Rarity.UNCOMMON;

    public Function<Integer, Integer> minCost = lvl -> 1;
    public Function<Integer, Integer> maxCost = lvl -> 10;
    public Predicate<ItemStack> applicableItems = item -> true;
    public Predicate<String> conflicts = id -> false;

    public EnchantmentComponent(SnowEnchantment parent) {
        super(parent);
    }
}
