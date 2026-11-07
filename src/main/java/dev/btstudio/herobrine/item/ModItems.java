package dev.btstudio.herobrine.item;

import dev.btstudio.herobrine.HerobrineMod;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.ArmorType;

/**
 * The items of <i>Herobrine: Forgotten</i>, and the iron-stick gear hammered out of them:
 *
 * <ul>
 *   <li>{@link #IRON_STICK} - three iron ingots hammered into four sticks.</li>
 *   <li>{@link #IRON_SWORD} - three iron sticks hammered into a blade: iron all the way down, and
 *       he does not like it.</li>
 *   <li>{@link #IRON_HELMET}, {@link #IRON_CHESTPLATE}, {@link #IRON_LEGGINGS}, {@link #IRON_BOOTS}
 *       - the full iron-stick set. It protects like iron armour, and wearing it makes him think
 *       twice about showing himself.</li>
 *   <li>{@link #HOLY_WATER} - only found in the churches that stand in plains; bottled from the
 *       cauldron on the altar.</li>
 *   <li>{@link #THE_CROSS} - an iron stick cross around a bottle of holy water. Right-click
 *       Herobrine with it and he is crucified... or he is not.</li>
 * </ul>
 *
 * <p>Every piece of the iron gear repels him: see {@link dev.btstudio.herobrine.event.IronWard}.
 *
 * <p>26.3 items need their id in the properties before construction, and the components are
 * initialised in the constructor, so the keys are built first and the same keys are used for the
 * registry entries. Swords and armour are plain {@link Item}s in 26.3 - the blade and the plating
 * live in the properties, not in a subclass.
 */
public final class ModItems {
    /** The sword hits exactly as hard and as fast as the vanilla one: it is the same iron. */
    private static final float SWORD_ATTACK_DAMAGE = 3.0F;
    private static final float SWORD_ATTACK_SPEED = -2.4F;

    public static final ResourceKey<Item> IRON_STICK_KEY = key("iron_stick");
    public static final ResourceKey<Item> IRON_SWORD_KEY = key("iron_sword");
    public static final ResourceKey<Item> IRON_HELMET_KEY = key("iron_helmet");
    public static final ResourceKey<Item> IRON_CHESTPLATE_KEY = key("iron_chestplate");
    public static final ResourceKey<Item> IRON_LEGGINGS_KEY = key("iron_leggings");
    public static final ResourceKey<Item> IRON_BOOTS_KEY = key("iron_boots");
    public static final ResourceKey<Item> HOLY_WATER_KEY = key("holy_water");
    public static final ResourceKey<Item> THE_CROSS_KEY = key("the_cross");

    public static final Item IRON_STICK = new Item(new Item.Properties().setId(IRON_STICK_KEY));
    public static final Item IRON_SWORD = new Item(new Item.Properties()
            .setId(IRON_SWORD_KEY)
            .sword(ToolMaterial.IRON, SWORD_ATTACK_DAMAGE, SWORD_ATTACK_SPEED));
    public static final Item IRON_HELMET = armour(IRON_HELMET_KEY, ArmorType.HELMET);
    public static final Item IRON_CHESTPLATE = armour(IRON_CHESTPLATE_KEY, ArmorType.CHESTPLATE);
    public static final Item IRON_LEGGINGS = armour(IRON_LEGGINGS_KEY, ArmorType.LEGGINGS);
    public static final Item IRON_BOOTS = armour(IRON_BOOTS_KEY, ArmorType.BOOTS);
    public static final Item HOLY_WATER = new Item(new Item.Properties().setId(HOLY_WATER_KEY));
    public static final Item THE_CROSS = new CrossItem(new Item.Properties()
            .setId(THE_CROSS_KEY)
            .stacksTo(1));

    private ModItems() {
    }

    /**
     * One piece of the iron-stick set: iron armour, hammered from sticks instead of ingots, so it
     * keeps iron's protection, durability, enchantability and equip sound.
     */
    private static Item armour(ResourceKey<Item> key, ArmorType type) {
        return new Item(new Item.Properties()
                .setId(key)
                .humanoidArmor(ArmorMaterials.IRON, type));
    }

    private static ResourceKey<Item> key(String name) {
        return ResourceKey.create(Registries.ITEM, HerobrineMod.id(name));
    }

    public static void register() {
        Registry.register(BuiltInRegistries.ITEM, IRON_STICK_KEY, IRON_STICK);
        Registry.register(BuiltInRegistries.ITEM, IRON_SWORD_KEY, IRON_SWORD);
        Registry.register(BuiltInRegistries.ITEM, IRON_HELMET_KEY, IRON_HELMET);
        Registry.register(BuiltInRegistries.ITEM, IRON_CHESTPLATE_KEY, IRON_CHESTPLATE);
        Registry.register(BuiltInRegistries.ITEM, IRON_LEGGINGS_KEY, IRON_LEGGINGS);
        Registry.register(BuiltInRegistries.ITEM, IRON_BOOTS_KEY, IRON_BOOTS);
        Registry.register(BuiltInRegistries.ITEM, HOLY_WATER_KEY, HOLY_WATER);
        Registry.register(BuiltInRegistries.ITEM, THE_CROSS_KEY, THE_CROSS);
    }
}
