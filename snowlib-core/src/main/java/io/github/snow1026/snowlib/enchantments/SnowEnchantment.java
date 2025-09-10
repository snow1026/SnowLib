package io.github.snow1026.snowlib.enchantments;

import io.github.snow1026.snowlib.Snow;
import io.github.snow1026.snowlib.components.EnchantmentComponent;
import io.github.snow1026.snowlib.components.EnchantmentEventDispatcher;
import io.github.snow1026.snowlib.registry.EnchantmentRegistry;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

public class SnowEnchantment extends Snow {
    private EnchantmentComponent component;
    private EnchantmentEventDispatcher dispatcher;

    public void register(Consumer<EnchantmentComponent> componentBuilder) {
        this.component = new EnchantmentComponent(this);
        componentBuilder.accept(component);
        EnchantmentRegistry.register(this);
    }

    public void register(Consumer<EnchantmentComponent> componentBuilder, Consumer<EnchantmentEventDispatcher> eventBuilder) {
        this.component = new EnchantmentComponent(this);
        this.dispatcher = new EnchantmentEventDispatcher(this);
        componentBuilder.accept(component);
        eventBuilder.accept(dispatcher);

        EnchantmentRegistry.register(this);
        EventDispatcher.register(this);
    }

    public EnchantmentComponent getComponent() {
        return component;
    }

    public EnchantmentEventDispatcher getDispatcher() {
        return dispatcher;
    }

    // ───── 이벤트 핸들러 인터페이스 ─────
    @FunctionalInterface public interface AttackHandler {
        void handle(Player attacker, LivingEntity victim, EntityDamageByEntityEvent event);
    }
    @FunctionalInterface public interface DamageHandler {
        void handle(Player victim, LivingEntity damager, EntityDamageEvent event);
    }
    @FunctionalInterface public interface GeneralHandler {
        void handle(Player player, org.bukkit.event.Event event);
    }
    @FunctionalInterface public interface BlockBreakHandler {
        void handle(Player player, org.bukkit.block.Block block, BlockBreakEvent event);
    }
    @FunctionalInterface public interface InteractHandler {
        void handle(Player player, ItemStack item, PlayerInteractEvent event);
    }
    @FunctionalInterface public interface EquipHandler {
        void handle(Player player, ItemStack item);
    }
    @FunctionalInterface public interface ProjectileHandler {
        void handle(Player shooter, LivingEntity target, ProjectileHitEvent event);
    }
    @FunctionalInterface public interface KillHandler {
        void handle(Player killer, LivingEntity dead, EntityDeathEvent event);
    }

    public enum Rarity { COMMON, UNCOMMON, RARE, VERY_RARE }
}
