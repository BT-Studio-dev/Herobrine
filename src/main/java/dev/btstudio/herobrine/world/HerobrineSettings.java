package dev.btstudio.herobrine.world;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

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
 *   <li><b>chaseUntil</b> - game time at which the current <i>chase mode</i> frenzy runs out. While
 *       it is running, apparitions come three times as often.</li>
 *   <li><b>musicVolume</b> - how loud the haunting track plays, 0-100.</li>
 *   <li><b>name</b> - the name he goes by when he pretends to be a player. See
 *       {@link HerobrineName}.</li>
 * </ul>
 *
 * <p>Everything else is a {@link Feature} switch. Six of them are still stored under the top-level
 * keys they had before the feature map existed ({@code schizo}, {@code herd}, {@code miner},
 * {@code arson}, {@code music} and {@code from_the_fog}) so that a world saved by an older version
 * keeps them exactly as they were; the rest live in the {@code features} map, which only ever
 * records the switches that were moved off their default.
 */
public class HerobrineSettings extends SavedData {
    /** The volume a fresh world starts on, and the one a silenced track is put back to. */
    public static final int DEFAULT_MUSIC_VOLUME = 70;

    private static final Codec<Map<String, Boolean>> FEATURES_CODEC =
            Codec.unboundedMap(Codec.STRING, Codec.BOOL);

    public static final Codec<HerobrineSettings> CODEC = RecordCodecBuilder.create(instance -> instance
            .group(
                    Codec.BOOL.optionalFieldOf("purist", false).forGetter(HerobrineSettings::isPurist),
                    Codec.BOOL.optionalFieldOf("banished", false).forGetter(HerobrineSettings::isBanished),
                    Codec.LONG.optionalFieldOf("chase_until", 0L).forGetter(HerobrineSettings::chaseUntil),
                    Codec.INT.optionalFieldOf("music_volume", DEFAULT_MUSIC_VOLUME).forGetter(HerobrineSettings::musicVolume),
                    // The six switches that predate the feature map keep their original keys.
                    Codec.BOOL.optionalFieldOf("schizo", true).forGetter(s -> s.enabled(Feature.SOUNDS)),
                    Codec.BOOL.optionalFieldOf("herd", true).forGetter(s -> s.enabled(Feature.HERD)),
                    Codec.BOOL.optionalFieldOf("miner", true).forGetter(s -> s.enabled(Feature.MINER)),
                    Codec.BOOL.optionalFieldOf("arson", true).forGetter(s -> s.enabled(Feature.FIRE)),
                    Codec.BOOL.optionalFieldOf("music", true).forGetter(s -> s.enabled(Feature.MUSIC)),
                    Codec.BOOL.optionalFieldOf("from_the_fog", false)
                            .forGetter(s -> s.enabled(Feature.FROM_THE_FOG)),
                    FEATURES_CODEC.optionalFieldOf("features", Map.of())
                            .forGetter(HerobrineSettings::savedFeatures),
                    Codec.STRING.optionalFieldOf("name", HerobrineName.DEFAULT)
                            .forGetter(HerobrineSettings::name))
            .apply(instance, HerobrineSettings::new));

    public static final SavedDataType<HerobrineSettings> TYPE = new SavedDataType<>(
            HerobrineMod.id("settings"),
            HerobrineSettings::new,
            CODEC,
            DataFixTypes.SAVED_DATA_WANDERING_TRADER);

    private final Map<Feature, Boolean> features = new EnumMap<>(Feature.class);
    private boolean purist;
    private boolean banished;
    private long chaseUntil;
    private int musicVolume;
    private String name;

    public HerobrineSettings() {
        this(false, false, 0L, DEFAULT_MUSIC_VOLUME, true, true, true, true, true, false,
                Map.of(), HerobrineName.DEFAULT);
    }

    private HerobrineSettings(boolean purist, boolean banished, long chaseUntil, int musicVolume,
                              boolean schizo, boolean herd, boolean miner, boolean arson, boolean music,
                              boolean fog, Map<String, Boolean> stored, String name) {
        for (Feature feature : Feature.values()) {
            this.features.put(feature, feature.defaultOn());
        }

        // The top-level keys win over the defaults, so an old world loads as it was left.
        this.features.put(Feature.SOUNDS, schizo);
        this.features.put(Feature.HERD, herd);
        this.features.put(Feature.MINER, miner);
        this.features.put(Feature.FIRE, arson);
        this.features.put(Feature.MUSIC, music);
        this.features.put(Feature.FROM_THE_FOG, fog);

        for (Map.Entry<String, Boolean> entry : stored.entrySet()) {
            Feature feature = Feature.byKey(entry.getKey());

            // A legacy switch is owned by its own top-level key, so it is never read twice, and a
            // key we do not recognise is simply ignored.
            if (feature != null && feature.legacyKey() == null && entry.getValue() != null) {
                this.features.put(feature, entry.getValue());
            }
        }

        this.purist = purist;
        this.banished = banished;
        this.chaseUntil = chaseUntil;
        this.musicVolume = Math.max(0, Math.min(100, musicVolume));
        this.name = HerobrineName.sanitize(name);
    }

    /**
     * The switches worth writing back: everything moved off its default, except the six that belong
     * to their own top-level keys. A world nobody has touched saves an empty map.
     */
    private Map<String, Boolean> savedFeatures() {
        Map<String, Boolean> saved = new LinkedHashMap<>();

        for (Feature feature : Feature.values()) {
            if (feature.legacyKey() == null && this.enabled(feature) != feature.defaultOn()) {
                saved.put(feature.key(), this.enabled(feature));
            }
        }

        return saved;
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

    /** Whether this one part of the haunting is allowed to happen. */
    public boolean enabled(Feature feature) {
        return Boolean.TRUE.equals(this.features.get(feature));
    }

    /** Turns a single feature on or off. */
    public void setEnabled(Feature feature, boolean enabled) {
        if (this.enabled(feature) != enabled) {
            this.features.put(feature, enabled);
            this.setDirty();
        }
    }

    /** Flips one feature and returns its new state. */
    public boolean toggle(Feature feature) {
        this.setEnabled(feature, !this.enabled(feature));
        return this.enabled(feature);
    }

    /** Every switch at once: {@code /herobrine config all on|off} and the matching screen button. */
    public void setAll(boolean enabled) {
        for (Feature feature : Feature.values()) {
            this.features.put(feature, enabled);
        }

        this.setDirty();
    }

    /** True only when every switch is on, so the "All on" button can show it. */
    public boolean allEnabled() {
        for (Feature feature : Feature.values()) {
            if (!this.enabled(feature)) {
                return false;
            }
        }

        return true;
    }

    /** The name he goes by when he pretends to be a player. */
    public String name() {
        return this.name;
    }

    /** Ignored unless it is a usable name; see {@link HerobrineName#isValid(String)}. */
    public void setName(String name) {
        String clean = HerobrineName.sanitize(name);

        if (!clean.equals(this.name)) {
            this.name = clean;
            this.setDirty();
        }
    }

    public boolean isHerd() {
        return this.enabled(Feature.HERD);
    }

    public void setHerd(boolean herd) {
        this.setEnabled(Feature.HERD, herd);
    }

    public boolean isMiner() {
        return this.enabled(Feature.MINER);
    }

    public void setMiner(boolean miner) {
        this.setEnabled(Feature.MINER, miner);
    }

    public boolean isArson() {
        return this.enabled(Feature.FIRE);
    }

    public void setArson(boolean arson) {
        this.setEnabled(Feature.FIRE, arson);
    }

    public boolean isSchizo() {
        return this.enabled(Feature.SOUNDS);
    }

    public void setSchizo(boolean schizo) {
        this.setEnabled(Feature.SOUNDS, schizo);
    }

    public boolean isMusic() {
        return this.enabled(Feature.MUSIC);
    }

    public void setMusic(boolean music) {
        this.setEnabled(Feature.MUSIC, music);
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
     * Whether the bundled <i>From The Fog</i> content may run. Off unless a player turns it on:
     * those functions are their own haunting, and running both at once is not the point.
     */
    public boolean isFog() {
        return this.enabled(Feature.FROM_THE_FOG);
    }

    public void setFog(boolean fog) {
        this.setEnabled(Feature.FROM_THE_FOG, fog);
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

    /** Whether one part of the haunting is allowed to happen in this dimension. */
    public static boolean enabled(ServerLevel level, Feature feature) {
        return of(level).enabled(feature);
    }

    /** Flips one feature and returns its new state. */
    public static boolean toggle(ServerLevel level, Feature feature) {
        return of(level).toggle(feature);
    }

    public static void setEnabled(ServerLevel level, Feature feature, boolean enabled) {
        of(level).setEnabled(feature, enabled);
    }

    /** Every switch at once, as {@code /herobrine config all on|off} asks for. */
    public static void setAll(ServerLevel level, boolean enabled) {
        of(level).setAll(enabled);
    }

    public static boolean schizo(ServerLevel level) {
        return enabled(level, Feature.SOUNDS);
    }

    public static boolean toggleSchizo(ServerLevel level) {
        return toggle(level, Feature.SOUNDS);
    }

    public static boolean setSchizo(ServerLevel level, boolean schizo) {
        setEnabled(level, Feature.SOUNDS, schizo);
        return schizo;
    }

    public static boolean herd(ServerLevel level) {
        return enabled(level, Feature.HERD);
    }

    public static boolean toggleHerd(ServerLevel level) {
        return toggle(level, Feature.HERD);
    }

    public static boolean miner(ServerLevel level) {
        return enabled(level, Feature.MINER);
    }

    public static boolean toggleMiner(ServerLevel level) {
        return toggle(level, Feature.MINER);
    }

    public static boolean music(ServerLevel level) {
        return enabled(level, Feature.MUSIC);
    }

    public static boolean toggleMusic(ServerLevel level) {
        return toggle(level, Feature.MUSIC);
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
        return enabled(level, Feature.FROM_THE_FOG);
    }

    /** Flips the bundled <i>From The Fog</i> switch and returns the new state. */
    public static boolean toggleFog(ServerLevel level) {
        return toggle(level, Feature.FROM_THE_FOG);
    }

    public static boolean arson(ServerLevel level) {
        return enabled(level, Feature.FIRE);
    }

    public static boolean toggleArson(ServerLevel level) {
        return toggle(level, Feature.FIRE);
    }

    /** The name he goes by in this dimension. */
    public static String name(ServerLevel level) {
        return of(level).name();
    }

    /**
     * True while a chase frenzy is running. It needs the world as well as the settings, because a
     * frenzy is a deadline in game time rather than a flag that is switched off when it expires.
     */
    public boolean isChasing(ServerLevel level) {
        return Math.max(0L, this.chaseUntil - level.getLevelData().getGameTime()) > 0L;
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
