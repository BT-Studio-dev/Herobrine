package com.herobrinemod.herobrine.items.material;

import com.herobrinemod.herobrine.blocks.BlockTagList;
import com.herobrinemod.herobrine.items.ItemList;
import net.minecraft.block.Block;
import net.minecraft.item.ToolMaterial;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.tag.TagKey;

public class CursedDiamondToolMaterial implements ToolMaterial {
    @Override
    public int getDurability() {
        return 1600;
    }

    @Override
    public float getMiningSpeedMultiplier() {
        return 9.0f;
    }

    @Override
    public float getAttackDamage() {
        return 3.0f;
    }

    @Override
    public TagKey<Block> getInverseTag() {
        return BlockTagList.INCORRECT_FOR_CURSED_DIAMOND_TOOL;
    }

    @Override
    public int getEnchantability() {
        return 22;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.ofItems(ItemList.CURSED_DIAMOND);
    }
}
