package dev.btstudio.herobrine.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.btstudio.herobrine.HerobrineMod;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Per-dimension mod settings, saved with the world.
 *
 * <ul>
 *   <li><b>purist</b> - the <i>True Herobrine</i> mode: he never edits a block, never sends chat
 *       and never attacks. He only ever appears above Y 60 in the Overworld, stands still, watches
 *       for two minutes, and is gone the moment you come within 25 blocks.</li>
 *   <li><b>banished</b> - the <i>Him</i> ritual result. A Notch Apple burned on a netherrack fire
 *       sends him to the void: no apparitions, no sabotage, nothing at all until an apple is burned
 *       again to call him back.</li>
 *   <li><b>schizo</b> - the random footsteps and cave noises that follow you around.</li>
 *   <li><b>chaseUntil</b> - game time at which the current <i>chase mode</i> frenzy runs out. While
 *       it is running, apparitions come three times as often.</li>
 *   <li><b>herd</b> - the haunted herd: he may possess a passive animal and make it watch you.</li>
 *   <li><b>miner</b> - the ghost miner: pickaxe sounds in stone that nobody is making.</li>
 *   <li><b>music</b> - the fifty second haunting track that plays when he is close. Off by
 *       default on servers? No: on by default, at 70%.</li>
 *   <li><b>arson</b> - whether he may start a fire near you. The only event that can damage a
 *       build, so it is the one that gets its own switch, exposed in the settings screen.</li>
 *   <li><b>fog</b> - whether the bundled <i>From The Fog</i> content (see
 *       {@link dev.btstudio.herobrine.event.FromTheFog}) is allowed to tick. Off by default, so its
 *       functions never run alongside our own haunting unless a player asks for them.</li>
 * </ul>
 */
public class HerobrineSettings extends SavedData {
    public static final Codec<HerobrineSettings> CODEC = RecordCodecBuilder.create(instance -> instance
            .group(
                    Codec.BOOL.optionalFieldOf("purist", false).forGetter(HerobrineSettings::isPurist),
                    Codec.BOOL.optionalFieldOf("banished", false).forGetter(HerobrineSettings::isBanished),
                    Codec.BOOL.optionalFieldOf("schizo", true).forGetter(HerobrineSettings::isSchizo),
                    Codec.LONG.optionalFieldOf("chase_until", 0L).forGetter(HerobrineSettings::chaseUntil),
                    Codec.BOOL.optionalFieldOf("herd", true).forGetter(HerobrineSettings::isHerd),
                    Codec.BOOL.optionalFieldOf("miner", true).forGetter(HerobrineSettings::isMiner),
                    Codec.BOOL.optionalFieldOf("arson", true).forGetter(HerobrineSettings::isArson),
                    Codec.BOOL.optionalFieldOf("music", true).forGetter(HerobrineSettings::isMusic),
                    Codec.INT.optionalFieldOf("music_volume", 70).forGetter(HerobrineSettings::musicVolume),
                    Codec.BOOL.optionalFieldOf("from_the_fog", false).forGetter(HerobrineSettings::isFog))
            .apply(instance, HerobrineSettings::new));

    public static final SavedDataType<HerobrineSettings> TYPE = new SavedDataType<>(
            HerobrineMod.id("settings"),
            HerobrineSettings::new,
            CODEC,
            DataFixTypes.SAVED_DATA_WANDERING_TRADER);

    private boolean purist;
    private boolean banished;
    private boolean schizo;
    private long chaseUntil;
    private boolean herd;
    private boolean miner;
    private boolean arson;
    private boolean music;
    private int musicVolume;
    private boolean fog;

    public HerobrineSettings() {
        this(false, false, true, 0L, true, true, true, true, 70, false);
    }

    public HerobrineSettings(boolean purist, boolean banished, boolean schizo, long chaseUntil,
                             boolean herd, boolean miner, boolean arson, boolean music,
                             int musicVolume, boolean fog) {
        this.purist = purist;
        this.banished = banished;
        this.schizo = schizo;
        this.chaseUntil = chaseUntil;
        this.herd = herd;
        this.miner = miner;
        this.arson = arson;
        this.music = music;
        this.musicVolume = musicVolume;
        this.fog = fog;
    }

    public boolean isPurist() {
        return this.purist;
    }

    public void setPurist(boolean purist) {
        if (this.purist != purist) {
            this.purist = purist;
            this.setDirty();
        }
    }

    public boolean isBanished() {
        return this.banished;
    }

    public void setBanished(boolean banished) {
        if (this.banished != banished) {
            this.banished = banished;
            this.setDirty();
        }
    }

    public boolean isSchizo() {
        return this.schizo;
    }

    public void setSchizo(boolean schizo) {
        if (this.schizo != schizo) {
            this.schizo = schizo;
            this.setDirty();
        }
    }

    public boolean isHerd() {
        return this.herd;
    }

    public void setHerd(boolean herd) {
        if (this.herd != herd) {
            this.herd = herd;
            this.setDirty();
        }
    }

    public boolean isMiner() {
        return this.miner;
    }

    public void setMiner(boolean miner) {
        if (this.miner != miner) {
            this.miner = miner;
            this.setDirty();
        }
    }

    public boolean isArson() {
        return this.arson;
    }

    public void setArson(boolean arson) {
        if (this.arson != arson) {
            this.arson = arson;
            this.setDirty();
        }
    }

    public boolean isMusic() {
        return this.music;
    }

    public void setMusic(boolean music) {
        if (this.music != music) {
            this.music = music;
            this.setDirty();
        }
    }

    /** 0-100. Anything else is clamped, so a hand-edited world file cannot blow the speakers. */
    public int musicVolume() {
        return this.musicVolume;
    }

    public void setMusicVolume(int volume) {
        int clamped = Math.max(0, Math.min(100, volume));

        if (this.musicVolume != clamped) {
            this.musicVolume = clamped;
            this.setDirty();
        }
    }

    /**
     * Whether the bundled <i>From The Fog</i> functions may run. Off unless a player turns it on:
     * those functions are their own haunting, and running both at once is not the point.
     */
    public boolean isFog() {
        return this.fog;
    }

    public void setFog(boolean fog) {
        if (this.fog != fog) {
            this.fog = fog;
            this.setDirty();
        }
    }

    public long chaseUntil() {
        return this.chaseUntil;
    }

    public void setChaseUntil(long chaseUntil) {
        if (this.chaseUntil != chaseUntil) {
            this.chaseUntil = chaseUntil;
            this.setDirty();
        }
    }

    /** Static access: the settings of the dimension the player is in. */
    public static HerobrineSettings of(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public static boolean purist(ServerLevel level) {
        return of(level).isPurist();
    }

    /** Returns true when the mode actually changed. */
    public static boolean setPurist(ServerLevel level, boolean purist) {
        HerobrineSettings settings = of(level);

        if (settings.isPurist() == purist) {
            return false;
        }

        settings.setPurist(purist);
        return true;
    }

    public static boolean banished(ServerLevel level) {
        return of(level).isBanished();
    }

    /** Flips the banishment and returns the new state: true means he is gone. */
    public static boolean toggleBanished(ServerLevel level) {
        HerobrineSettings settings = of(level);
        settings.setBanished(!settings.isBanished());
        return settings.isBanished();
    }

    public static boolean setBanished(ServerLevel level, boolean banished) {
        HerobrineSettings settings = of(level);

        if (settings.isBanished() == banished) {
            return false;
        }

        settings.setBanished(banished);
        return true;
    }

    public static boolean schizo(ServerLevel level) {
        return of(level).isSchizo();
    }

    public static boolean toggleSchizo(ServerLevel level) {
        HerobrineSettings settings = of(level);
        settings.setSchizo(!settings.isSchizo());
        return settings.isSchizo();
    }

    public static boolean setSchizo(ServerLevel level, boolean schizo) {
        HerobrineSettings settings = of(level);

        if (settings.isSchizo() == schizo) {
            return false;
        }

        settings.setSchizo(schizo);
        return true;
    }

    public static boolean herd(ServerLevel level) {
        return of(level).isHerd();
    }

    public static boolean toggleHerd(ServerLevel level) {
        HerobrineSettings settings = of(level);
        settings.setHerd(!settings.isHerd());
        return settings.isHerd();
    }

    public static boolean miner(ServerLevel level) {
        return of(level).isMiner();
    }

    public static boolean toggleMiner(ServerLevel level) {
        HerobrineSettings settings = of(level);
        settings.setMiner(!settings.isMiner());
        return settings.isMiner();
    }

    public static boolean music(ServerLevel level) {
        return of(level).isMusic();
    }

    public static boolean toggleMusic(ServerLevel level) {
        HerobrineSettings settings = of(level);
        settings.setMusic(!settings.isMusic());
        return settings.isMusic();
    }

    public static int musicVolume(ServerLevel level) {
        return of(level).musicVolume();
    }

    /** Steps the volume by ten, wrapping from silent back up to full. Returns the new volume. */
    public static int cycleMusicVolume(ServerLevel level) {
        HerobrineSettings settings = of(level);
        int next = settings.musicVolume() + 10;

        if (next > 100) {
            next = 0;
        }

        settings.setMusicVolume(next);
        return settings.musicVolume();
    }

    public static int setMusicVolume(ServerLevel level, int volume) {
        HerobrineSettings settings = of(level);
        settings.setMusicVolume(volume);
        return settings.musicVolume();
    }

    public static boolean fog(ServerLevel level) {
        return of(level).isFog();
    }

    /** Flips the bundled <i>From The Fog</i> switch and returns the new state. */
    public static boolean toggleFog(ServerLevel level) {
        HerobrineSettings settings = of(level);
        settings.setFog(!settings.isFog());
        return settings.isFog();
    }

    public static boolean arson(ServerLevel level) {
        return of(level).isArson();
    }

    public static boolean toggleArson(ServerLevel level) {
        HerobrineSettings settings = of(level);
        settings.setArson(!settings.isArson());
        return settings.isArson();
    }

    public static boolean isChasing(ServerLevel level) {
        return chaseRemainingTicks(level) > 0L;
    }

    public static long chaseRemainingTicks(ServerLevel level) {
        long remaining = of(level).chaseUntil() - level.getLevelData().getGameTime();
        return Math.max(0L, remaining);
    }

    public static void startChase(ServerLevel level, int ticks) {
        of(level).setChaseUntil(level.getLevelData().getGameTime() + ticks);
    }

    public static void endChase(ServerLevel level) {
        of(level).setChaseUntil(0L);
    }
}
