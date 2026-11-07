package dev.btstudio.herobrine.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import dev.btstudio.herobrine.entity.Apparition;
import dev.btstudio.herobrine.entity.HerobrineEntity;
import dev.btstudio.herobrine.entity.ModEntities;
import dev.btstudio.herobrine.sound.ModSounds;
import dev.btstudio.herobrine.world.Feature;
import dev.btstudio.herobrine.world.HerobrineName;
import dev.btstudio.herobrine.world.HerobrineSettings;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Random, low-frequency haunting events rolled once per second for each survival player.
 *
 * <p>This is the full Herobrine repertoire: a torch nobody placed, footsteps behind you, an
 * "Herobrine joined / left the game" line, your torches gone, your torches swapped for redstone and
 * soul light, your candles blown out, a tree with no leaves, a fresh tunnel, a sign with one line on
 * it, something unpleasant in your chest, a door that opens itself, a fire that starts itself, an
 * odd sand pyramid - and the mob himself, watching from further away the longer you have survived.
 *
 * <p>The haunting escalates: every two in-game days the roll gets more frequent, he appears closer,
 * and from stage 3 he sometimes arrives hunting instead of watching.
 *
 * <p>The apparitions themselves come from <i>Him - Herobrine</i>: a distant watcher, a figure
 * already standing behind you, a scream in your face, or a silent thing that lunges. One apparition
 * in twelve starts a <b>chase mode</b> frenzy where all of this happens three times as fast. A
 * <b>Notch Apple</b> burned on netherrack ({@link NotchAppleRitual}) sets the per-dimension banished
 * flag, and then nothing in here runs at all.
 *
 * <p>Iron works the other way: the iron-stick gear ({@link IronWard}) lowers the chance of an
 * apparition for whoever is carrying it, and it makes him retreat when it hits him.
 */
public final class HauntingEvents {
    /** Chance per player per second at stage 0 (1/180 is roughly once every 3 min). */
    private static final int ROLL_ONE_IN = 180;

    /** Ticks per escalation stage: two in-game days. */
    private static final long TICKS_PER_STAGE = 48_000L;

    private static final int MAX_STAGE = 3;

    /** Do not stack stalkers: if one is already watching, skip this roll. */
    private static final double STALKER_STACK_RADIUS = 48.0;

    /**
     * The fake "Herobrine joined/left the game" and "Herobrine: stop" chat messages. They use
     * vanilla chat formatting on purpose, so they are indistinguishable from a real player. Set
     * this to {@code false} if that illusion is confusing on your server - those rolls then fall
     * through to the next event.
     */
    private static final boolean FAKE_CHAT_MESSAGES = true;

    /** Stops a single roll from shredding a whole forest. */
    /** The base of a sand pyramid, in blocks from the centre. A radius of 3 steps 7-5-3-1. */
    private static final int PYRAMID_RADIUS = 3;
    /** How far out the bare grove reaches, in blocks. */
    private static final int GROVE_RADIUS = 6;
    private static final int GROVE_HEIGHT_ABOVE = 10;
    private static final int GROVE_DEPTH_BELOW = 2;
    /** ...or from felling every tree and fungus nearby. */
    private static final int MAX_TREE_LOGS_REMOVED = 24;
    private static final int MAX_WARTS_STRIPPED = 32;

    /** True Herobrine only ever spawns above this height, so you can actually spot him. */
    private static final int WATCHER_MIN_Y = 60;

    /** Apparitions come three times as often while a chase is running. */
    private static final int CHASE_INTERVAL_DIVISOR = 3;
    /** A chase frenzy lasts three minutes. */
    private static final int CHASE_TICKS = 3600;
    /** Chance (one in N) that an apparition kicks off a chase. */
    private static final int CHASE_ONE_IN = 12;

    /** The haunting track plays when he is this close... */
    private static final double MUSIC_RANGE = 28.0;
    /** ...at most once every five minutes... */
    /** Ten minutes, plus half a minute of quiet: the track is ten minutes long, so a shorter
     * gap would start a second one on top of the first. */
    private static final int MUSIC_COOLDOWN_TICKS = 12_600;
    /** ...and it is only ever considered every ten seconds. */
    private static final int MUSIC_CHECK_TICKS = 200;
    /** Played at this fraction of the configured volume: music, not a siren. */
    private static final float MUSIC_BASE_VOLUME = 0.6F;

    /** When each player last heard the track, in game ticks. */
    private static final Map<UUID, Long> LAST_MUSIC = new HashMap<>();

    /** How many blocks the dug tunnel runs into the hillside. */
    private static final int TUNNEL_LENGTH = 24;
    /** A corridor shorter than this is not worth the hole in the surface, so nothing is dug. */
    private static final int MIN_TUNNEL_LENGTH = 6;

    /**
     * Event weights. The roll is 0-99 and each event owns everything below its threshold, so the
     * bands must stay in ascending order and end at (or below) 100.
     */
    private static final int TORCH_ROLL = 9;
    private static final int FOOTSTEP_ROLL = 15;
    private static final int SOUND_ROLL = 21;
    private static final int SNUFF_ROLL = 27;
    private static final int CURSE_ROLL = 33;
    private static final int LEAVE_ROLL = 38;
    private static final int TUNNEL_ROLL = 43;
    private static final int SIGN_ROLL = 49;
    private static final int DONATION_ROLL = 54;
    private static final int DOOR_ROLL = 59;
    private static final int JOIN_ROLL = 62;
    private static final int MESSAGE_ROLL = 66;
    private static final int ARSON_ROLL = 69;
    private static final int STRUCTURE_ROLL = 73;
    private static final int COBBLE_ROLL = 76;
    private static final int TREE_ROLL = 79;
    private static final int WART_ROLL = 81;
    private static final int CHURCH_ROLL = 84;
    private static final int GRAVEL_ROLL = 86;
    private static final int MARKER_ROLL = 88;
    private static final int HERD_ROLL = 90;
    private static final int MINER_ROLL = 92;
    private static final int WINDOW_ROLL = 94;

    private HauntingEvents() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 20 != 0) {
                return;
            }

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (player.isSpectator() || player.isCreative() || !player.isAlive()) {
                    continue;
                }

                ServerLevel level = player.level();

                // Banished to the void by the Notch Apple ritual: nothing happens at all.
                if (HerobrineSettings.banished(level)) {
                    continue;
                }

                RandomSource random = player.getRandom();

                if (random.nextInt(intervalFor(level, stageOf(player))) == 0) {
                    runRandomEvent(server, player, random);
                }

                maybePlayMusic(level, player);
            }
        });
    }

    /**
     * The haunting track: ten minutes of drone, wind, distant bells and something moving under
     * the floor, played when he is genuinely close. It is a setting, it has a volume, and the
     * gap between plays is longer than the track, so it never interrupts itself.
     */
    private static void maybePlayMusic(ServerLevel level, ServerPlayer player) {
        long gameTime = level.getLevelData().getGameTime();

        if (gameTime % MUSIC_CHECK_TICKS != 0) {
            return;
        }

        HerobrineSettings settings = HerobrineSettings.of(level);

        if (!settings.isMusic() || settings.musicVolume() <= 0) {
            return;
        }

        Long last = LAST_MUSIC.get(player.getUUID());

        if (last != null && gameTime - last < MUSIC_COOLDOWN_TICKS) {
            return;
        }

        boolean nearby = !level.getEntitiesOfClass(HerobrineEntity.class,
                player.getBoundingBox().inflate(MUSIC_RANGE), HerobrineEntity::isAlive).isEmpty();

        if (!nearby) {
            return;
        }

        LAST_MUSIC.put(player.getUUID(), gameTime);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.MUSIC_HAUNT,
                SoundSource.MUSIC, MUSIC_BASE_VOLUME * settings.musicVolume() / 100.0F, 1.0F);
    }

    /**
     * The volume the haunting track would play at right now, or 0 when it would not play at all -
     * the switch is off, the volume is zero, or he is not within {@link #MUSIC_RANGE}. Nothing is
     * played and no cooldown is touched: this is what {@code /herobrine config musictest} reports.
     */
    public static float musicTestVolume(ServerLevel level) {
        HerobrineSettings settings = HerobrineSettings.of(level);

        if (!settings.isMusic() || settings.musicVolume() <= 0) {
            return 0.0F;
        }

        return MUSIC_BASE_VOLUME * settings.musicVolume() / 100.0F;
    }

    /** 0-3, one step every two in-game days, so a long-lived world gets noticeably worse. */
    public static int stageOf(ServerPlayer player) {
        long days = player.level().getLevelData().getGameTime() / TICKS_PER_STAGE;
        return (int) Math.min(MAX_STAGE, Math.max(0L, days));
    }

    /** Seconds between rolls, before chase mode is taken into account. */
    public static int oneInFor(int stage) {
        return Math.max(60, ROLL_ONE_IN - stage * 40);
    }

    /** The effective roll interval: three times as fast while a chase frenzy is running. */
    public static int intervalFor(ServerLevel level, int stage) {
        int base = oneInFor(stage);

        if (!HerobrineSettings.isChasing(level)) {
            return base;
        }

        return Math.max(20, base / CHASE_INTERVAL_DIVISOR);
    }

    public static void runRandomEvent(MinecraftServer server, ServerPlayer player, RandomSource random) {
        ServerLevel level = player.level();
        HerobrineSettings settings = HerobrineSettings.of(level);

        if (settings.isBanished()) {
            return;
        }

        if (settings.isPurist()) {
            puristEvent(level, player, random);
            return;
        }

        boolean sounds = settings.enabled(Feature.SOUNDS);
        boolean nether = level.dimension().equals(Level.NETHER);
        int roll = random.nextInt(100);

        // Every branch below is one roll of the dice, and every one of them starts by asking
        // whether that particular thing is switched on. A switch that is off is not a failure:
        // whatever he would have done simply does not happen, and the next roll comes on its own.
        if (roll < TORCH_ROLL) {
            if (settings.enabled(Feature.TORCHES)) {
                placeTorch(level, player, random);
            }
        } else if (roll < FOOTSTEP_ROLL) {
            // "Fearful footsteps". With the sounds off he used to leave a torch instead, and he
            // still does - but only while torches are switched on too.
            if (sounds) {
                footsteps(level, player, random);
            } else if (settings.enabled(Feature.TORCHES)) {
                placeTorch(level, player, random);
            }
        } else if (roll < SOUND_ROLL) {
            if (sounds) {
                creepySound(level, player, random);
            } else if (settings.enabled(Feature.DOORS)) {
                Sabotage.ghostDoor(level, player, random);
            }
        } else if (roll < SNUFF_ROLL) {
            if (settings.enabled(Feature.CANDLES)) {
                Sabotage.snuffLights(level, player, random);
            }
        } else if (roll < CURSE_ROLL) {
            // Two halves of one branch: the crimson curse, or every candle and campfire going out.
            // Whichever he reaches for first, he falls back on the other when it is switched off.
            boolean curseFirst = random.nextBoolean();

            if (curseFirst) {
                if (settings.enabled(Feature.CURSE)) {
                    Sabotage.curseLights(level, player, random);
                } else if (settings.enabled(Feature.CANDLES)) {
                    Sabotage.douseFlames(level, player, random);
                }
            } else {
                if (settings.enabled(Feature.CANDLES)) {
                    Sabotage.douseFlames(level, player, random);
                } else if (settings.enabled(Feature.CURSE)) {
                    Sabotage.curseLights(level, player, random);
                }
            }
        } else if (roll < LEAVE_ROLL) {
            // In the Nether there are no leaves to strip, so he takes the wart blocks instead.
            if (nether) {
                if (settings.enabled(Feature.WARTS)) {
                    stripNetherWarts(level, player);
                }
            } else if (settings.enabled(Feature.LEAVES)) {
                stripLeaves(level, player, random);
            }
        } else if (roll < TUNNEL_ROLL) {
            if (settings.enabled(Feature.TUNNELS)) {
                digTunnel(level, player, random);
            }
        } else if (roll < SIGN_ROLL) {
            if (settings.enabled(Feature.SIGNS)) {
                Sabotage.creepySign(level, player, random, HerobrineSettings.name(level));
            }
        } else if (roll < DONATION_ROLL) {
            if (settings.enabled(Feature.DONATION)) {
                Sabotage.donation(level, player, random);
            }
        } else if (roll < DOOR_ROLL) {
            if (settings.enabled(Feature.DOORS)) {
                Sabotage.ghostDoor(level, player, random);
            }
        } else if (FAKE_CHAT_MESSAGES && roll < JOIN_ROLL) {
            fakeJoinLeave(server, random, HerobrineSettings.name(level));
        } else if (FAKE_CHAT_MESSAGES && roll < MESSAGE_ROLL) {
            creepyMessage(server, HerobrineSettings.name(level));
        } else if (roll < ARSON_ROLL) {
            if (settings.enabled(Feature.FIRE)) {
                Sabotage.arson(level, player, random);
            }
        } else if (roll < STRUCTURE_ROLL) {
            // "Perfect sand pyramids in the oceans or rivers" first, a dry one if there is no water.
            if (settings.enabled(Feature.STRUCTURES) && !buildPyramid(level, player, random, true)) {
                buildPyramid(level, player, random, false);
            }
        } else if (roll < COBBLE_ROLL) {
            if (settings.enabled(Feature.COBBLE)) {
                placeCobblestone(level, player, random);
            }
        } else if (roll < TREE_ROLL) {
            // "Mined trees": the logs are gone, the leaves are still floating there.
            if (nether) {
                if (settings.enabled(Feature.WARTS)) {
                    stripNetherWarts(level, player);
                }
            } else if (settings.enabled(Feature.TREES)) {
                stripTreeLogs(level, player, random);
            }
        } else if (roll < WART_ROLL) {
            if (nether) {
                if (settings.enabled(Feature.WARTS)) {
                    stripNetherWarts(level, player);
                }
            } else if (settings.enabled(Feature.COBBLE)) {
                placeCobblestone(level, player, random);
            }
        } else if (roll < CHURCH_ROLL) {
            // "Things spawn that you don't remember building": a chapel out in the plains.
            if (settings.enabled(Feature.CHURCHES)) {
                Churches.spawnNearPlayer(level, player, random);
            }
        } else if (roll < GRAVEL_ROLL) {
            if (settings.enabled(Feature.GRAVEL)) {
                placeGravelMound(level, player, random);
            }
        } else if (roll < MARKER_ROLL) {
            if (settings.enabled(Feature.MARKERS)) {
                placeMarker(level, player, random);
            }
        } else if (roll < HERD_ROLL) {
            // "Haunted herd": one of the animals nearby stops behaving like an animal. No animal
            // to possess, and he leaves a block of cobblestone instead - if he is allowed to.
            if (!(settings.enabled(Feature.HERD) && herdEvent(level, player, random))
                    && settings.enabled(Feature.COBBLE)) {
                placeCobblestone(level, player, random);
            }
        } else if (roll < MINER_ROLL) {
            // "Ghost miner": a pickaxe in stone that nobody is swinging.
            if (settings.enabled(Feature.MINER)) {
                ghostMiner(level, player, random);
            } else if (sounds) {
                creepySound(level, player, random);
            }
        } else if (roll < WINDOW_ROLL) {
            if (settings.enabled(Feature.WINDOW)) {
                windowSighting(level, player, random);
            }
        } else {
            spawnApparition(level, player, random, stageOf(player));
        }
    }

    /**
     * One of the four <i>Him</i> apparitions, and sometimes the start of a chase frenzy.
     *
     * <p>Early on he mostly just watches. By stage 2 the four are equally likely, so the odds of him
     * standing behind you or in your face go up the longer the world has been running.
     *
     * <p>Iron is the one thing that lowers those odds again: a player carrying or wearing the
     * iron-stick gear ({@link IronWard}) turns a share of the sightings away, up to six in ten.
     */
    public static HerobrineEntity spawnApparition(ServerLevel level, ServerPlayer player, RandomSource random,
                                                  int stage) {
        if (someoneAlreadyWatching(level, player)) {
            return null;
        }

        // He keeps his distance from iron: the sighting is dropped, and nothing is built or moved
        // to make up for it.
        if (IronWard.turnsAway(player, random)) {
            return null;
        }

        Apparition apparition = pickApparition(level, random, stage);

        if (apparition == null) {
            return null;
        }

        // Automatic rolls never tell the player when they find no ground: a sighting that cannot
        // be placed simply is not placed, and the next roll happens on its own. Only the operator
        // command reports a failure.
        HerobrineEntity herobrine = Apparitions.spawn(level, player, apparition, random);

        if (herobrine != null) {
            maybeStartChase(level, random);
        }

        return herobrine;
    }

    /**
     * Picks one of the sightings that are switched on, weighted by {@link Apparition#weight(int)}.
     * With all seven on the weights add up to 100, so this rolls exactly as it always did; with some
     * of them off the others take their share. Null when every sighting is switched off, in which
     * case no apparition is placed at all.
     */
    public static Apparition pickApparition(ServerLevel level, RandomSource random, int stage) {
        int total = 0;

        for (Apparition apparition : Apparition.values()) {
            if (HerobrineSettings.enabled(level, apparition.feature())) {
                total += apparition.weight(stage);
            }
        }

        if (total <= 0) {
            return null;
        }

        int roll = random.nextInt(total);

        for (Apparition apparition : Apparition.values()) {
            if (!HerobrineSettings.enabled(level, apparition.feature())) {
                continue;
            }

            roll -= apparition.weight(stage);

            if (roll < 0) {
                return apparition;
            }
        }

        return null;
    }

    /** Chase mode: a few minutes where he cannot leave you alone. */
    private static void maybeStartChase(ServerLevel level, RandomSource random) {
        if (HerobrineSettings.isChasing(level) || random.nextInt(CHASE_ONE_IN) != 0) {
            return;
        }

        HerobrineSettings.startChase(level, CHASE_TICKS);

        MinecraftServer server = level.getServer();

        if (server != null) {
            server.getPlayerList().broadcastSystemMessage(
                    Component.translatable("herobrine.message.chase").withStyle(ChatFormatting.DARK_RED),
                    false);
        }
    }

    /** True when an apparition of his is already standing around this player. */
    private static boolean someoneAlreadyWatching(ServerLevel level, ServerPlayer player) {
        return !level.getEntitiesOfClass(HerobrineEntity.class,
                player.getBoundingBox().inflate(STALKER_STACK_RADIUS),
                herobrine -> !herobrine.isHostile()).isEmpty();
    }

    /**
     * Purist mode, straight out of <i>True Herobrine</i>: nothing is built, broken, swapped, burned
     * or said. He is only ever seen.
     */
    private static void puristEvent(ServerLevel level, ServerPlayer player, RandomSource random) {
        int roll = random.nextInt(100);
        boolean ambience = HerobrineSettings.schizo(level);

        if (roll < 60 || !ambience) {
            spawnWatcher(level, player, random);
        } else if (roll < 80) {
            footsteps(level, player, random);
        } else {
            creepySound(level, player, random);
        }
    }

    /**
     * A watcher sighting: Overworld only, above Y 60, never closer than 25 blocks, and he stays put
     * staring at you for two minutes unless you walk towards him.
     */
    private static void spawnWatcher(ServerLevel level, ServerPlayer player, RandomSource random) {
        if (!level.dimension().equals(Level.OVERWORLD)) {
            return;
        }

        if (someoneAlreadyWatching(level, player)) {
            return;
        }

        for (int attempt = 0; attempt < 32; attempt++) {
            BlockPos pos = randomSurfacePos(level, player, random, 26, 40);

            if (pos.getY() < WATCHER_MIN_Y) {
                continue;
            }

            HerobrineEntity herobrine = ModEntities.HEROBRINE.create(level, EntitySpawnReason.EVENT);

            if (herobrine == null) {
                return;
            }

            herobrine.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            herobrine.setWatcher(true);
            level.addFreshEntity(herobrine);
            return;
        }
    }

    /** Places a single torch somewhere in the distance, like someone was here. */
    private static void placeTorch(ServerLevel level, ServerPlayer player, RandomSource random) {
        BlockPos pos = randomSurfacePos(level, player, random, 10, 24);
        BlockPos below = pos.below();

        if (level.getBlockState(pos).isAir()
                && level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
            level.setBlock(pos, Blocks.TORCH.defaultBlockState(), 3);
        }
    }

    /** "Fearful footsteps": steps behind you, close enough to turn around for. */
    private static void footsteps(ServerLevel level, ServerPlayer player, RandomSource random) {
        Vec3 behind = behindPlayer(player, 3.5);

        for (int step = 0; step < 3; step++) {
            level.playSound(null, behind.x, player.getY(), behind.z, ModSounds.FOOTSTEP,
                    SoundSource.AMBIENT, 0.9F, 0.8F + step * 0.1F);
        }
    }

    /** A cave sound from a spot behind the player. */
    private static void creepySound(ServerLevel level, ServerPlayer player, RandomSource random) {
        // The mod's own sound set: whispers, wind, a drone, glitches, a pickaxe and a heartbeat.
        SoundEvent sound = switch (random.nextInt(6)) {
            case 0 -> ModSounds.WHISPER;
            case 1 -> ModSounds.FOG;
            case 2 -> ModSounds.AMBIENT;
            case 3 -> ModSounds.GLITCH;
            case 4 -> ModSounds.MINING;
            default -> ModSounds.HEARTBEAT;
        };

        Vec3 behind = behindPlayer(player, 6.0);

        // One time in four the mod's own noise gives way to the sound players already dread:
        // vanilla's cave ambience. When that happens he is genuinely out there watching, so the
        // sound is never a lie.
        if (random.nextInt(4) == 0) {
            level.playSound(null, behind.x, player.getY(), behind.z,
                    SoundEvents.AMBIENT_CAVE, SoundSource.AMBIENT, 1.0F, 1.0F);
            Apparitions.spawn(level, player, Apparition.LURKING, random);
            return;
        }

        level.playSound(null, behind.x, player.getY(), behind.z, sound, SoundSource.AMBIENT, 1.0F, 0.8F);
    }

    /**
     * "Ghost miner": a pickaxe working stone in a tunnel that is not there. Only the sound exists.
     */
    public static void ghostMiner(ServerLevel level, ServerPlayer player, RandomSource random) {
        Vec3 spot = behindPlayer(player, 8.0 + random.nextInt(12));
        int strikes = 3 + random.nextInt(3);

        for (int strike = 0; strike < strikes; strike++) {
            level.playSound(null, spot.x, player.getY() - 1.0, spot.z, ModSounds.MINING,
                    SoundSource.AMBIENT, 1.0F, 0.85F + random.nextFloat() * 0.3F);
        }
    }

    /**
     * "Haunted herd": a nearby animal is possessed - glowing, named, and it will not despawn. It
     * keeps doing animal things; that is the point.
     */
    public static boolean herdEvent(ServerLevel level, ServerPlayer player, RandomSource random) {
        List<Animal> animals = level.getEntitiesOfClass(Animal.class, player.getBoundingBox().inflate(20.0),
                animal -> animal.isAlive() && !animal.isBaby());

        if (animals.isEmpty()) {
            return false;
        }

        Animal animal = animals.get(random.nextInt(animals.size()));
        animal.setCustomName(Component.translatable("herobrine.possessed", animal.getName()));
        animal.setCustomNameVisible(true);
        animal.setGlowingTag(true);
        animal.setPersistenceRequired();
        level.playSound(null, animal.getX(), animal.getY(), animal.getZ(), ModSounds.CAUTION,
                SoundSource.AMBIENT, 0.8F, 0.7F);
        return true;
    }

    /** "Window sighting": he stands on the other side of a pane of glass and watches you through it. */
    public static boolean windowSighting(ServerLevel level, ServerPlayer player, RandomSource random) {
        BlockPos origin = player.blockPosition();

        for (int attempt = 0; attempt < 96; attempt++) {
            BlockPos glass = origin.offset(random.nextInt(17) - 8, random.nextInt(7) - 3, random.nextInt(17) - 8);
            BlockState state = level.getBlockState(glass);

            if (!state.is(Blocks.GLASS) && !state.is(Blocks.GLASS_PANE)) {
                continue;
            }

            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos outside = glass.relative(direction);

                if (outside.distSqr(origin) < 4.0) {
                    continue;
                }

                if (Apparitions.spawnAt(level, outside, Apparition.WATCH, player) != null) {
                    lightTheWindow(level, outside, glass);
                    level.playSound(null, outside.getX() + 0.5, outside.getY() + 0.5, outside.getZ() + 0.5,
                            ModSounds.WHISPER, SoundSource.AMBIENT, 1.0F, 0.9F);
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Sets a soul torch beside him on the far side of the pane. Without it a night-time sighting is
     * a silhouette you cannot actually make out, which defeats the point of him standing there.
     * Only air over solid ground is used, and the pane itself is never replaced.
     */
    private static void lightTheWindow(ServerLevel level, BlockPos outside, BlockPos glass) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos pos = outside.relative(direction);

            if (pos.equals(glass) || !level.getBlockState(pos).isAir()
                    || level.getBlockState(pos.below()).isAir()) {
                continue;
            }

            level.setBlock(pos, Blocks.SOUL_TORCH.defaultBlockState(), 3);
            return;
        }
    }

    /** "Gravel mounds": a small pile of gravel where nothing was a second ago. */
    public static boolean placeGravelMound(ServerLevel level, ServerPlayer player, RandomSource random) {
        BlockPos centre = randomSurfacePos(level, player, random, 8, 24);
        int placed = 0;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos pos = centre.offset(dx, 0, dz);

                if (isFreeForSand(level, pos) && random.nextBoolean()) {
                    level.setBlock(pos, Blocks.GRAVEL.defaultBlockState(), 3);
                    placed++;
                }
            }
        }

        if (placed > 0 && isFreeForSand(level, centre.above())) {
            level.setBlock(centre.above(), Blocks.GRAVEL.defaultBlockState(), 3);
        }

        return placed > 0;
    }

    /** "Markers made of redstone and gold": a gold block with a redstone torch on top. */
    public static boolean placeMarker(ServerLevel level, ServerPlayer player, RandomSource random) {
        for (int attempt = 0; attempt < 12; attempt++) {
            BlockPos pos = randomSurfacePos(level, player, random, 8, 26);

            if (isFreeForSand(level, pos) && isFreeForSand(level, pos.above())) {
                level.setBlock(pos, Blocks.GOLD_BLOCK.defaultBlockState(), 3);
                level.setBlock(pos.above(), Blocks.REDSTONE_TORCH.defaultBlockState(), 3);
                return true;
            }
        }

        return false;
    }

    private static Vec3 behindPlayer(ServerPlayer player, double distance) {
        double behind = -Math.toRadians(player.getYRot());

        return new Vec3(player.getX() + Math.sin(behind) * distance, player.getY(),
                player.getZ() + Math.cos(behind) * distance);
    }

    /**
     * Strips every leaf in a grove-sized sphere and leaves the trunks standing - the "who stripped
     * my trees?" sign that the original story is built on. The whole stand goes at once rather than
     * a fixed handful of blocks, so what is left is a ring of bare trunks with open sky above them.
     */
    public static void stripLeaves(ServerLevel level, ServerPlayer player, RandomSource random) {
        BlockPos centre = randomSurfacePos(level, player, random, 8, 24);
        int radiusSquared = GROVE_RADIUS * GROVE_RADIUS;

        for (int dx = -GROVE_RADIUS; dx <= GROVE_RADIUS; dx++) {
            for (int dy = -GROVE_DEPTH_BELOW; dy <= GROVE_HEIGHT_ABOVE; dy++) {
                for (int dz = -GROVE_RADIUS; dz <= GROVE_RADIUS; dz++) {
                    if (dx * dx + dy * dy + dz * dz > radiusSquared) {
                        continue;
                    }

                    BlockPos pos = centre.offset(dx, dy, dz);

                    if (level.getBlockState(pos).is(BlockTags.LEAVES)) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    /**
     * Digs a short, crudely carved **2x2** tunnel (a shaft down and a two-by-two corridor running off
     * into the hillside) - Herobrine's "long 2x2 tunnel pathways in caves", and the same thing he
     * does in the Nether. Nothing is carved unless the whole corridor lands in natural stone, dirt or
     * netherrack, so player builds are never touched.
     */
    public static void digTunnel(ServerLevel level, ServerPlayer player, RandomSource random) {
        BlockPos mouth = randomSurfacePos(level, player, random, 12, 26);

        if (!level.getFluidState(mouth).isEmpty() || !isNaturalGround(level.getBlockState(mouth.below()))) {
            return;
        }

        Direction direction = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        Direction side = direction.getClockWise();
        BlockPos floor = mouth.below(3);
        List<BlockPos> corridor = new ArrayList<>();

        corridor.add(mouth.below(1));
        corridor.add(mouth.below(1).relative(side));
        corridor.add(mouth.below(2));
        corridor.add(mouth.below(2).relative(side));
        corridor.add(floor);
        corridor.add(floor.relative(side));

        for (BlockPos pos : corridor) {
            if (!isDiggable(level, pos)) {
                return;
            }
        }

        // The corridor runs on until the hillside stops being natural. Carving as far as the
        // ground allows - rather than abandoning the whole tunnel the moment one block is wrong -
        // is what lets it run long: a 24 block corridor almost always meets a cave or an ore
        // seam part of the way, and a tunnel that stops there is still a tunnel.
        int carved = 0;

        for (int step = 1; step <= TUNNEL_LENGTH; step++) {
            BlockPos column = floor.relative(direction, step);
            List<BlockPos> ring = List.of(column, column.above(), column.relative(side),
                    column.relative(side).above());
            boolean clear = true;

            for (BlockPos pos : ring) {
                clear &= isDiggable(level, pos);
            }

            if (!clear) {
                break;
            }

            corridor.addAll(ring);
            carved = step;
        }

        if (carved < MIN_TUNNEL_LENGTH) {
            return;
        }

        for (BlockPos pos : corridor) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    /** Air, or ground that was there before the player was: stone, dirt, netherrack and friends. */
    private static boolean isDiggable(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);

        return state.isAir() || isNaturalGround(state);
    }

    /**
     * Leaves a small sand pyramid behind - Herobrine's "abnormal structures", and Inferno's
     * signature **perfect pyramids out in the water**. Only air and water are ever replaced, so a
     * pyramid can never eat a player's blocks.
     *
     * @param water when true only a water surface is used, so the caller can fall back to a dry one
     * @return true when a pyramid was actually built
     */
    public static boolean buildPyramid(ServerLevel level, ServerPlayer player, RandomSource random, boolean water) {
        for (int attempt = 0; attempt < 10; attempt++) {
            BlockPos centre = randomSurfacePos(level, player, random, 8, 26);
            BlockPos base = water ? centre.below() : centre;

            if (water ? isWater(level, base) : isFreeForSand(level, base)) {
                pyramidAt(level, base);
                return true;
            }
        }

        return false;
    }

    private static void pyramidAt(ServerLevel level, BlockPos base) {
        for (int layer = PYRAMID_RADIUS; layer >= 1; layer--) {
            int y = PYRAMID_RADIUS - layer;

            for (int dx = -layer; dx <= layer; dx++) {
                for (int dz = -layer; dz <= layer; dz++) {
                    BlockPos pos = base.offset(dx, y, dz);

                    if (isReplaceableForSand(level, pos)) {
                        level.setBlock(pos, Blocks.SAND.defaultBlockState(), 3);
                    }
                }
            }
        }

        BlockPos cap = base.above(PYRAMID_RADIUS);

        if (isReplaceableForSand(level, cap)) {
            level.setBlock(cap, Blocks.SAND.defaultBlockState(), 3);
        }
    }

    /** "Mined trees": the trunk disappears and the leaves stay exactly where they were. */
    public static int stripTreeLogs(ServerLevel level, ServerPlayer player, RandomSource random) {
        BlockPos centre = randomSurfacePos(level, player, random, 8, 26);
        int removed = 0;

        for (int dx = -6; dx <= 6; dx++) {
            for (int dy = -2; dy <= 14; dy++) {
                for (int dz = -6; dz <= 6; dz++) {
                    BlockPos pos = centre.offset(dx, dy, dz);

                    if (!level.getBlockState(pos).is(BlockTags.LOGS)) {
                        continue;
                    }

                    // Only a real tree: it has to be surrounded by leaves, or it is a player's build.
                    if (countLeavesAround(level, pos) < 8) {
                        continue;
                    }

                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);

                    if (++removed >= MAX_TREE_LOGS_REMOVED) {
                        return removed;
                    }
                }
            }
        }

        return removed;
    }

    private static int countLeavesAround(ServerLevel level, BlockPos centre) {
        int leaves = 0;

        for (int dx = -4; dx <= 4; dx++) {
            for (int dy = -2; dy <= 4; dy++) {
                for (int dz = -4; dz <= 4; dz++) {
                    if (level.getBlockState(centre.offset(dx, dy, dz)).is(BlockTags.LEAVES)) {
                        leaves++;
                    }
                }
            }
        }

        return leaves;
    }

    /** In the Nether he takes the wart blocks off the huge fungi and leaves the stalk standing. */
    public static int stripNetherWarts(ServerLevel level, ServerPlayer player) {
        BlockPos centre = player.blockPosition();
        int removed = 0;

        for (int dx = -12; dx <= 12; dx++) {
            for (int dy = -8; dy <= 12; dy++) {
                for (int dz = -12; dz <= 12; dz++) {
                    BlockPos pos = centre.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(pos);

                    if (!state.is(Blocks.NETHER_WART_BLOCK) && !state.is(Blocks.WARPED_WART_BLOCK)) {
                        continue;
                    }

                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);

                    if (++removed >= MAX_WARTS_STRIPPED) {
                        return removed;
                    }
                }
            }
        }

        return removed;
    }

    /** "Randomly placed cobblestone": one block of cobble on open ground, as if someone was here. */
    public static boolean placeCobblestone(ServerLevel level, ServerPlayer player, RandomSource random) {
        for (int attempt = 0; attempt < 24; attempt++) {
            BlockPos pos = randomSurfacePos(level, player, random, 6, 22);

            if (isFreeForSand(level, pos)) {
                level.setBlock(pos, Blocks.COBBLESTONE.defaultBlockState(), 3);
                return true;
            }
        }

        return false;
    }

    /**
     * The oldest trick in the book: a line in chat saying he joined, or left, when he did neither.
     * He goes by whatever name the world has given him.
     */
    private static void fakeJoinLeave(MinecraftServer server, RandomSource random, String name) {
        String key = random.nextBoolean() ? "multiplayer.player.joined" : "multiplayer.player.left";

        server.getPlayerList().broadcastSystemMessage(
                Component.translatable(key, HerobrineName.component(name))
                        .withStyle(ChatFormatting.YELLOW),
                false);
    }

    /** He answers in chat with the one word the original creepypasta is famous for. */
    private static void creepyMessage(MinecraftServer server, String name) {
        server.getPlayerList().broadcastSystemMessage(
                Component.translatable("chat.type.text",
                        HerobrineName.component(name).withStyle(ChatFormatting.DARK_RED),
                        Component.translatable("herobrine.message.stop")),
                false);
    }

    /** Stone, deepslate, dirt and the loose surface materials a wild tunnel may pass through. */
    private static boolean isNaturalGround(BlockState state) {
        return state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(BlockTags.DIRT)
                || state.is(Blocks.GRAVEL)
                || state.is(Blocks.SAND)
                || state.is(Blocks.SANDSTONE)
                // The same tunnels, through the Nether's own ground.
                || state.is(Blocks.NETHERRACK)
                || state.is(Blocks.BASALT)
                || state.is(Blocks.BLACKSTONE)
                || state.is(Blocks.SOUL_SAND)
                || state.is(Blocks.SOUL_SOIL);
    }

    private static boolean isWater(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).is(Blocks.WATER);
    }

    private static boolean isReplaceableForSand(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.is(Blocks.WATER);
    }

    private static boolean isFreeForSand(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).isAir()
                && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    private static BlockPos randomSurfacePos(ServerLevel level, ServerPlayer player, RandomSource random,
                                             int minDistance, int maxDistance) {
        double angle = random.nextDouble() * Math.PI * 2.0;
        double distance = minDistance + random.nextInt(maxDistance - minDistance + 1);
        int x = (int) Math.floor(player.getX() + Math.cos(angle) * distance);
        int z = (int) Math.floor(player.getZ() + Math.sin(angle) * distance);

        // Under the bedrock roof the heightmap points at the roof itself, so stay near the player.
        if (level.dimension().equals(Level.NETHER)) {
            for (int attempt = 0; attempt < 6; attempt++) {
                int y = (int) Math.floor(player.getY()) + random.nextInt(17) - 8;
                BlockPos pos = new BlockPos(x, y, z);

                if (isFreeForSand(level, pos)) {
                    return pos;
                }
            }
        }

        return surfaceAt(level, x, z);
    }

    private static BlockPos surfaceAt(ServerLevel level, int x, int z) {
        return new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
    }
}
