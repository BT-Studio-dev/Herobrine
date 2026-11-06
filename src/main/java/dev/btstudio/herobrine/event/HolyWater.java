package dev.btstudio.herobrine.event;

import dev.btstudio.herobrine.item.ModItems;
import dev.btstudio.herobrine.world.HolySites;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/**
 * Bottling holy water: an empty glass bottle, right-clicked on the cauldron of a church, comes back
 * full. The cauldron is then empty and no longer holy - one bottle per church, which is why the
 * cross is such an expensive thing to make.
 */
public final class HolyWater {
    private HolyWater() {
    }

    public static void register() {
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (!(level instanceof ServerLevel serverLevel) || !player.getItemInHand(hand).is(Items.GLASS_BOTTLE)) {
                return InteractionResult.PASS;
            }

            BlockPos pos = hit.getBlockPos();

            if (!serverLevel.getBlockState(pos).is(Blocks.WATER_CAULDRON) || !HolySites.isHoly(serverLevel, pos)) {
                return InteractionResult.PASS;
            }

            HolySites.remove(serverLevel, pos);
            serverLevel.setBlock(pos, Blocks.CAULDRON.defaultBlockState(), 3);
            serverLevel.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);

            if (!player.isCreative()) {
                player.getItemInHand(hand).shrink(1);
            }

            ItemStack holy = new ItemStack(ModItems.HOLY_WATER);

            if (!player.getInventory().add(holy)) {
                serverLevel.addFreshEntity(new ItemEntity(serverLevel, player.getX(), player.getY() + 0.5,
                        player.getZ(), holy));
            }

            player.sendSystemMessage(Component.translatable("herobrine.holy_water.filled"));
            return InteractionResult.SUCCESS;
        });
    }
}
