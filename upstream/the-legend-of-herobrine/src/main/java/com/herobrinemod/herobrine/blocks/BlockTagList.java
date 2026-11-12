package com.herobrinemod.herobrine.blocks;

import com.herobrinemod.herobrine.HerobrineMod;
import net.minecraft.block.Block;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

public class BlockTagList {
    public static final TagKey<Block> INCORRECT_FOR_CURSED_DIAMOND_TOOL = TagKey.of(RegistryKeys.BLOCK, Identifier.of(HerobrineMod.MODID, "incorrect_for_cursed_diamond_tool"));
    public static final TagKey<Block> NEEDS_CURSED_DIAMOND_TOOL = TagKey.of(RegistryKeys.BLOCK, Identifier.of(HerobrineMod.MODID, "needs_cursed_diamond_tool"));
}