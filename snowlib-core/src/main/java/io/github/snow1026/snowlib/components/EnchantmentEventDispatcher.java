package io.github.snow1026.snowlib.components;

import io.github.snow1026.snowlib.enchantments.SnowEnchantment;

public class EnchantmentEventDispatcher extends Component<SnowEnchantment> {
    public SnowEnchantment.AttackHandler onAttack;
    public SnowEnchantment.DamageHandler onDamage;
    public SnowEnchantment.GeneralHandler onGeneral;
    public SnowEnchantment.BlockBreakHandler onBlockBreak;
    public SnowEnchantment.InteractHandler onInteract;
    public SnowEnchantment.EquipHandler onEquip;
    public SnowEnchantment.EquipHandler onUnequip;
    public SnowEnchantment.ProjectileHandler onProjectileHit;
    public SnowEnchantment.KillHandler onKill;

    public EnchantmentEventDispatcher(SnowEnchantment parent) {
        super(parent);
    }
}
