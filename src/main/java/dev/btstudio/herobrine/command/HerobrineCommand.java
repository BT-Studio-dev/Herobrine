package dev.btstudio.herobrine.command;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import dev.btstudio.herobrine.entity.AlexFriendEntity;
import dev.btstudio.herobrine.entity.Apparition;
import dev.btstudio.herobrine.entity.HerobrineEntity;
import dev.btstudio.herobrine.entity.ModEntities;
import dev.btstudio.herobrine.entity.SteveFriendEntity;
import dev.btstudio.herobrine.event.Apparitions;
import dev.btstudio.herobrine.event.Churches;
import dev.btstudio.herobrine.event.HauntingEvents;
import dev.btstudio.herobrine.event.Dread;
import dev.btstudio.herobrine.event.NotchAppleRitual;
import dev.btstudio.herobrine.event.Sabotage;
import dev.btstudio.herobrine.sound.ModSounds;
import dev.btstudio.herobrine.world.Feature;
import dev.btstudio.herobrine.world.HerobrineName;
import dev.btstudio.herobrine.world.HerobrineSettings;
import dev.btstudio.herobrine.world.PhantomPresence;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * {@code /herobrine} - operator tooling for the mod, so the mob and both shrine rites can be
 * tested without hunting for a haunting or stacking blocks by hand.
 *
 * <pre>
 * /herobrine summon [&lt;stalker|hostile|elite|watcher&gt;]
 * /herobrine haunt [&lt;player&gt;]
 * /herobrine leaves | tunnel | structure [water] | tree | cobble | warts
 * /herobrine torches | curse | candles | sign | donation | door | fire
 * /herobrine phantom [add|clear]
 * /herobrine mode [full|purist|toggle]
 * /herobrine apparition &lt;watch|scare|jumpscare|spike|runner|lurking|dwelling&gt; [&lt;player&gt;]
 * /herobrine schizo [on|off]
 * /herobrine hauntstate
 * /herobrine banish [on|off]
 * /herobrine shrine [gold]
 * /herobrine gravel | marker | herd | miner | window
 * /herobrine config [&lt;setting&gt; | all on|off | name &lt;text&gt;|reset | reset | musicvolume]
 * /herobrine steve [spawn|dismiss|stay|follow]
 * /herobrine alex [spawn|dismiss|stay|follow]
 * /herobrine church
 * </pre>
 */
public final class HerobrineCommand {
    private static final SuggestionProvider<CommandSourceStack> FORM_SUGGESTIONS =
            (context, builder) -> SharedSuggestionProvider.suggest(List.of("stalker", "hostile", "elite", "watcher"), builder);

    /**
     * What each accepted word means, including the obvious guesses. These last are worth carrying:
     * "summon herobrine" is what everyone types first, and bouncing it back as an unknown form is
     * unhelpful when there is exactly one thing it could mean.
     */
    private static final Map<String, String> FORMS = Map.ofEntries(
            Map.entry("stalker", "stalker"),
            Map.entry("hostile", "hostile"),
            Map.entry("elite", "elite"),
            Map.entry("watcher", "watcher"),
            Map.entry("herobrine", "stalker"),
            Map.entry("default", "stalker"),
            Map.entry("normal", "stalker"),
            Map.entry("passive", "stalker"),
            Map.entry("aggressive", "hostile"),
            Map.entry("angry", "hostile"));

    /** Forms that used to exist and were deliberately taken out, so they can be named as such. */
    private static final Set<String> REMOVED_FORMS = Set.of("inferno");
    private static final SuggestionProvider<CommandSourceStack> APPARITION_SUGGESTIONS =
            (context, builder) -> SharedSuggestionProvider.suggest(
                    List.of("watch", "scare", "jumpscare", "spike", "runner", "lurking", "dwelling"), builder);
    private static final SuggestionProvider<CommandSourceStack> RING_SUGGESTIONS =
            (context, builder) -> SharedSuggestionProvider.suggest(List.of("gold"), builder);

    /**
     * Which switch governs each one-off trace command. Kept next to the command tree it belongs to
     * so that turning a thing off really does stop it, however it was asked for.
     */
    private static final Map<String, Feature> TRACE_FEATURES = Map.ofEntries(
            Map.entry("leaves", Feature.LEAVES),
            Map.entry("tunnel", Feature.TUNNELS),
            Map.entry("structure", Feature.STRUCTURES),
            Map.entry("structure.water", Feature.STRUCTURES),
            Map.entry("tree", Feature.TREES),
            Map.entry("cobble", Feature.COBBLE),
            Map.entry("warts", Feature.WARTS),
            Map.entry("gravel", Feature.GRAVEL),
            Map.entry("marker", Feature.MARKERS),
            Map.entry("herd", Feature.HERD),
            Map.entry("miner", Feature.MINER),
            Map.entry("window", Feature.WINDOW),
            Map.entry("torches", Feature.CANDLES),
            Map.entry("curse", Feature.CURSE),
            Map.entry("candles", Feature.CANDLES),
            Map.entry("sign", Feature.SIGNS),
            Map.entry("donation", Feature.DONATION),
            Map.entry("door", Feature.DOORS),
            Map.entry("fire", Feature.FIRE));

    private HerobrineCommand() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> {
            // The permission sits on each branch rather than on the root. Brigadier tests the
            // requirement of every node it walks through.
            LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("herobrine")
                    .executes(context -> {
                        context.getSource().sendSuccess(() -> Component.translatable("herobrine.command.help"), false);
                        return 1;
                    });

            // The form is optional: /herobrine summon on its own gives you the stalker rather
            // than a syntax error.
            root.then(op("summon")
                    .executes(context -> summon(context, "stalker"))
                    .then(Commands.argument("form", StringArgumentType.word())
                            .suggests(FORM_SUGGESTIONS)
                            .executes(context -> summon(context,
                                    StringArgumentType.getString(context, "form")))));

            root.then(op("haunt")
                    .executes(HerobrineCommand::hauntSelf)
                    .then(Commands.argument("player", EntityArgument.player())
                            .executes(HerobrineCommand::hauntTarget)));

            root.then(op("leaves").executes(context -> trace(context, "leaves")));
            root.then(op("tunnel").executes(context -> trace(context, "tunnel")));
            root.then(op("structure")
                    .executes(context -> trace(context, "structure"))
                    .then(Commands.literal("water").executes(context -> trace(context, "structure.water"))));
            root.then(op("tree").executes(context -> trace(context, "tree")));
            root.then(op("cobble").executes(context -> trace(context, "cobble")));
            root.then(op("warts").executes(context -> trace(context, "warts")));
            root.then(op("gravel").executes(context -> trace(context, "gravel")));
            root.then(op("marker").executes(context -> trace(context, "marker")));
            root.then(op("herd").executes(context -> trace(context, "herd")));
            root.then(op("miner").executes(context -> trace(context, "miner")));
            root.then(op("window").executes(context -> trace(context, "window")));

            root.then(op("steve")
                    .executes(context -> companionStatus(context, STEVE_COMPANION))
                    .then(Commands.literal("spawn")
                            .executes(context -> companionSpawn(context, STEVE_COMPANION)))
                    .then(Commands.literal("dismiss")
                            .executes(context -> companionDismiss(context, STEVE_COMPANION)))
                    .then(Commands.literal("stay")
                            .executes(context -> companionStay(context, STEVE_COMPANION, true)))
                    .then(Commands.literal("follow")
                            .executes(context -> companionStay(context, STEVE_COMPANION, false))));

            root.then(op("alex")
                    .executes(context -> companionStatus(context, ALEX_COMPANION))
                    .then(Commands.literal("spawn")
                            .executes(context -> companionSpawn(context, ALEX_COMPANION)))
                    .then(Commands.literal("dismiss")
                            .executes(context -> companionDismiss(context, ALEX_COMPANION)))
                    .then(Commands.literal("stay")
                            .executes(context -> companionStay(context, ALEX_COMPANION, true)))
                    .then(Commands.literal("follow")
                            .executes(context -> companionStay(context, ALEX_COMPANION, false))));

            root.then(configBranch());
            root.then(op("torches").executes(context -> trace(context, "torches")));
            root.then(op("curse").executes(context -> trace(context, "curse")));
            root.then(op("candles").executes(context -> trace(context, "candles")));
            root.then(op("sign").executes(context -> trace(context, "sign")));
            root.then(op("donation").executes(context -> trace(context, "donation")));
            root.then(op("door").executes(context -> trace(context, "door")));
            root.then(op("fire").executes(context -> trace(context, "fire")));

            root.then(op("phantom")
                    .executes(HerobrineCommand::phantomStatus)
                    .then(Commands.literal("add").executes(HerobrineCommand::phantomAdd))
                    .then(Commands.literal("clear").executes(HerobrineCommand::phantomClear)));

            root.then(op("mode")
                    .executes(HerobrineCommand::modeStatus)
                    .then(Commands.literal("full").executes(context -> setMode(context, false)))
                    .then(Commands.literal("purist").executes(context -> setMode(context, true)))
                    .then(Commands.literal("toggle").executes(context ->
                            setMode(context, !HerobrineSettings.purist(context.getSource().getLevel())))));

            root.then(op("apparition")
                    .then(Commands.argument("type", StringArgumentType.word())
                            .suggests(APPARITION_SUGGESTIONS)
                            .executes(HerobrineCommand::apparitionSelf)
                            .then(Commands.argument("player", EntityArgument.player())
                                    .executes(HerobrineCommand::apparitionTarget))));

            root.then(op("schizo")
                    .executes(HerobrineCommand::toggleSchizo)
                    .then(Commands.literal("on").executes(context -> setSchizo(context, true)))
                    .then(Commands.literal("off").executes(context -> setSchizo(context, false))));

            root.then(op("hauntstate").executes(HerobrineCommand::hauntState));

            root.then(op("banish")
                    .executes(HerobrineCommand::toggleBanish)
                    .then(Commands.literal("on").executes(context -> setBanish(context, true)))
                    .then(Commands.literal("off").executes(context -> setBanish(context, false))));

            root.then(op("church").executes(HerobrineCommand::buildChurch));

            root.then(op("shrine")
                    .executes(context -> buildShrine(context, "gold"))
                    .then(Commands.argument("ring", StringArgumentType.word())
                            .suggests(RING_SUGGESTIONS)
                            .executes(context -> buildShrine(context, StringArgumentType.getString(context, "ring")
                                    .toLowerCase(Locale.ROOT)))));

            dispatcher.register(root);
        });
    }

    /**
     * A branch that changes the world, the haunting or the mod, so it stays behind the operator
     * permission. Nested literals are covered by their parent: Brigadier never descends into a
     * node whose requirement failed.
     */
    private static LiteralArgumentBuilder<CommandSourceStack> op(String name) {
        return Commands.literal(name).requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
    }

    private static int summon(CommandContext<CommandSourceStack> context, String word) {
        CommandSourceStack source = context.getSource();
        String asked = word.toLowerCase(Locale.ROOT);
        String form = FORMS.get(asked);
        ServerLevel level = source.getLevel();
        Vec3 pos = source.getPosition();

        if (form == null) {
            source.sendFailure(REMOVED_FORMS.contains(asked)
                    ? Component.translatable("herobrine.command.summon.removed_form", asked)
                    : Component.translatable("herobrine.command.summon.unknown_form", asked));
            return 0;
        }

        HerobrineEntity herobrine = ModEntities.HEROBRINE.create(level, EntitySpawnReason.COMMAND);

        if (herobrine == null) {
            source.sendFailure(Component.translatable("herobrine.command.summon.failed"));
            return 0;
        }

        herobrine.snapTo(pos.x, pos.y, pos.z, level.getRandom().nextFloat() * 360.0F, 0.0F);

        if (form.equals("stalker")) {
            herobrine.setHostile(false);
        } else if (form.equals("watcher")) {
            herobrine.setWatcher(true);
        } else {
            herobrine.setHostile(true);

            if (form.equals("elite")) {
                herobrine.setElite(true);
            }
        }

        herobrine.finalizeSpawn(level, level.getCurrentDifficultyAt(herobrine.blockPosition()),
                EntitySpawnReason.COMMAND, null);

        // A freshly spawned elite has the base 60 HP of the normal mob until topped up.
        if (herobrine.isElite()) {
            herobrine.setHealth(herobrine.getMaxHealth());
        }

        if (!level.tryAddFreshEntityWithPassengers(herobrine)) {
            source.sendFailure(Component.translatable("herobrine.command.summon.failed"));
            return 0;
        }

        source.sendSuccess(() -> Component.translatable("herobrine.command.summon.success", form), true);
        return 1;
    }

    private static int hauntSelf(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return haunt(context, context.getSource().getPlayerOrException());
    }

    private static int hauntTarget(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return haunt(context, EntityArgument.getPlayer(context, "player"));
    }

    private static int haunt(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        CommandSourceStack source = context.getSource();

        HauntingEvents.runRandomEvent(source.getServer(), target, target.getRandom());

        source.sendSuccess(
                () -> Component.translatable("herobrine.command.haunt.success", target.getDisplayName()),
                true);
        return 1;
    }

    private static int trace(CommandContext<CommandSourceStack> context, String trace)
            throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = source.getLevel();
        RandomSource random = level.getRandom();
        Feature feature = TRACE_FEATURES.getOrDefault(trace, Feature.FIRE);

        if (switchedOff(source, feature)) {
            return 0;
        }

        switch (trace) {
            case "leaves" -> HauntingEvents.stripLeaves(level, player, random);
            case "tunnel" -> HauntingEvents.digTunnel(level, player, random);
            case "structure" -> {
                // Water pyramid first - oceans and rivers are his favourite - then a dry one.
                if (!HauntingEvents.buildPyramid(level, player, random, true)) {
                    HauntingEvents.buildPyramid(level, player, random, false);
                }
            }
            case "structure.water" -> HauntingEvents.buildPyramid(level, player, random, true);
            case "tree" -> HauntingEvents.stripTreeLogs(level, player, random);
            case "cobble" -> HauntingEvents.placeCobblestone(level, player, random);
            case "warts" -> HauntingEvents.stripNetherWarts(level, player);
            case "gravel" -> HauntingEvents.placeGravelMound(level, player, random);
            case "marker" -> HauntingEvents.placeMarker(level, player, random);
            case "herd" -> HauntingEvents.herdEvent(level, player, random);
            case "miner" -> HauntingEvents.ghostMiner(level, player, random);
            case "window" -> HauntingEvents.windowSighting(level, player, random);
            case "torches" -> Sabotage.snuffLights(level, player, random);
            case "curse" -> Sabotage.curseLights(level, player, random);
            case "candles" -> Sabotage.douseFlames(level, player, random);
            case "sign" -> Sabotage.creepySign(level, player, random, HerobrineSettings.name(level));
            case "donation" -> Sabotage.donation(level, player, random);
            case "door" -> Sabotage.ghostDoor(level, player, random);
            default -> Sabotage.arson(level, player, random);
        }

        source.sendSuccess(() -> Component.translatable("herobrine.command.trace." + trace), true);
        return 1;
    }

    /** Something has to sit inside {@code PhantomPresence}; this is the operator side of it. */
    private static int phantomAdd(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPos.containing(source.getPosition());

        if (!PhantomPresence.add(level, pos)) {
            source.sendFailure(Component.translatable("herobrine.command.phantom.duplicate"));
            return 0;
        }

        source.sendSuccess(() -> Component.translatable("herobrine.command.phantom.added",
                pos.getX(), pos.getY(), pos.getZ(), (int) PhantomPresence.RADIUS), true);
        return 1;
    }

    private static int phantomClear(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        int removed = PhantomPresence.clear(source.getLevel());

        source.sendSuccess(() -> Component.translatable("herobrine.command.phantom.cleared", removed), true);
        return removed;
    }

    private static int phantomStatus(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        int count = PhantomPresence.positions(level).size();

        if (count == 0) {
            source.sendSuccess(() -> Component.translatable("herobrine.command.phantom.none"), false);
            return 0;
        }

        BlockPos here = BlockPos.containing(source.getPosition());
        int nearest = (int) Math.sqrt(PhantomPresence.nearestDistanceSqr(level, here));

        source.sendSuccess(() -> Component.translatable("herobrine.command.phantom.status", count, nearest), false);
        return count;
    }

    private static int apparitionSelf(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return apparition(context, context.getSource().getPlayerOrException());
    }

    private static int apparitionTarget(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return apparition(context, EntityArgument.getPlayer(context, "player"));
    }

    /** Puts one of the four <i>Him</i> apparitions next to a player, on demand. */
    private static int apparition(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        CommandSourceStack source = context.getSource();
        String name = StringArgumentType.getString(context, "type");
        Apparition apparition = Apparition.byName(name);

        if (apparition == null) {
            source.sendFailure(Component.translatable("herobrine.command.apparition.unknown", name));
            return 0;
        }

        if (switchedOff(source, apparition.feature())) {
            return 0;
        }

        // The command can target a player in another dimension; search and spawn in their world,
        // not the world from which the command was run.
        ServerLevel level = (ServerLevel) target.level();
        HerobrineEntity herobrine = Apparitions.spawn(level, target, apparition, level.getRandom());

        if (herobrine == null) {
            source.sendFailure(Component.translatable("herobrine.command.apparition.failed"));
            return 0;
        }

        source.sendSuccess(() -> Component.translatable("herobrine.command.apparition.success",
                apparition.id(), target.getDisplayName()), true);
        return 1;
    }

    private static int toggleSchizo(CommandContext<CommandSourceStack> context) {
        return reportSchizo(context, HerobrineSettings.toggleSchizo(context.getSource().getLevel()));
    }

    private static int setSchizo(CommandContext<CommandSourceStack> context, boolean schizo) {
        HerobrineSettings.setSchizo(context.getSource().getLevel(), schizo);
        return reportSchizo(context, schizo);
    }

    private static int reportSchizo(CommandContext<CommandSourceStack> context, boolean schizo) {
        CommandSourceStack source = context.getSource();

        source.sendSuccess(() -> Component.translatable("herobrine.command.schizo.set",
                Component.translatable(onOffKey(schizo))), true);
        return schizo ? 1 : 0;
    }

    private static int toggleBanish(CommandContext<CommandSourceStack> context) {
        return reportBanish(context, HerobrineSettings.toggleBanished(context.getSource().getLevel()));
    }

    private static int setBanish(CommandContext<CommandSourceStack> context, boolean banished) {
        HerobrineSettings.setBanished(context.getSource().getLevel(), banished);
        return reportBanish(context, banished);
    }

    private static int reportBanish(CommandContext<CommandSourceStack> context, boolean banished) {
        CommandSourceStack source = context.getSource();

        if (banished) {
            NotchAppleRitual.purgeApparitions(source.getLevel());
        }

        source.sendSuccess(() -> Component.translatable(
                banished ? "herobrine.message.banished" : "herobrine.message.returned"), true);
        return banished ? 1 : 0;
    }

    /** Everything the haunting is currently doing, in three lines. */
    private static int hauntState(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        HerobrineSettings settings = HerobrineSettings.of(level);
        long chase = chaseSeconds(level);
        int stage = stageOf(source);
        int interval = HauntingEvents.intervalFor(level, stage);

        source.sendSuccess(() -> Component.translatable("herobrine.command.state.mode",
                Component.translatable(modeKey(settings.isPurist())),
                Component.translatable(settings.isBanished()
                        ? "herobrine.command.state.banished" : "herobrine.command.state.haunted"),
                Component.translatable(onOffKey(settings.isSchizo()))), false);

        source.sendSuccess(() -> chaseLine(chase), false);
        source.sendSuccess(() -> Component.translatable("herobrine.command.state.interval", interval, stage), false);

        if (settings.enabled(Feature.ESCALATION)) {
            source.sendSuccess(() -> Component.translatable("herobrine.command.state.dread",
                    settings.dread(), HerobrineSettings.MAX_DREAD, Dread.tierOf(settings.dread())), false);
        }

        return 1;
    }

    /** Seconds left in the current chase frenzy; 0 when he is not chasing anyone. */
    private static long chaseSeconds(ServerLevel level) {
        return HerobrineSettings.chaseRemainingTicks(level) / 20L;
    }

    /** The chase line, used by {@code /herobrine hauntstate}. */
    private static Component chaseLine(long chaseSeconds) {
        return chaseSeconds > 0L
                ? Component.translatable("herobrine.command.state.chase.active", chaseSeconds)
                : Component.translatable("herobrine.command.state.chase.idle");
    }

    /** The escalation stage, which is tracked per player and is 0 for anything else. */
    private static int stageOf(CommandSourceStack source) {
        return source.getEntity() instanceof ServerPlayer player ? HauntingEvents.stageOf(player) : 0;
    }

    private static String onOffKey(boolean value) {
        return value ? "herobrine.command.state.on" : "herobrine.command.state.off";
    }

    private static int modeStatus(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        boolean purist = HerobrineSettings.purist(source.getLevel());

        source.sendSuccess(() -> Component.translatable("herobrine.command.mode.status",
                Component.translatable(modeKey(purist))), false);
        return purist ? 1 : 0;
    }

    private static int setMode(CommandContext<CommandSourceStack> context, boolean purist) {
        CommandSourceStack source = context.getSource();
        HerobrineSettings.setPurist(source.getLevel(), purist);

        source.sendSuccess(() -> Component.translatable("herobrine.command.mode.set",
                Component.translatable(modeKey(purist))), true);
        return 1;
    }

    private static String modeKey(boolean purist) {
        return purist ? "herobrine.command.mode.purist" : "herobrine.command.mode.full";
    }

    /**
     * {@code /herobrine config}: one branch that flips a single switch, plus the branches that set
     * them all at once, rename him, and put everything back. Every literal under it is built from
     * {@link Feature#commandIds()}, so adding a switch to the enum is all it takes to add its
     * command - and the old names ({@code arson}, {@code fog}) stay where they have always been.
     */
    private static LiteralArgumentBuilder<CommandSourceStack> configBranch() {
        LiteralArgumentBuilder<CommandSourceStack> config = op("config")
                .executes(HerobrineCommand::configStatus)
                .then(Commands.literal("reset").executes(HerobrineCommand::resetConfig))
                .then(Commands.literal("all")
                        .then(Commands.literal("on").executes(context -> setAllFeatures(context, true)))
                        .then(Commands.literal("off").executes(context -> setAllFeatures(context, false))))
                .then(Commands.literal("name")
                        .executes(HerobrineCommand::showName)
                        .then(Commands.literal("reset").executes(HerobrineCommand::resetName))
                        .then(Commands.argument("value", StringArgumentType.word())
                                .executes(HerobrineCommand::setName)))
                .then(Commands.literal("musicvolume")
                        .executes(HerobrineCommand::cycleMusicVolume)
                        .then(Commands.argument("percent", IntegerArgumentType.integer(0, 100))
                                .executes(HerobrineCommand::setMusicVolume)))
                .then(Commands.literal("musictest").executes(HerobrineCommand::musicTest));

        for (String id : Feature.commandIds()) {
            config.then(Commands.literal(id).executes(context -> toggleFeature(context, id)));
        }

        return config;
    }

    /** Flips one switch and says so. */
    private static int toggleFeature(CommandContext<CommandSourceStack> context, String id) {
        CommandSourceStack source = context.getSource();
        Feature feature = Feature.byCommandId(id);
        boolean on = HerobrineSettings.toggle(source.getLevel(), feature);

        source.sendSuccess(() -> Component.translatable("herobrine.command.config.set",
                Component.translatable(feature.titleKey()),
                Component.translatable(onOffKey(on))), true);
        return on ? 1 : 0;
    }

    /** Every switch at once: {@code /herobrine config all on|off}. */
    private static int setAllFeatures(CommandContext<CommandSourceStack> context, boolean on) {
        CommandSourceStack source = context.getSource();
        HerobrineSettings.setAll(source.getLevel(), on);

        source.sendSuccess(() -> Component.translatable("herobrine.command.config.all",
                Component.translatable(onOffKey(on))), true);
        return 1;
    }

    private static int showName(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        source.sendSuccess(() -> Component.translatable("herobrine.command.config.name.show",
                HerobrineSettings.name(source.getLevel())), false);
        return 1;
    }

    /**
     * {@code /herobrine config name <text>}: the name he goes by. One short line when the name will
     * not do, rather than a silent correction to something he never said.
     */
    private static int setName(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String value = StringArgumentType.getString(context, "value");

        if (!HerobrineName.isValid(value)) {
            source.sendFailure(Component.translatable("herobrine.command.config.name.invalid",
                    HerobrineName.MAX_LENGTH));
            return 0;
        }

        HerobrineSettings.of(source.getLevel()).setName(value);
        source.sendSuccess(() -> Component.translatable("herobrine.command.config.name.set", value), true);
        return 1;
    }

    /** {@code /herobrine config name reset}: back to the name he has always gone by. */
    private static int resetName(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        HerobrineSettings.of(source.getLevel()).setName(HerobrineName.DEFAULT);

        source.sendSuccess(() -> Component.translatable("herobrine.command.config.name.reset",
                Component.translatable("herobrine.name.default")), true);
        return 1;
    }

    /**
     * True when this part of the haunting is switched off in the sender's dimension, having told
     * them so. Used by every command that reaches for a feature directly.
     */
    private static boolean switchedOff(CommandSourceStack source, Feature feature) {
        if (HerobrineSettings.enabled(source.getLevel(), feature)) {
            return false;
        }

        source.sendFailure(Component.translatable("herobrine.command.feature.off",
                Component.translatable(feature.titleKey())));
        return true;
    }

    /** Restore the user-facing settings to the defaults for this dimension. */
    private static int resetConfig(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        HerobrineSettings settings = HerobrineSettings.of(source.getLevel());

        for (Feature feature : Feature.values()) {
            settings.setEnabled(feature, feature.defaultOn());
        }

        settings.setMusicVolume(70);
        settings.setPurist(false);
        settings.setBanished(false);
        settings.setName(HerobrineName.DEFAULT);
        source.sendSuccess(() -> Component.translatable("herobrine.command.config.reset"), true);
        return 1;
    }

    /**
     * {@code /herobrine config musictest}: plays the haunting track there and then, at the volume
     * the dimension is set to, skipping the two things that normally gate it - he has to be within
     * 28 blocks, and he has not played it in the last five minutes. If you hear it, the sound and
     * the volume are fine and what you are really looking at is how rarely those conditions line
     * up; if you hear nothing, the switch or the volume is what is wrong.
     */
    private static int musicTest(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        HerobrineSettings settings = HerobrineSettings.of(level);

        // A test that answers "you switched it off" is not much of a test. The point of pressing
        // Play is to hear the track, so the two things that silence it are put right first and
        // then reported - otherwise **All off** turns this into an error, which is what it looked
        // like when it was really the switch doing its job.
        boolean musicWasOff = !settings.isMusic();
        boolean volumeWasSilent = settings.musicVolume() <= 0;

        if (musicWasOff) {
            settings.setMusic(true);
        }

        if (volumeWasSilent) {
            settings.setMusicVolume(HerobrineSettings.DEFAULT_MUSIC_VOLUME);
        }

        // Read once: the message below is built in a lambda, and a local it captures has to be
        // effectively final.
        boolean revived = musicWasOff || volumeWasSilent;
        float volume = HauntingEvents.musicTestVolume(level);

        if (volume <= 0.0F) {
            source.sendFailure(Component.translatable("herobrine.command.config.musictest.silent",
                    Component.translatable(onOffKey(settings.isMusic())),
                    Component.literal(settings.musicVolume() + "%")));
            return 0;
        }

        Vec3 at = source.getPosition();
        level.playSound(null, at.x(), at.y(), at.z(), ModSounds.MUSIC_HAUNT, SoundSource.MUSIC, volume, 1.0F);
        source.sendSuccess(() -> Component.translatable(revived
                        ? "herobrine.command.config.musictest.revived"
                        : "herobrine.command.config.musictest.playing",
                Component.literal(Math.round(volume * 100) + "%")), false);
        return 1;
    }

    /**
     * {@code /herobrine config} on its own: a one line reminder of what the branch does.
     */
    private static int configStatus(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(
                () -> Component.translatable("herobrine.command.config.status"), false);
        return 1;
    }

    /**
     * The two companions. They follow, defend and wait identically - that is the point of them -
     * so the four commands are written once against this and registered for each in turn.
     */
    private record Companion<T extends SteveFriendEntity>(Class<T> type, EntityType<T> entityType,
                                                         Feature feature, String key) {
        private List<T> mine(ServerLevel level, ServerPlayer player) {
            return level.getEntitiesOfClass(this.type, player.getBoundingBox().inflate(64.0),
                    companion -> companion.isOwnedBy(player));
        }

        private MutableComponent message(String suffix, Object... args) {
            return Component.translatable("herobrine.command." + this.key + "." + suffix, args);
        }
    }

    private static final Companion<SteveFriendEntity> STEVE_COMPANION =
            new Companion<>(SteveFriendEntity.class, ModEntities.STEVE_FRIEND, Feature.STEVE, "steve");

    private static final Companion<AlexFriendEntity> ALEX_COMPANION =
            new Companion<>(AlexFriendEntity.class, ModEntities.ALEX_FRIEND, Feature.ALEX, "alex");

    /** Summons the companion three blocks in front of you. One of each per player, not a crowd. */
    private static <T extends SteveFriendEntity> int companionSpawn(
            CommandContext<CommandSourceStack> context, Companion<T> companion)
            throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();

        if (switchedOff(source, companion.feature())) {
            return 0;
        }

        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = source.getLevel();

        for (T existing : companion.mine(level, player)) {
            if (existing.isAlive()) {
                source.sendFailure(companion.message("already"));
                return 0;
            }
        }

        Vec3 ahead = player.position().add(player.getLookAngle().multiply(3.0, 0.0, 3.0));
        T friend = companion.entityType().create(level, EntitySpawnReason.COMMAND);

        if (friend == null) {
            source.sendFailure(companion.message("help"));
            return 0;
        }

        friend.setPos(ahead.x, player.getY(), ahead.z);
        friend.setYRot(player.getYRot());
        friend.setOwner(player);
        friend.setHealth(friend.getMaxHealth());
        level.addFreshEntity(friend);

        source.sendSuccess(() -> companion.message("spawned"), true);
        return 1;
    }

    private static <T extends SteveFriendEntity> int companionDismiss(
            CommandContext<CommandSourceStack> context, Companion<T> companion)
            throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        List<T> friends = companion.mine(source.getLevel(), player);

        if (friends.isEmpty()) {
            source.sendFailure(companion.message("none"));
            return 0;
        }

        friends.forEach(Entity::discard);
        source.sendSuccess(() -> companion.message("dismissed"), true);
        return 1;
    }

    private static <T extends SteveFriendEntity> int companionStay(
            CommandContext<CommandSourceStack> context, Companion<T> companion, boolean stay)
            throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        List<T> friends = companion.mine(source.getLevel(), player);

        if (friends.isEmpty()) {
            source.sendFailure(companion.message("none"));
            return 0;
        }

        friends.forEach(friend -> friend.setStaying(stay));
        source.sendSuccess(() -> companion.message(stay ? "stay" : "follow"), true);
        return 1;
    }

    private static <T extends SteveFriendEntity> int companionStatus(
            CommandContext<CommandSourceStack> context, Companion<T> companion)
            throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        List<T> friends = companion.mine(source.getLevel(), player);

        if (friends.isEmpty()) {
            source.sendSuccess(() -> companion.message("help"), false);
            return 1;
        }

        boolean staying = friends.stream().allMatch(SteveFriendEntity::isStaying);
        source.sendSuccess(() -> companion.message("status",
                companion.message(staying ? "state.stay" : "state.follow")), false);
        return 1;
    }

    private static int cycleMusicVolume(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        int volume = HerobrineSettings.cycleMusicVolume(source.getLevel());

        source.sendSuccess(() -> Component.translatable("herobrine.command.config.volume", volume), true);
        return volume;
    }

    private static int setMusicVolume(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        int requested = IntegerArgumentType.getInteger(context, "percent");
        int volume = HerobrineSettings.setMusicVolume(source.getLevel(), requested);

        source.sendSuccess(() -> Component.translatable("herobrine.command.config.volume", volume), true);
        return volume;
    }

    /** Raises a church (and its holy water) eight blocks in front of the player. */
    private static int buildChurch(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();

        if (switchedOff(source, Feature.CHURCHES)) {
            return 0;
        }

        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = source.getLevel();
        BlockPos ahead = player.blockPosition().relative(player.getDirection(), 8);
        BlockPos origin = new BlockPos(ahead.getX(),
                level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ahead.getX(), ahead.getZ()),
                ahead.getZ());

        if (!Churches.build(level, origin)) {
            source.sendFailure(Component.translatable("herobrine.command.church.failed"));
            return 0;
        }

        source.sendSuccess(() -> Component.translatable("herobrine.command.church.success"), true);
        return 1;
    }

    /** Builds the eight-block gold shrine two blocks in front of the player, sunk into the ground. */
    private static int buildShrine(CommandContext<CommandSourceStack> context, String ring)
            throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();

        if (switchedOff(source, Feature.SHRINE)) {
            return 0;
        }


        if (!ring.equals("gold")) {
            source.sendFailure(Component.translatable("herobrine.command.shrine.unknown_ring", ring));
            return 0;
        }

        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = source.getLevel();

        // Two blocks ahead of the player, so the netherrack is not placed where they stand, and one
        // down: the shrine is sunk in a shallow pit. The plate sits below the surface with only the
        // netherrack and the four torches showing at the rim.
        BlockPos center = player.blockPosition().relative(player.getDirection(), 2).below();

        level.setBlock(center, Blocks.NETHERRACK.defaultBlockState(), 3);
        level.setBlock(center.above(), Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(center.above(2), Blocks.AIR.defaultBlockState(), 3);

        // The full ring: all eight horizontal neighbours, not just the four sides. The block above
        // each one is cleared so the pit is open and the torches have somewhere to stand.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }

                level.setBlock(center.offset(dx, 0, dz), Blocks.GOLD_BLOCK.defaultBlockState(), 3);
                level.setBlock(center.offset(dx, 1, dz), Blocks.AIR.defaultBlockState(), 3);
            }
        }

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            level.setBlock(center.relative(direction).above(), Blocks.REDSTONE_TORCH.defaultBlockState(), 3);
        }

        source.sendSuccess(() -> Component.translatable("herobrine.command.shrine.success"), true);
        return 1;
    }
}
