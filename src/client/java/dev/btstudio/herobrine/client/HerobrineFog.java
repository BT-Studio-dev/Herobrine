package dev.btstudio.herobrine.client;

import java.util.List;

import dev.btstudio.herobrine.entity.HerobrineEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * "From the fog": how much the fog should close in, based on how close the nearest Herobrine is.
 *
 * <p>Read on the client from the entities the server already sends - no packets, no extra state, and
 * it costs one box query per frame at most. {@code FogRendererMixin} turns this number into actual
 * fog.
 */
public final class HerobrineFog {
    /** He has to be inside this many blocks before the fog starts to move in at all. */
    private static final double RANGE = 48.0;

    private HerobrineFog() {
    }

    /** 0 when he is nowhere near, 1 when he is standing in front of you. */
    public static float closeness(Entity cameraEntity) {
        Player player = Minecraft.getInstance().player;

        if (player == null || player.level() == null || cameraEntity == null) {
            return 0.0F;
        }

        List<HerobrineEntity> found = player.level().getEntitiesOfClass(HerobrineEntity.class,
                player.getBoundingBox().inflate(RANGE));

        double nearest = RANGE;

        for (HerobrineEntity herobrine : found) {
            nearest = Math.min(nearest, herobrine.distanceTo(player));
        }

        if (nearest >= RANGE) {
            return 0.0F;
        }

        return (float) (1.0 - nearest / RANGE);
    }
}
