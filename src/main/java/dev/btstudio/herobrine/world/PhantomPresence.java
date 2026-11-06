package dev.btstudio.herobrine.world;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Herobrine's invisible presence: a saved point in the world that refuses natural mob spawns within
 * {@link #RADIUS} blocks, just like the 24 block bubble a real player carries around.
 *
 * <p>This is the effect players use to "prove" Herobrine is in their world: the vanilla game already
 * does it around the world spawn point, and now it can happen anywhere. The suppression itself is
 * hooked into {@code NaturalSpawner} by {@code NaturalSpawnerMixin}, so it is the game's own spawn
 * rule - not a cleanup pass that deletes mobs afterwards.
 */
public final class PhantomPresence {
    /** Vanilla refuses natural spawns within 24 blocks of a player (24 * 24 = 576). */
    public static final double RADIUS = 24.0;

    private static final double RADIUS_SQR = RADIUS * RADIUS;

    private PhantomPresence() {
    }

    private static PhantomData data(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(PhantomData.TYPE);
    }

    public static List<BlockPos> positions(ServerLevel level) {
        return data(level).positions();
    }

    public static boolean add(ServerLevel level, BlockPos pos) {
        return data(level).add(pos);
    }

    public static int clear(ServerLevel level) {
        return data(level).clear();
    }

    public static double nearestDistanceSqr(ServerLevel level, BlockPos pos) {
        return data(level).nearestDistanceSqr(pos);
    }

    /** Called for every natural spawn attempt: {@code true} means "pretend a player stands here". */
    public static boolean blocksSpawnAt(ServerLevel level, BlockPos pos) {
        return data(level).isNear(pos, RADIUS_SQR);
    }
}
