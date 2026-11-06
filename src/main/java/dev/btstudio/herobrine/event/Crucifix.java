package dev.btstudio.herobrine.event;

import dev.btstudio.herobrine.entity.HerobrineEntity;
import dev.btstudio.herobrine.sound.ModSounds;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Crucifying him, as in <i>Herobrine: Forgotten</i>: "you can crucifix it, but the result might not
 * be what you wanted".
 *
 * <p>Most of the time the cross works and he cannot stay: he is gone in a burst of smoke and every
 * apparition in the dimension goes with him. The rest of the time it does nothing except make him
 * drop the act - he turns into the hunter at full health and the cross is still used up.
 */
public final class Crucifix {
    /** Forty percent of the time it does not work. */
    private static final float FAILURE_CHANCE = 0.4F;

    private Crucifix() {
    }

    public static InteractionResult attempt(ServerLevel level, Player player, HerobrineEntity herobrine,
                                            ItemStack stack) {
        BlockPos pos = herobrine.blockPosition();
        boolean worked = level.getRandom().nextFloat() >= FAILURE_CHANCE;

        // The cross is spent either way: it is wood and iron, and it just met him.
        if (!player.isCreative()) {
            stack.shrink(1);
        }

        if (worked) {
            level.sendParticles(ParticleTypes.SMOKE, herobrine.getX(), herobrine.getY() + 1.0, herobrine.getZ(),
                    40, 0.4, 0.8, 0.4, 0.02);
            level.playSound(null, pos, ModSounds.CHIME, SoundSource.PLAYERS, 1.0F, 1.0F);
            level.playSound(null, pos, ModSounds.FOG, SoundSource.HOSTILE, 1.0F, 0.6F);

            herobrine.discard();
            NotchAppleRitual.purgeApparitions(level);

            player.sendSystemMessage(Component.translatable("herobrine.cross.success").withStyle(ChatFormatting.GRAY));
        } else {
            level.playSound(null, pos, ModSounds.GLITCH, SoundSource.HOSTILE, 1.2F, 1.2F);

            herobrine.setHostile(true);
            herobrine.setHealth(herobrine.getMaxHealth());

            player.sendSystemMessage(Component.translatable("herobrine.cross.failed").withStyle(ChatFormatting.DARK_RED));
        }

        return InteractionResult.SUCCESS;
    }
}
