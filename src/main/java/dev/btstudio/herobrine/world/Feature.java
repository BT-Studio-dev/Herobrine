package dev.btstudio.herobrine.world;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.minecraft.world.level.ItemLike;
import net.minecraft.world.item.Items;

/**
 * Everything in the haunting that can be switched off, one constant per switch.
 *
 * <p>Each feature carries the key it is stored under, the group the settings screen files it under,
 * and the value a world gets when it has never been changed. Every default matches the behaviour of
 * the mod before these switches existed, so a world saved by an older version carries on exactly as
 * it was - with the bundled <i>From The Fog</i> content as the one exception, because that one has
 * always been opt-in.
 *
 * <p>Six of them predate this enum and are still stored under their original top-level keys
 * ({@code schizo}, {@code herd}, {@code miner}, {@code arson}, {@code music}, {@code from_the_fog}),
 * which is what {@link #legacyKey()} is for: those keys are read and written by
 * {@link HerobrineSettings} directly, so an old world file keeps working untouched.
 */
public enum Feature {
    // ---- World events: the traces he leaves behind. ----
    /** "Disappearing torches": a light out in the distance that was not there before. */
    TORCHES(Group.WORLD, Items.TORCH),
    /** "Missing leaves": trees stripped bare in the night. */
    LEAVES(Group.WORLD, Items.OAK_LEAVES),
    /** "Mysterious tunnels": a tunnel dug through stone that leads nowhere. */
    TUNNELS(Group.WORLD, Items.STONE_PICKAXE),
    /** "Abnormal structures": a perfect little pyramid of sand. */
    STRUCTURES(Group.WORLD, Items.SANDSTONE),
    /** "Mined trees": the logs are gone, the leaves are still floating there. */
    TREES(Group.WORLD, Items.OAK_LOG),
    /** A single block of cobblestone, standing on open ground. */
    COBBLE(Group.WORLD, Items.COBBLESTONE),
    /** The Nether's own version of the stripped leaves: wart blocks taken off the wall. */
    WARTS(Group.WORLD, Items.NETHER_WART),
    /** A little mound of gravel, dropped on the nearest flat spot. */
    GRAVEL(Group.WORLD, Items.GRAVEL),
    /** The marker: a single upright block where nothing was standing. */
    MARKERS(Group.WORLD, Items.REDSTONE_TORCH),
    /** "Haunted herd": one of the animals nearby stops behaving like an animal. */
    HERD(Group.WORLD, Items.COW_SPAWN_EGG, "herd"),
    /** "Ghost miner": a pickaxe working stone in a tunnel that is not there. */
    MINER(Group.WORLD, Items.IRON_PICKAXE, "miner"),
    /** The face at the window, just outside, looking in. */
    WINDOW(Group.WORLD, Items.GLASS_PANE),
    /** "Ghost doors": a door swings open or shut on its own. */
    DOORS(Group.WORLD, Items.OAK_DOOR),
    /** "Sinister signs": a sign appears with a single line on it. */
    SIGNS(Group.WORLD, Items.OAK_SIGN),
    /** "Chilled candles": candles and campfires go out around you. */
    CANDLES(Group.WORLD, Items.CANDLE),
    /** "Crimson curse": torches turn to redstone, lanterns to soul lanterns. */
    CURSE(Group.WORLD, Items.SOUL_TORCH),
    /** "Dreadful donations": something unpleasant waiting in a chest. */
    DONATION(Group.WORLD, Items.CHEST),
    /** "Burning base": a block of fire, next to you, where nothing was burning. */
    FIRE(Group.WORLD, Items.FLINT_AND_STEEL, "arson"),

    // ---- Apparitions: the ways he shows himself. ----
    /** Standing a long way off, doing nothing but looking at you. */
    WATCH(Group.APPARITIONS, Items.SPYGLASS),
    /** Running across the distance and gone behind the trees. */
    RUNNER(Group.APPARITIONS, Items.LEATHER_BOOTS),
    /** Out on the very edge of the render distance. */
    LURKING(Group.APPARITIONS, Items.ENDER_PEARL),
    /** Around a campfire in the dark, keeping to himself. */
    DWELLING(Group.APPARITIONS, Items.CAMPFIRE),
    /** Directly behind you. */
    SCARE(Group.APPARITIONS, Items.SKELETON_SKULL),
    /** In your face, with the sound. */
    JUMPSCARE(Group.APPARITIONS, Items.CREEPER_HEAD),
    /** Straight up out of the ground in front of you. */
    SPIKE(Group.APPARITIONS, Items.POINTED_DRIPSTONE),

    // ---- Sound ----
    /** The footsteps behind you and the cave noises that answer them. */
    SOUNDS(Group.SOUND, Items.SOUL_LANTERN, "schizo"),
    /** The ten minute haunting track, played when he is genuinely close. */
    MUSIC(Group.SOUND, Items.MUSIC_DISC_13, "music"),
    /**
     * Keep that track going for as long as he is close, starting it again the moment it runs out
     * rather than waiting out the cooldown, and cutting it when he is no longer near. Off, the
     * track is the rare thing it always was: one playthrough, then at least ten minutes of quiet.
     */
    MUSIC_LOOP(Group.SOUND, Items.REPEATER),

    // ---- Presence: the bigger pieces of the mod. ----
    /** "From the fog": the fog closing in as he gets closer. */
    FOG_CLOSING_IN(Group.PRESENCE, Items.COBWEB),
    /** Chapels that appear out on the plains, with holy water in the cauldron. */
    CHURCHES(Group.PRESENCE, Items.BELL),
    /** Steve, the friendly one: a companion you can summon to travel with. */
    STEVE(Group.PRESENCE, Items.PLAYER_HEAD),
    /** The Notch Apple ritual: burn one on a netherrack fire and he is gone. */
    NOTCH_APPLE(Group.PRESENCE, Items.ENCHANTED_GOLDEN_APPLE),
    /** The gold shrine: eight gold blocks, four redstone torches, and lightning. */
    SHRINE(Group.PRESENCE, Items.GOLD_BLOCK),
    /**
     * The bundled <i>From The Fog</i> content. Opt-in, like {@link #ESCALATION}, so it starts off.
     */
    FROM_THE_FOG(Group.PRESENCE, Items.SCULK_CATALYST, "from_the_fog", false),

    /**
     * Escalation: he keeps a grudge. Provoking him - killing animals, lingering in the dark below
     * ground - pushes a dread score up through four tiers, and each one brings him closer and more
     * often. Warding him off pushes it back down. Off, the haunting escalates on time alone, the
     * way it always has.
     */
    ESCALATION(Group.PRESENCE, Items.SCULK_SHRIEKER, null, false),

    ;

    private static final Map<String, Feature> BY_KEY = Stream.of(values())
            .collect(Collectors.toUnmodifiableMap(Feature::key, Function.identity()));

    /**
     * Names these switches went by before they had keys of their own. {@code arson} became
     * {@link #FIRE} and {@code fog} became {@link #FROM_THE_FOG}, and both old names still work on
     * the command line so nothing anyone typed into a command block stops working.
     */
    private static final Map<String, Feature> ALIASES = Map.of(
            "arson", FIRE,
            "fog", FROM_THE_FOG);

    private final Group group;
    private final ItemLike icon;
    private final String legacyKey;
    private final boolean defaultOn;
    private final String key;
    private final String titleKey;
    private final String descriptionKey;

    Feature(Group group, ItemLike icon) {
        this(group, icon, null, true);
    }

    Feature(Group group, ItemLike icon, String legacyKey) {
        this(group, icon, legacyKey, true);
    }

    Feature(Group group, ItemLike icon, String legacyKey, boolean defaultOn) {
        this.group = group;
        this.icon = icon;
        this.legacyKey = legacyKey;
        this.defaultOn = defaultOn;
        this.key = this.name().toLowerCase(Locale.ROOT);
        this.titleKey = "herobrine.feature." + this.key;
        this.descriptionKey = "herobrine.feature." + this.key + ".desc";
    }

    /** The settings screen tab this feature belongs to. */
    public Group group() {
        return this.group;
    }

    /** The item drawn next to the row in the settings screen. */
    public ItemLike icon() {
        return this.icon;
    }

    /** The boolean a world gets when nobody has touched the switch. */
    public boolean defaultOn() {
        return this.defaultOn;
    }

    /** The id used by {@code /herobrine config <key>} and by the settings screen. */
    public String key() {
        return this.key;
    }

    /** The lang key holding the row's title. */
    public String titleKey() {
        return this.titleKey;
    }

    /** The lang key holding the row's one-line description. */
    public String descriptionKey() {
        return this.descriptionKey;
    }

    /**
     * The top-level key this feature was saved under before the feature map existed, or null for
     * the switches that were never stored any other way. Never written twice: a legacy feature is
     * stored under this key and left out of the map.
     */
    public String legacyKey() {
        return this.legacyKey;
    }

    /** The feature with this key, or null when the key is not one of ours. */
    public static Feature byKey(String key) {
        return BY_KEY.get(key);
    }

    /** The feature a command means by this word: its own key first, then an old name for it. */
    public static Feature byCommandId(String id) {
        Feature feature = BY_KEY.get(id);
        return feature != null ? feature : ALIASES.get(id);
    }

    /** Every word {@code /herobrine config} accepts: each key, plus the old names kept for it. */
    public static List<String> commandIds() {
        return Stream.concat(Stream.of(values()).map(Feature::key), ALIASES.keySet().stream())
                .distinct()
                .sorted()
                .toList();
    }

    /** Every feature of one group, in declaration order. */
    public static List<Feature> inGroup(Group group) {
        return Stream.of(values()).filter(feature -> feature.group == group).toList();
    }

    /** Every command and screen id, for Brigadier suggestions. */
    public static List<String> keys() {
        return Stream.of(values()).map(Feature::key).toList();
    }

    /**
     * The page of the settings screen a feature belongs to.
     */
    public enum Group {
        WORLD, APPARITIONS, SOUND, PRESENCE
    }
}
