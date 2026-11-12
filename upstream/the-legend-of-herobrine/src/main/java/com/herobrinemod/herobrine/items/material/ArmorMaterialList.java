package com.herobrinemod.herobrine.items.material;

import com.herobrinemod.herobrine.HerobrineMod;
import com.herobrinemod.herobrine.items.ItemList;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.util.EnumMap;
import java.util.List;

public class ArmorMaterialList {
    public static final RegistryEntry<ArmorMaterial> CURSED_DIAMOND = Registry.registerReference(Registries.ARMOR_MATERIAL, Identifier.of(HerobrineMod.MODID, "cursed_diamond"), new ArmorMaterial(Util.make(new EnumMap(ArmorItem.Type.class), map -> {
        map.put(ArmorItem.Type.BOOTS, 3);
        map.put(ArmorItem.Type.LEGGINGS, 6);
        map.put(ArmorItem.Type.CHESTPLATE, 8);
        map.put(ArmorItem.Type.HELMET, 3);
        map.put(ArmorItem.Type.BODY, 11);
    }), /*Enchantability*/ 22, SoundEvents.ITEM_ARMOR_EQUIP_DIAMOND, () -> Ingredient.ofItems(ItemList.CURSED_DIAMOND), List.of(new ArmorMaterial.Layer(Identifier.of(HerobrineMod.MODID, "cursed_diamond"))), /*Armor Toughness*/ 2.0f, /*Knockback Resistance*/ 0.0f));
}
