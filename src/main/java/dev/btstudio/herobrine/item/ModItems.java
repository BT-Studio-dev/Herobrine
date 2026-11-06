package dev.btstudio.herobrine.item;

import dev.btstudio.herobrine.HerobrineMod;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

/**
 * The three items of <i>Herobrine: Forgotten</i>:
 *
 * <ul>
 *   <li>{@link #IRON_STICK} - three iron ingots hammered into four sticks.</li>
 *   <li>{@link #HOLY_WATER} - only found in the churches that stand in plains; bottled from the
 *       cauldron on the altar.</li>
 *   <li>{@link #THE_CROSS} - an iron stick cross around a bottle of holy water. Right-click
 *       Herobrine with it and he is crucified... or he is not.</li>
 * </ul>
 *
 * <p>26.3 items need their id in the properties before construction, and the components are
 * initialised in the constructor, so the keys are built first and the same keys are used for the
 * registry entries.
 */
public final class ModItems {
    public static final ResourceKey<Item> IRON_STICK_KEY = key("iron_stick");
    public static final ResourceKey<Item> HOLY_WATER_KEY = key("holy_water");
    public static final ResourceKey<Item> THE_CROSS_KEY = key("the_cross");

    public static final Item IRON_STICK = new Item(new Item.Properties().setId(IRON_STICK_KEY));
    public static final Item HOLY_WATER = new Item(new Item.Properties().setId(HOLY_WATER_KEY));
    public static final Item THE_CROSS = new CrossItem(new Item.Properties()
            .setId(THE_CROSS_KEY)
            .stacksTo(1));

    private ModItems() {
    }

    private static ResourceKey<Item> key(String name) {
        return ResourceKey.create(Registries.ITEM, HerobrineMod.id(name));
    }

    public static void register() {
        Registry.register(BuiltInRegistries.ITEM, IRON_STICK_KEY, IRON_STICK);
        Registry.register(BuiltInRegistries.ITEM, HOLY_WATER_KEY, HOLY_WATER);
        Registry.register(BuiltInRegistries.ITEM, THE_CROSS_KEY, THE_CROSS);
    }
}
