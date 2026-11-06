package dev.btstudio.herobrine.mixin;

import dev.btstudio.herobrine.world.PhantomPresence;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.chunk.ChunkAccess;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes the natural spawner believe a player is standing at every phantom presence.
 *
 * <p>{@code isRightDistanceToPlayerAndSpawnPoint} is the routine that returns false inside the
 * 24 block "no spawning near a player" bubble (and inside the same bubble around the world spawn
 * point). Returning false from it for phantom positions reproduces that bubble exactly, with no
 * follow-up cleanup: the spawn never happens in the first place.
 */
@Mixin(NaturalSpawner.class)
public class NaturalSpawnerMixin {
    @Inject(method = "isRightDistanceToPlayerAndSpawnPoint", at = @At("HEAD"), cancellable = true)
    private static void herobrine$refuseSpawnsNearPhantoms(ServerLevel level, ChunkAccess chunk,
                                                           BlockPos.MutableBlockPos pos,
                                                           double nearestPlayerDistanceSqr,
                                                           CallbackInfoReturnable<Boolean> callback) {
        if (PhantomPresence.blocksSpawnAt(level, pos)) {
            callback.setReturnValue(false);
        }
    }
}
