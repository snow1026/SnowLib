package io.github.snow1026.snowlib.registry;

import io.github.snow1026.snowlib.SnowLibrary;
import io.github.snow1026.snowlib.components.EnchantmentComponent;
import io.github.snow1026.snowlib.enchantments.EnchantmentTarget;
import io.github.snow1026.snowlib.enchantments.SnowEnchantment;
import io.github.snow1026.snowlib.utils.reflect.ReflectFinder;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;

// ... (클래스 Javadoc은 이전과 동일)
public final class EnchantmentRegistry {

    private EnchantmentRegistry() { /* utility */ }

    // --- 필드 선언 ---
    private static final boolean IS_MODERN_REGISTRATION;
    private static final String NMS_VERSION;
    private static final Class<?> CLASS_RESOURCE_LOCATION;
    private static final Constructor<?> CONSTRUCTOR_RESOURCE_LOCATION;
    private static final Class<?> CLASS_REGISTRY;
    private static final Class<?> CLASS_CRAFT_ITEM_STACK;
    private static final Method METHOD_AS_BUKKIT_COPY;

    // Modern (1.20.5+)
    private static Class<?> CLASS_REGISTRIES;
    private static Class<?> CLASS_ITEM_TAGS;
    private static Constructor<?> CONSTRUCTOR_NMS_ENCHANTMENT_MODERN;
    private static Method METHOD_ENCHANTMENT_DEFINITION;
    private static Constructor<?> CONSTRUCTOR_ENCHANTMENT_COST;
    private static Field FIELD_REGISTRY_STATIC_ACCESS;
    private static Method METHOD_REGISTRY_ACCESS_LOOKUP_OR_THROW;
    private static Method METHOD_WRITABLE_REGISTRY_REGISTER;
    private static Method METHOD_REGISTRY_GET_HOLDER_SET;

    // Legacy (pre-1.20.5)
    private static Class<?> CLASS_NMS_ENCHANTMENT;
    private static Object REGISTRY_ENCHANTMENT_INSTANCE;
    private static Field FIELD_REGISTRY_FROZEN;
    private static final Method METHOD_REGISTRY_GET_KEY;

    static {
        try {
            String serverPackage = Bukkit.getServer().getClass().getPackage().getName();
            NMS_VERSION = serverPackage.substring(serverPackage.lastIndexOf('.') + 1);
            IS_MODERN_REGISTRATION = ReflectFinder.findClass("net.minecraft.world.item.enchantment.Enchantment$EnchantmentDefinition") != null;

            CLASS_RESOURCE_LOCATION = requireFound(ReflectFinder.findClass("net.minecraft.resources.ResourceLocation"), "NMS class ResourceLocation not found.");
            CONSTRUCTOR_RESOURCE_LOCATION = requireFound(ReflectFinder.findConstructor(CLASS_RESOURCE_LOCATION, String.class, String.class), "ResourceLocation(String,String) constructor not found.");
            CLASS_REGISTRY = requireFound(ReflectFinder.findClass("net.minecraft.core.Registry"), "NMS class Registry not found.");
            CLASS_CRAFT_ITEM_STACK = requireFound(ReflectFinder.findClass("org.bukkit.craftbukkit." + NMS_VERSION + ".inventory.CraftItemStack"), "CraftItemStack not found for version " + NMS_VERSION);
            Class<?> nmsItemStackClass = requireFound(ReflectFinder.findClass("net.minecraft.world.item.ItemStack"), "NMS ItemStack class not found.");
            METHOD_AS_BUKKIT_COPY = requireFound(ReflectFinder.findMethod(CLASS_CRAFT_ITEM_STACK, "asBukkitCopy", nmsItemStackClass), "CraftItemStack.asBukkitCopy(ItemStack) not found.");

            METHOD_REGISTRY_GET_KEY = requireFound(ReflectFinder.findMethod(CLASS_REGISTRY, "getKey", Object.class), "Registry.getKey(T) not found.");

            if (IS_MODERN_REGISTRATION) {
                initializeModernHandles();
                SnowLibrary.instance.getLogger().info("Using Modern (1.20.5+) enchantment registration method.");
            } else {
                initializeLegacyHandles();
                SnowLibrary.instance.getLogger().info("Using Legacy (pre-1.20.5) enchantment registration method.");
            }
            SnowLibrary.instance.getLogger().info("Successfully hooked into NMS for custom enchantments.");
        } catch (Exception e) {
            SnowLibrary.instance.getLogger().log(Level.SEVERE, "Failed to initialize NMS hooks. Custom enchantments will not work.", e);
            throw new RuntimeException("NMS Initialization Failed", e);
        }
    }

    private static void initializeModernHandles() {
        Class<?> enchantmentClass = requireFound(ReflectFinder.findClass("net.minecraft.world.item.enchantment.Enchantment"), "NMS Enchantment class not found.");
        Class<?> definitionClass = requireFound(ReflectFinder.findClass("net.minecraft.world.item.enchantment.Enchantment$EnchantmentDefinition"), "EnchantmentDefinition class not found.");
        CONSTRUCTOR_NMS_ENCHANTMENT_MODERN = requireFound(ReflectFinder.findConstructor(enchantmentClass, definitionClass), "Enchantment(EnchantmentDefinition) constructor not found.");

        Class<?> holderSetClass = requireFound(ReflectFinder.findClass("net.minecraft.core.HolderSet"), "HolderSet class not found.");
        Class<?> costClass = requireFound(ReflectFinder.findClass("net.minecraft.world.item.enchantment.Enchantment$Cost"), "Enchantment$Cost class not found.");
        Class<?> slotGroupClass = requireFound(ReflectFinder.findClass("net.minecraft.world.entity.EquipmentSlotGroup"), "EquipmentSlotGroup class not found.");
        Class<?> registryAccessClass = requireFound(ReflectFinder.findClass("net.minecraft.core.RegistryAccess"), "RegistryAccess class not found.");
        Class<?> resourceKeyClass = requireFound(ReflectFinder.findClass("net.minecraft.resources.ResourceKey"), "ResourceKey class not found.");
        Class<?> tagKeyClass = requireFound(ReflectFinder.findClass("net.minecraft.tags.TagKey"), "TagKey class not found.");
        CLASS_REGISTRIES = requireFound(ReflectFinder.findClass("net.minecraft.core.registries.Registries"), "Registries helper class not found.");
        Class<?> registryLayerClass = requireFound(ReflectFinder.findClass("net.minecraft.server.RegistryLayer"), "RegistryLayer class not found.");
        CLASS_ITEM_TAGS = requireFound(ReflectFinder.findClass("net.minecraft.tags.ItemTags"), "ItemTags class not found.");

        CONSTRUCTOR_ENCHANTMENT_COST = requireFound(ReflectFinder.findConstructor(costClass, int.class, int.class), "Enchantment.Cost(int,int) constructor not found.");
        METHOD_ENCHANTMENT_DEFINITION = requireFound(ReflectFinder.findMethod(enchantmentClass, "definition", holderSetClass, int.class, int.class, costClass, costClass, int.class, slotGroupClass), "Enchantment.definition(...) factory method not found.");

        FIELD_REGISTRY_STATIC_ACCESS = requireFound(ReflectFinder.findField(registryLayerClass, "STATIC_ACCESS"), "RegistryLayer.STATIC_ACCESS field not found.");
        METHOD_REGISTRY_ACCESS_LOOKUP_OR_THROW = requireFound(ReflectFinder.findMethod(registryAccessClass, "lookupOrThrow", resourceKeyClass), "RegistryAccess.lookupOrThrow(ResourceKey) not found.");
        METHOD_WRITABLE_REGISTRY_REGISTER = requireFound(ReflectFinder.findMethod(CLASS_REGISTRY, "register", CLASS_RESOURCE_LOCATION, Object.class), "WritableRegistry.register(ResourceLocation, T) method not found.");
        METHOD_REGISTRY_GET_HOLDER_SET = requireFound(ReflectFinder.findMethod(CLASS_REGISTRY, "getHolderSet", tagKeyClass), "Registry.getHolderSet(TagKey) method not found.");
    }

    private static void initializeLegacyHandles() throws ReflectiveOperationException {
        CLASS_NMS_ENCHANTMENT = requireFound(ReflectFinder.findClass("net.minecraft.world.item.enchantment.Enchantment"), "Legacy NMS Enchantment class not found.");

        Class<?> builtInRegistriesClass = ReflectFinder.findClass("net.minecraft.core.registries.BuiltInRegistries");
        if (builtInRegistriesClass == null) builtInRegistriesClass = ReflectFinder.findClass("net.minecraft.data.registries.BuiltInRegistries");

        Field enchantmentRegistryField = requireFound(ReflectFinder.findField(builtInRegistriesClass, "ENCHANTMENT"), "BuiltInRegistries.ENCHANTMENT field not found.");
        REGISTRY_ENCHANTMENT_INSTANCE = enchantmentRegistryField.get(null);

        Class<?> mappedRegistryClass = requireFound(ReflectFinder.findClass("net.minecraft.core.MappedRegistry"), "MappedRegistry class not found for legacy registry manipulation.");
        FIELD_REGISTRY_FROZEN = requireFound(ReflectFinder.findField(mappedRegistryClass, "frozen"), "MappedRegistry.frozen field not found.");
    }

    public static void register(SnowEnchantment enchant) {
        Objects.requireNonNull(enchant, "enchant cannot be null");
        final EnchantmentComponent component = enchant.getComponent();
        final NamespacedKey key = component.key;
        try {
            if (IS_MODERN_REGISTRATION) {
                registerModern(enchant);
            } else {
                registerLegacy(enchant);
            }
            SnowLibrary.instance.getLogger().info("Successfully registered custom enchantment: " + key);
        } catch (Exception e) {
            SnowLibrary.instance.getLogger().log(Level.SEVERE, "Failed to register custom enchantment: " + key, e);
            throw new RuntimeException("Failed to register enchantment " + key, e);
        }
    }

    private static void registerModern(SnowEnchantment enchant) throws Exception {
        EnchantmentComponent component = enchant.getComponent();
        NamespacedKey key = component.key;
        Object nmsId = CONSTRUCTOR_RESOURCE_LOCATION.newInstance(key.getNamespace(), key.getKey());

        Object staticRegistryAccess = FIELD_REGISTRY_STATIC_ACCESS.get(null);
        Object itemRegistryKey = requireFound(ReflectFinder.findField(CLASS_REGISTRIES, "ITEM").get(null), "Registries.ITEM key not found.");
        Object itemRegistry = METHOD_REGISTRY_ACCESS_LOOKUP_OR_THROW.invoke(staticRegistryAccess, itemRegistryKey);

        Object targetTagKey = getTagKeyForTarget(component.targetSlot);
        Object supportedItemsHolderSet = METHOD_REGISTRY_GET_HOLDER_SET.invoke(itemRegistry, targetTagKey);

        Object minCost = CONSTRUCTOR_ENCHANTMENT_COST.newInstance(component.minCost.apply(1), 0);
        Object maxCost = CONSTRUCTOR_ENCHANTMENT_COST.newInstance(component.maxCost.apply(1), 0);

        // component.targetSlot은 이제 snowlib의 커스텀 타입이므로 getApplicableSlots()를 호출할 수 있습니다.
        Object[] slotGroups = convertSlotsToGroups(component.targetSlot.getApplicableSlots());

        Object definition = METHOD_ENCHANTMENT_DEFINITION.invoke(null, supportedItemsHolderSet, component.weight, component.maxLevel, minCost, maxCost, component.anvilCost, slotGroups);

        Object enchantmentRegistryKey = requireFound(ReflectFinder.findField(CLASS_REGISTRIES, "ENCHANTMENT").get(null), "Registries.ENCHANTMENT key not found.");
        Object enchantmentRegistry = METHOD_REGISTRY_ACCESS_LOOKUP_OR_THROW.invoke(staticRegistryAccess, enchantmentRegistryKey);

        Object nmsEnchantment = CONSTRUCTOR_NMS_ENCHANTMENT_MODERN.newInstance(definition);

        METHOD_WRITABLE_REGISTRY_REGISTER.invoke(enchantmentRegistry, nmsId, nmsEnchantment);
    }

    private static void registerLegacy(SnowEnchantment enchant) throws Exception {
        EnchantmentComponent component = enchant.getComponent();
        NamespacedKey key = component.key;

        // snowlib의 커스텀 EnchantmentTarget에서 직접 EquipmentSlot 배열을 가져옵니다.

        InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
            case "getMaxLevel" -> component.maxLevel;
            case "getMinCost" -> component.minCost.apply((Integer) args[0]);
            case "getMaxCost" -> component.maxCost.apply((Integer) args[0]);
            case "isTreasureOnly" -> component.treasure;
            case "isCurse" -> component.curse;
            case "canEnchant" -> component.applicableItems.test((ItemStack) METHOD_AS_BUKKIT_COPY.invoke(null, args[0]));
            case "checkCompatibility" -> {
                Enchantment other = getBukkitEnchantment(args[0]);
                // Predicate<String>에 맞게 인챈트의 키 문자열을 전달합니다.
                yield other != null && !component.conflicts.test(other.getKey().getKey());
            }
            case "equals" -> proxy == args[0];
            case "hashCode" -> System.identityHashCode(proxy);
            case "toString" -> "Proxy<SnowEnchantment:" + key + ">";
            default -> {
                Class<?> returnType = method.getReturnType();
                if (returnType.isPrimitive()) yield returnType.equals(boolean.class) ? false : 0;
                yield null;
            }
        };

        Object nmsEnchantment = ReflectFinder.createProxy(CLASS_NMS_ENCHANTMENT, handler);

        Object nmsKey = CONSTRUCTOR_RESOURCE_LOCATION.newInstance(key.getNamespace(), key.getKey());

        FIELD_REGISTRY_FROZEN.setAccessible(true);
        FIELD_REGISTRY_FROZEN.set(REGISTRY_ENCHANTMENT_INSTANCE, false);

        Method registerMethod = requireFound(ReflectFinder.findMethod(CLASS_REGISTRY, "register", CLASS_REGISTRY, CLASS_RESOURCE_LOCATION, Object.class), "Registry.register(...) method not found for legacy registration.");
        registerMethod.invoke(null, REGISTRY_ENCHANTMENT_INSTANCE, nmsKey, nmsEnchantment);

        FIELD_REGISTRY_FROZEN.set(REGISTRY_ENCHANTMENT_INSTANCE, true);
    }

    private static Object[] convertSlotsToGroups(EquipmentSlot[] slots) throws ReflectiveOperationException {
        if (slots == null || slots.length == 0) {
            return new Object[]{getSlotGroup("ANY")};
        }
        List<Object> groups = new ArrayList<>();
        boolean hasHand = false, hasArmor = false;
        for (EquipmentSlot s : slots) {
            switch (s) {
                case HAND, OFF_HAND -> hasHand = true;
                case HEAD, CHEST, LEGS, FEET -> hasArmor = true;
                default -> {}
            }
        }
        if (hasHand) groups.add(getSlotGroup("MAINHAND"));
        if (hasArmor) groups.add(getSlotGroup("ARMOR"));
        if (groups.isEmpty()) groups.add(getSlotGroup("ANY"));
        return groups.toArray();
    }

    private static Object getSlotGroup(String name) throws ReflectiveOperationException {
        Class<?> slotGroupClass = requireFound(ReflectFinder.findClass("net.minecraft.world.entity.EquipmentSlotGroup"), "EquipmentSlotGroup class not found.");
        Field field = requireFound(ReflectFinder.findField(slotGroupClass, name), "EquipmentSlotGroup." + name + " field not found.");
        return field.get(null);
    }

    // 이제 snowlib의 커스텀 EnchantmentTarget을 파라미터로 받습니다.
    private static Object getTagKeyForTarget(EnchantmentTarget target) throws ReflectiveOperationException {
        String tagName = switch (target) {
            case ARMOR, ARMOR_FEET, ARMOR_LEGS, ARMOR_TORSO, ARMOR_HEAD -> "ARMOR_ENCHANTABLE";
            case WEAPON -> "SWORD_ENCHANTABLE";
            case TOOL -> "MINING_ENCHANTABLE";
            case BOW -> "BOW_ENCHANTABLE";
            case CROSSBOW -> "CROSSBOW_ENCHANTABLE";
            case TRIDENT -> "TRIDENT_ENCHANTABLE";
            case FISHING_ROD -> "FISHING_ENCHANTABLE";
            case WEARABLE -> "EQUIPPABLE_ENCHANTABLE";
            default -> "DURABILITY_ENCHANTABLE";
        };
        Field tagField = requireFound(ReflectFinder.findField(CLASS_ITEM_TAGS, tagName), "ItemTags." + tagName + " field not found.");
        return tagField.get(null);
    }

    private static <T> T requireFound(T obj, String message) {
        if (obj == null) throw new RuntimeException(message);
        return obj;
    }

    public static Enchantment getBukkitEnchantment(Object nmsEnchantment) {
        // 이 메소드는 다른 인챈트와의 호환성을 검사하는 로직(checkCompatibility)에서 주로 사용됩니다.
        // 버전 분기 처리를 통해 1.20.5 이전과 이후 환경 모두에서 안정적으로 작동하도록 수정되었습니다.
        try {
            if (nmsEnchantment == null) {
                return null;
            }

            Object resourceLocation;

            if (IS_MODERN_REGISTRATION) {
                // Modern (1.20.5+): 동적으로 가져온 인챈트 레지스트리를 사용해야 합니다.
                // 이 레지스트리는 서버가 시작될 때 `RegistryAccess`를 통해 조회됩니다.
                Object staticRegistryAccess = FIELD_REGISTRY_STATIC_ACCESS.get(null);
                Object enchantmentRegistryKey = ReflectFinder.findField(CLASS_REGISTRIES, "ENCHANTMENT").get(null);
                Object enchantmentRegistry = METHOD_REGISTRY_ACCESS_LOOKUP_OR_THROW.invoke(staticRegistryAccess, enchantmentRegistryKey);

                // 공통 `getKey` 메소드를 현대적인 레지스트리 인스턴스에 대해 호출합니다.
                resourceLocation = METHOD_REGISTRY_GET_KEY.invoke(enchantmentRegistry, nmsEnchantment);

            } else {
                // Legacy (pre-1.20.5): 기존 방식대로 BuiltInRegistries의 정적 인스턴스를 사용합니다.
                resourceLocation = METHOD_REGISTRY_GET_KEY.invoke(REGISTRY_ENCHANTMENT_INSTANCE, nmsEnchantment);
            }

            if (resourceLocation == null) {
                // 레지스트리에 등록되지 않은 인챈트일 수 있으므로, null을 반환합니다.
                return null;
            }

            // --- 여기서부터는 NMS ResourceLocation을 Bukkit NamespacedKey로 변환하는 공통 로직입니다 ---

            // ResourceLocation에서 namespace와 path를 추출합니다.
            Method getNamespaceMethod = requireFound(ReflectFinder.findMethod(CLASS_RESOURCE_LOCATION, "getNamespace"), "ResourceLocation.getNamespace() not found");
            Method getPathMethod = requireFound(ReflectFinder.findMethod(CLASS_RESOURCE_LOCATION, "getPath"), "ResourceLocation.getPath() not found");

            String namespace = (String) getNamespaceMethod.invoke(resourceLocation);
            String path = (String) getPathMethod.invoke(resourceLocation);

            // Bukkit의 NamespacedKey를 생성합니다.
            NamespacedKey key = new NamespacedKey(namespace, path);

            // Paper API를 사용하여 안전하게 Bukkit Enchantment 객체를 가져옵니다.
            // getOrThrow 대신 get을 사용하여, 키가 존재하지 않을 경우 예외 대신 null을 반환하도록 합니다.
            return RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT).get(key);

        } catch (Exception e) {
            SnowLibrary.instance.getLogger().log(Level.WARNING, "Failed to convert NMS enchantment to Bukkit enchantment", e);
            return null;
        }
    }
}
