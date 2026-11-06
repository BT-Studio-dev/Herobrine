package dev.btstudio.herobrine.sound;

import dev.btstudio.herobrine.HerobrineMod;

import net.minecraft.sounds.SoundEvent;

/**
 * The mod's own sound set - every sound in here is generated for this mod (no audio is taken from
 * any other mod or resource pack) and is played instead of the vanilla placeholders the earlier
 * versions used.
 *
 * <p>The ogg files live in {@code assets/herobrine/sounds/} and the event names, categories and
 * subtitles are in {@code assets/herobrine/sounds.json}.
 */
public final class ModSounds {
    /** Low, slow drone that follows you around once he is close. */
    public static final SoundEvent AMBIENT = sound("ambient");
    /** Breathy, syllable-shaped whispering. */
    public static final SoundEvent WHISPER = sound("whisper");
    /** Three gravel-ish footsteps that stop when you turn around. */
    public static final SoundEvent FOOTSTEP = sound("footstep");
    /** The jumpscare stinger. */
    public static final SoundEvent JUMPSCARE = sound("jumpscare");
    /** Two thumps, in your chest rather than in the world. */
    public static final SoundEvent HEARTBEAT = sound("heartbeat");
    /** A pickaxe in stone somewhere that no one is mining. */
    public static final SoundEvent MINING = sound("mining");
    /** Wind that rolls in when he appears, and the sound of him leaving. */
    public static final SoundEvent FOG = sound("fog");
    /** Corrupted, stuttering noise - "it did not work". */
    public static final SoundEvent GLITCH = sound("glitch");
    /** A bell for the cross. */
    public static final SoundEvent CHIME = sound("chime");
    /** Reverse swell and a low boom - banished to the void. */
    public static final SoundEvent VOID = sound("void");
    /** Fifty seconds of drone, wind, distant bells and something moving under the floor. */
    public static final SoundEvent MUSIC_HAUNT = sound("music_haunt");
    /** Two tense notes under a "caution" caption. */
    public static final SoundEvent CAUTION = sound("caution");

    private ModSounds() {
    }

    private static SoundEvent sound(String name) {
        return SoundEvent.createVariableRangeEvent(HerobrineMod.id(name));
    }

    /** Touching the class is enough to build the events; this makes that explicit and greppable. */
    public static void register() {
        // Variable range sound events need no registry entry of their own.
    }
}
