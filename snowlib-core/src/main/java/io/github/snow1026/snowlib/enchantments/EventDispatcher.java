package io.github.snow1026.snowlib.enchantments;

import io.github.snow1026.snowlib.SnowLibrary;
import io.github.snow1026.snowlib.components.EnchantmentEventDispatcher;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;

// record 대신 class로 변경하여 캐싱 필드를 추가합니다.
public final class EventDispatcher implements Listener {

    private final SnowEnchantment enchant;
    private final EnchantmentEventDispatcher dispatcher;
    private Enchantment cachedEnchant; // Bukkit 인챈트 객체를 캐싱하기 위한 필드

    private EventDispatcher(SnowEnchantment enchant, EnchantmentEventDispatcher dispatcher) {
        this.enchant = enchant;
        this.dispatcher = dispatcher;
    }

    public static void register(SnowEnchantment enchant) {
        SnowLibrary.instance.getServer().getPluginManager().registerEvents(new EventDispatcher(enchant, enchant.getDispatcher()), SnowLibrary.instance);
    }

    /**
     * Bukkit의 인챈트 객체를 가져옵니다.
     * 한번 조회한 후에는 결과를 캐싱하여 다음부터 빠르게 반환합니다.
     */
    private Enchantment getEnchantment() {
        if (this.cachedEnchant == null) {
            // Paper API를 사용하여 버전과 무관하게 안전하게 인챈트를 가져옵니다.
            this.cachedEnchant = RegistryAccess.registryAccess()
                    .getRegistry(RegistryKey.ENCHANTMENT)
                    .get(enchant.getComponent().key);
        }
        return this.cachedEnchant;
    }

    @EventHandler
    public void onAttack(EntityDamageByEntityEvent e) {
        if (dispatcher.onAttack == null) return;
        if (!(e.getDamager() instanceof Player attacker)) return;
        if (!(e.getEntity() instanceof LivingEntity victim)) return;

        // 공격자의 주 손에 든 아이템에 인챈트가 있는지 확인
        ItemStack weapon = attacker.getInventory().getItemInMainHand();
        if (weapon.getEnchantmentLevel(getEnchantment()) > 0) {
            dispatcher.onAttack.handle(attacker, victim, e);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (dispatcher.onDamage == null) return;
        if (!(e.getEntity() instanceof Player victim)) return;

        // 피격자가 착용한 방어구 중 하나라도 인챈트가 있는지 확인
        boolean hasEnchant = Arrays.stream(victim.getInventory().getArmorContents())
                .anyMatch(item -> item != null && item.getEnchantmentLevel(getEnchantment()) > 0);

        if (hasEnchant) {
            LivingEntity damager = null;
            if (e instanceof EntityDamageByEntityEvent edbe) {
                if (edbe.getDamager() instanceof LivingEntity) {
                    damager = (LivingEntity) edbe.getDamager();
                }
            }
            dispatcher.onDamage.handle(victim, damager, e);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        if (dispatcher.onBlockBreak == null) return;

        // 블록을 부순 도구에 인챈트가 있는지 확인
        ItemStack tool = e.getPlayer().getInventory().getItemInMainHand();
        if (tool.getEnchantmentLevel(getEnchantment()) > 0) {
            dispatcher.onBlockBreak.handle(e.getPlayer(), e.getBlock(), e);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (dispatcher.onInteract == null) return;

        // 상호작용한 아이템에 인챈트가 있는지 확인
        ItemStack item = e.getItem();
        if (item != null && item.getEnchantmentLevel(getEnchantment()) > 0) {
            dispatcher.onInteract.handle(e.getPlayer(), item, e);
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent e) {
        if (dispatcher.onProjectileHit == null) return;
        if (!(e.getEntity().getShooter() instanceof Player shooter)) return;
        if (!(e.getHitEntity() instanceof LivingEntity target)) return;

        // 발사체가 발사될 때 사용한 무기(활, 쇠뇌 등)에 인챈트가 있는지 확인
        // 참고: 이 이벤트는 발사체 자체의 정보만 있으므로, 발사 시점의 아이템을 정확히 알기는 어렵습니다.
        // 여기서는 주 손과 다른 손의 아이템을 확인하는 것으로 간소화합니다.
        ItemStack mainHand = shooter.getInventory().getItemInMainHand();
        ItemStack offHand = shooter.getInventory().getItemInOffHand();

        if (mainHand.getEnchantmentLevel(getEnchantment()) > 0 || offHand.getEnchantmentLevel(getEnchantment()) > 0) {
            dispatcher.onProjectileHit.handle(shooter, target, e);
        }
    }

    @EventHandler
    public void onKill(EntityDeathEvent e) {
        if (dispatcher.onKill == null) return;
        Player killer = e.getEntity().getKiller();
        if (killer == null) return;

        // 킬을 한 무기에 인챈트가 있는지 확인
        ItemStack weapon = killer.getInventory().getItemInMainHand();
        if (weapon.getEnchantmentLevel(getEnchantment()) > 0) {
            dispatcher.onKill.handle(killer, e.getEntity(), e);
        }
    }
}