package dev.btstudio.herobrine.event;

import dev.btstudio.herobrine.entity.Apparition;
import dev.btstudio.herobrine.sound.ModSounds;
import dev.btstudio.herobrine.world.HerobrineSettings;

import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

/**
 * The bed sighting from <i>From The Fog</i>: sometimes, when you get into bed, he is already standing
 * at the foot of it, watching you try to sleep.
 *
 * <p>This is the one place where he is allowed near a bed - {@code ALLOW_SLEEPING} refuses to let you
 * sleep while he is within 48 blocks, and this spawns after that check has already passed. The form
 * used is the dwelling one, so he will not simply evaporate in a dark bedroom.
 */
public final class BedWatcher {
    /** One bedtime in five. */
    private static final float CHANCE = 0.2F;

    private static final int MIN_DISTANCE = 3;
    private static final int MAX_DISTANCE = 6;

    private BedWatcher() {
    }

    public static void register() {
        EntitySleepEvents.START_SLEEPING.register((entity, sleepingPos) -> {
            if (!(entity instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) {
                return;
            }

            if (HerobrineSettings.banished(level) || level.getRandom().nextFloat() >= CHANCE) {
                return;
            }

            for (int attempt = 0; attempt < 12; attempt++) {
                Direction direction = Direction.Plane.HORIZONTAL.getRandomDirection(level.getRandom());
                int distance = MIN_DISTANCE + level.getRandom().nextInt(MAX_DISTANCE - MIN_DISTANCE + 1);
                BlockPos spot = standingSpot(level, sleepingPos.relative(direction, distance));

                if (spot == null) {
                    continue;
                }

                if (Apparitions.spawnAt(level, spot, Apparition.DWELLING, player) != null) {
                    level.playSound(null, spot.getX() + 0.5, spot.getY() + 0.5, spot.getZ() + 0.5,
                            ModSounds.CAUTION, SoundSource.AMBIENT, 0.9F, 0.8F);
                    player.sendSystemMessage(Component.translatable("herobrine.bed.watched"));
                    return;
                }
            }
        });
    }

    /** Two blocks of air over solid ground, near the block that was asked for. */
    private static BlockPos standingSpot(ServerLevel level, BlockPos around) {
        for (int dy = 1; dy >= -3; dy--) {
            BlockPos pos = around.offset(0, dy, 0);

            if (level.getBlockState(pos).isAir()
                    && level.getBlockState(pos.above()).isAir()
                    && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) {
                return pos;
            }
        }

        return null;
    }
}
