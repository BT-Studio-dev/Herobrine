package dev.btstudio.herobrine.event;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import dev.btstudio.herobrine.world.Feature;
import dev.btstudio.herobrine.world.HerobrineSettings;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.Animal;

/**
 * Escalation: the grudge he keeps.
 *
 * <p>Left alone, the haunting only ever changes with the clock, and a hundred days in looks exactly
 * like the first night. This is the other way round: a score that goes up when you provoke him and
 * back down when you ward him off, and four tiers hanging off it. Provoking him is killing animals
 * and lingering below ground; warding him off is carrying iron. The tiers are what everything else
 * already reads as the escalation stage, so a world at tier three simply gets him more often - and
 * more often in a temper - than a world at tier nothing.
 *
 * <p>The score is kept per dimension alongside the rest of the settings, so it survives a restart
 * and a reload, and it is behind its own switch: with {@link Feature#ESCALATION} off nothing here
 * runs and the haunting escalates on time alone, exactly as it always did.
 */
public final class Dread {
    /** Four tiers, evenly spread across {@link HerobrineSettings#MAX_DREAD}. */
    private static final int TIER_SIZE = HerobrineSettings.MAX_DREAD / 4;
    public static final int MAX_TIER = 3;

    /** Killing an animal is the one thing the book in every version of this story warns about. */
    private static final int KILL_GAIN = 2;

    /** Below this height you are down in the dark with him, and he notices. */
    private static final int UNDERGROUND_Y = 60;
    private static final int UNDERGROUND_GAIN = 1;
    /** Seconds below ground per point: an hour and a half in the caves is a tier on its own. */
    private static final int UNDERGROUND_SECONDS = 90;

    /** Any iron on you at all: one piece of the set, or a stick or sword in either hand. */
    private static final float WARD_THRESHOLD = 0.10F;
    private static final int WARD_RELIEF = 2;
    /** Seconds warded per relief: a quarter of an hour carrying iron undoes a tier. */
    private static final int WARD_SECONDS = 60;

    /** Seconds spent below ground since the last point, per player. */
    private static final Map<UUID, Integer> UNDERGROUND_CLOCK = new HashMap<>();
    /** Seconds spent warded since the last relief, per player. */
    private static final Map<UUID, Integer> WARD_CLOCK = new HashMap<>();

    private Dread() {
    }

    /** The tier a score sits in, 0 to {@value #MAX_TIER}. */
    public static int tierOf(int dread) {
        return Math.min(MAX_TIER, Math.max(0, dread / TIER_SIZE));
    }

    /** The killing of animals, which is the one provocation that is not simply being somewhere. */
    public static void register() {
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity instanceof Animal) || !(source.getEntity() instanceof ServerPlayer player)) {
                return;
            }

            HerobrineSettings settings = HerobrineSettings.of(player.level());

            if (settings.enabled(Feature.ESCALATION) && !settings.isBanished()) {
                settings.addDread(KILL_GAIN);
            }
        });
    }

    /**
     * One second of a player's life, called from the haunting's own tick. The two clocks are kept
     * in seconds rather than game time, and are dropped when the player leaves, so that coming back
     * does not hand out a point for time spent down there before logging out.
     */
    public static void tick(ServerLevel level, ServerPlayer player, HerobrineSettings settings) {
        if (!settings.enabled(Feature.ESCALATION) || settings.isBanished()) {
            return;
        }

        UUID id = player.getUUID();

        if (player.getY() < UNDERGROUND_Y) {
            if (UNDERGROUND_CLOCK.merge(id, 1, Integer::sum) >= UNDERGROUND_SECONDS) {
                UNDERGROUND_CLOCK.remove(id);
                settings.addDread(UNDERGROUND_GAIN);
            }
        } else {
            UNDERGROUND_CLOCK.remove(id);
        }

        if (IronWard.wardOf(player) >= WARD_THRESHOLD) {
            if (WARD_CLOCK.merge(id, 1, Integer::sum) >= WARD_SECONDS) {
                WARD_CLOCK.remove(id);
                settings.addDread(-WARD_RELIEF);
            }
        } else {
            WARD_CLOCK.remove(id);
        }
    }

    /**
     * Drops what we were holding about a player who has left. Both clocks are abandoned rather
     * than banked: coming back into the same cave starts the ninety seconds again rather than
     * handing out a point for time spent down there before you logged out.
     */
    public static void forget(UUID id) {
        UNDERGROUND_CLOCK.remove(id);
        WARD_CLOCK.remove(id);
    }
}
