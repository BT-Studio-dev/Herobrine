package dev.btstudio.herobrine.event;

import java.util.ArrayList;
import java.util.List;

import dev.btstudio.herobrine.entity.HerobrineEntity;
import dev.btstudio.herobrine.sound.ModSounds;
import dev.btstudio.herobrine.world.Feature;
import dev.btstudio.herobrine.world.HerobrineSettings;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/**
 * The Notch Apple ritual, straight out of <i>Him - Herobrine</i>: throw one into a fire burning on
 * netherrack and the apple is consumed.
 *
 * <p>If he is around, he is banished to the void - every apparition in that dimension is gone and
 * nothing else happens until the ritual is repeated. If he is already gone, burning the apple
 * calls him back.
 *
 * <p>The apple can be crafted with the expensive recipe shipped in
 * {@code data/herobrine/recipe/notch_apple.json} (gold nuggets, nether star, dragon head and diamond
 * blocks around an apple), which is what "the Notch Apple" means here.
 */
public final class NotchAppleRitual {
    /** One check per second is plenty for something that is not going anywhere. */
    private static final int CHECK_INTERVAL = 20;

    private NotchAppleRitual() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % CHECK_INTERVAL != 0) {
                return;
            }

            for (ServerLevel level : server.getAllLevels()) {
                if (HerobrineSettings.enabled(level, Feature.NOTCH_APPLE)) {
                    offerBurningApples(server, level);
                }
            }
        });
    }

    private static void offerBurningApples(MinecraftServer server, ServerLevel level) {
        List<ItemEntity> offerings = new ArrayList<>();

        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof ItemEntity item && isBurningOffering(level, item)) {
                offerings.add(item);
            }
        }

        for (ItemEntity offering : offerings) {
            performRitual(server, level, offering);
        }
    }

    /** A Notch Apple sitting in a fire that is burning on top of netherrack. */
    private static boolean isBurningOffering(ServerLevel level, ItemEntity item) {
        ItemStack stack = item.getItem();

        if (stack.isEmpty() || !stack.is(Items.ENCHANTED_GOLDEN_APPLE)) {
            return false;
        }

        BlockPos pos = item.blockPosition();

        return level.getBlockState(pos).is(Blocks.FIRE)
                && level.getBlockState(pos.below()).is(Blocks.NETHERRACK);
    }

    private static void performRitual(MinecraftServer server, ServerLevel level, ItemEntity offering) {
        BlockPos pos = offering.blockPosition();
        boolean banished = HerobrineSettings.toggleBanished(level);

        // The offering is consumed and the flame goes out.
        offering.discard();
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);

        level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 0.6F);
        level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                ModSounds.VOID, SoundSource.HOSTILE, 1.0F, 1.0F);

        if (banished) {
            purgeApparitions(level);
        }

        server.getPlayerList().broadcastSystemMessage(
                Component.translatable(banished ? "herobrine.message.banished" : "herobrine.message.returned")
                        .withStyle(banished ? ChatFormatting.GRAY : ChatFormatting.DARK_RED),
                false);
    }

    /** Everything of his standing in this dimension goes with him. */
    public static void purgeApparitions(ServerLevel level) {
        List<HerobrineEntity> apparitions = new ArrayList<>();

        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof HerobrineEntity herobrine) {
                apparitions.add(herobrine);
            }
        }

        for (HerobrineEntity herobrine : apparitions) {
            herobrine.discard();
        }
    }
}
