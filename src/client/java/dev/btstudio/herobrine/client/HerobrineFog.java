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
 * fog, and {@link HerobrineFogOverlay} draws it in screen space when that path never ran - which is
 * what happens under a shader pack, because a shader pack owns the fog pipeline outright.
 */
public final class HerobrineFog {
    /** He has to be inside this many blocks before the fog starts to move in at all. */
    private static final double RANGE = 48.0;

    /** Set every time the vanilla fog path actually took our values this frame. */
    private static boolean applied;

    private HerobrineFog() {
    }

    /**
     * Called by {@code FogRendererMixin} once it has put our distances into the fog data. A shader
     * pack replaces the whole fog pipeline, so this simply never happens while one is active - and
     * that absence is exactly how the overlay knows to take over.
     */
    public static void noteApplied() {
        applied = true;
    }

    /**
     * Whether the fog was really thickened this frame, which the HUD overlay asks once per frame.
     * Reading it clears it, so the answer belongs to one frame only.
     */
    public static boolean consumeApplied() {
        boolean wasApplied = applied;
        applied = false;
        return wasApplied;
    }

    /** 0 when he is nowhere near, 1 when he is standing in front of you. */
    public static float closeness(Entity cameraEntity) {
        Player player = Minecraft.getInstance().player;

        if (player == null || player.level() == null || cameraEntity == null) {
            return 0.0F;
        }

        // The one setting this class cannot work out for itself: whether the fog is switched on
        // in the dimension the client is standing in.
        if (!HerobrineClientState.fogClosesIn()) {
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
