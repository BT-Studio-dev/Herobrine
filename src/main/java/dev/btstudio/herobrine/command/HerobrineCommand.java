package dev.btstudio.herobrine.command;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import dev.btstudio.herobrine.entity.Apparition;
import dev.btstudio.herobrine.entity.HerobrineEntity;
import dev.btstudio.herobrine.entity.ModEntities;
import dev.btstudio.herobrine.entity.SteveFriendEntity;
import dev.btstudio.herobrine.event.Apparitions;
import dev.btstudio.herobrine.event.Churches;
import dev.btstudio.herobrine.event.HauntingEvents;
import dev.btstudio.herobrine.event.NotchAppleRitual;
import dev.btstudio.herobrine.event.Sabotage;
import dev.btstudio.herobrine.item.ModItems;
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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * {@code /herobrine} - operator tooling for the mod, so the mob and both shrine rites can be
 * tested without hunting for a haunting or stacking blocks by hand.
 *
 * <pre>
 * /herobrine summon &lt;stalker|hostile|elite&gt;
 * /herobrine haunt [&lt;player&gt;]
 * /herobrine leaves | tunnel | structure [water] | tree | cobble | warts
 * /herobrine torches | curse | candles | sign | donation | door | fire
 * /herobrine phantom [add|clear]
 * /herobrine mode [full|purist|toggle]
 * /herobrine apparition &lt;watch|scare|jumpscare|spike|runner|lurking|dwelling&gt; [&lt;player&gt;]
 * /herobrine schizo [on|off]
 * /herobrine hauntstate
 * /herobrine settings
 * /herobrine banish [on|off]
 * /herobrine shrine [gold]
 * /herobrine gravel | marker | herd | miner | window
 * /herobrine config [&lt;setting&gt; | all on|off | name &lt;text&gt;|reset | reset | musicvolume]
 * /herobrine steve [spawn|dismiss|stay|follow]
 * /herobrine church
 * /herobrine give &lt;iron_stick|iron_sword|iron_helmet|iron_chestplate|iron_leggings|iron_boots|holy_water|the_cross&gt;
 * </pre>
 */
public final class HerobrineCommand {
    private static final SuggestionProvider<CommandSourceStack> FORM_SUGGESTIONS =
            (context, builder) -> SharedSuggestionProvider.suggest(List.of("stalker", "hostile", "elite", "watcher"), builder);
    private static final SuggestionProvider<CommandSourceStack> APPARITION_SUGGESTIONS =
            (context, builder) -> SharedSuggestionProvider.suggest(
                    List.of("watch", "scare", "jumpscare", "spike", "runner", "lurking", "dwelling"), builder);
    private static final SuggestionProvider<CommandSourceStack> GIVE_SUGGESTIONS =
            (context, builder) -> SharedSuggestionProvider.suggest(
                    List.of("iron_stick", "iron_sword", "iron_helmet", "iron_chestplate", "iron_leggings",
                            "iron_boots", "holy_water", "the_cross"), builder);
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
            // requirement of every node it walks through, so a requirement here would also hide
            // /herobrine settings, which only reads values and is open to everyone.
            LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("herobrine")
                    .executes(context -> {
                        context.getSource().sendSuccess(() -> Component.translatable("herobrine.command.help"), false);
                        return 1;
                    });

            root.then(op("summon")
                    .then(Commands.argument("form", StringArgumentType.word())
                            .suggests(FORM_SUGGESTIONS)
                            .executes(HerobrineCommand::summon)));

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
                    .executes(HerobrineCommand::steveStatus)
                    .then(Commands.literal("spawn").executes(HerobrineCommand::steveSpawn))
                    .then(Commands.literal("dismiss").executes(HerobrineCommand::steveDismiss))
                    .then(Commands.literal("stay").executes(context -> steveStay(context, true)))
                    .then(Commands.literal("follow").executes(context -> steveStay(context, false))));

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

            root.then(op("give")
                    .then(Commands.argument("item", StringArgumentType.word())
                            .suggests(GIVE_SUGGESTIONS)
                            .executes(HerobrineCommand::give)));

            root.then(op("shrine")
                    .executes(context -> buildShrine(context, "gold"))
                    .then(Commands.argument("ring", StringArgumentType.word())
                            .suggests(RING_SUGGESTIONS)
                            .executes(context -> buildShrine(context, StringArgumentType.getString(context, "ring")
                                    .toLowerCase(Locale.ROOT)))));

            // Only reads settings, so it is the one branch every player may run.
            root.then(Commands.literal("settings")
                    .requires(Commands.hasPermission(Commands.LEVEL_ALL))
                    .executes(HerobrineCommand::settings));

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

    private static int summon(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String form = StringArgumentType.getString(context, "form").toLowerCase(Locale.ROOT);
        ServerLevel level = source.getLevel();
        Vec3 pos = source.getPosition();

        if (!form.equals("stalker") && !form.equals("hostile") && !form.equals("elite")
                && !form.equals("watcher")) {
            source.sendFailure(Component.translatable("herobrine.command.summon.unknown_form", form));
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
        return 1;
    }

    /**
     * Every setting for the dimension the sender is in, one per line, in a single message. This
     * only reads values, which is why it is the one branch of the command open to everyone: the
     * rest of {@code /herobrine} changes things and stays behind the operator permission.
     */
    private static int settings(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        HerobrineSettings settings = HerobrineSettings.of(level);
        int stage = stageOf(source);
        MutableComponent message = Component.translatable("herobrine.command.settings.header");

        line(message, "herobrine.command.settings.name", Component.literal(settings.name()));
        line(message, "herobrine.command.settings.mode",
                Component.translatable(modeKey(settings.isPurist())));
        line(message, "herobrine.command.settings.banished",
                Component.translatable(onOffKey(settings.isBanished())));
        line(message, "herobrine.command.settings.volume", Component.literal(settings.musicVolume() + "%"));
        line(message, "herobrine.command.settings.stage", Component.literal(String.valueOf(stage)));
        line(message, "herobrine.command.settings.interval",
                Component.literal(String.valueOf(HauntingEvents.intervalFor(level, stage))));
        message.append(Component.literal("\n")).append(chaseLine(chaseSeconds(level)));

        // Then every switch there is, filed under the same headings the settings screen uses.
        for (Feature.Group group : Feature.Group.values()) {
            message.append(Component.literal("\n"))
                    .append(Component.translatable("herobrine.command.settings.group."
                            + group.name().toLowerCase(Locale.ROOT)));

            for (Feature feature : Feature.inGroup(group)) {
                line(message, "herobrine.command.settings.feature",
                        Component.translatable(feature.titleKey()),
                        Component.translatable(onOffKey(settings.enabled(feature))));
            }
        }

        source.sendSuccess(() -> message, false);
        return 1;
    }

    /** One indented line on the settings listing. */
    private static void line(MutableComponent message, String key, Object... args) {
        message.append(Component.literal("\n")).append(Component.translatable(key, args));
    }

    /** Seconds left in the current chase frenzy; 0 when he is not chasing anyone. */
    private static long chaseSeconds(ServerLevel level) {
        return HerobrineSettings.chaseRemainingTicks(level) / 20L;
    }

    /** The chase line, shared by {@code /herobrine hauntstate} and {@code /herobrine settings}. */
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
        float volume = HauntingEvents.musicTestVolume(level);

        if (volume <= 0.0F) {
            source.sendFailure(Component.translatable("herobrine.command.config.musictest.silent",
                    Component.translatable(onOffKey(settings.isMusic())),
                    Component.literal(settings.musicVolume() + "%")));
            return 0;
        }

        Vec3 at = source.getPosition();
        level.playSound(null, at.x(), at.y(), at.z(), ModSounds.MUSIC_HAUNT, SoundSource.MUSIC, volume, 1.0F);
        source.sendSuccess(() -> Component.translatable("herobrine.command.config.musictest.playing",
                Component.literal(Math.round(volume * 100) + "%")), false);
        return 1;
    }

    /**
     * {@code /herobrine config} on its own: a one line reminder of what the branch does rather than
     * a dump of thirty-odd values, which is what {@code /herobrine settings} is for.
     */
    private static int configStatus(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(
                () -> Component.translatable("herobrine.command.config.status"), false);
        return 1;
    }

    /** Every Steve belonging to this player, in this dimension. */
    private static List<SteveFriendEntity> mySteves(ServerLevel level, ServerPlayer player) {
        return level.getEntitiesOfClass(SteveFriendEntity.class,
                player.getBoundingBox().inflate(64.0),
                steve -> steve.isOwnedBy(player));
    }

    /** Summons the friendly Steve three blocks in front of you. One per player, not a crowd. */
    private static int steveSpawn(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();

        if (switchedOff(source, Feature.STEVE)) {
            return 0;
        }

        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = source.getLevel();

        for (SteveFriendEntity existing : mySteves(level, player)) {
            if (existing.isAlive()) {
                source.sendFailure(Component.translatable("herobrine.command.steve.already"));
                return 0;
            }
        }

        Vec3 ahead = player.position().add(player.getLookAngle().multiply(3.0, 0.0, 3.0));
        SteveFriendEntity steve = new SteveFriendEntity(ModEntities.STEVE_FRIEND, level);
        steve.setPos(ahead.x, player.getY(), ahead.z);
        steve.setYRot(player.getYRot());
        steve.setOwner(player);
        steve.setHealth(steve.getMaxHealth());
        level.addFreshEntity(steve);

        source.sendSuccess(() -> Component.translatable("herobrine.command.steve.spawned"), true);
        return 1;
    }

    private static int steveDismiss(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        List<SteveFriendEntity> steves = mySteves(source.getLevel(), player);

        if (steves.isEmpty()) {
            source.sendFailure(Component.translatable("herobrine.command.steve.none"));
            return 0;
        }

        steves.forEach(Entity::discard);
        source.sendSuccess(() -> Component.translatable("herobrine.command.steve.dismissed"), true);
        return 1;
    }

    private static int steveStay(CommandContext<CommandSourceStack> context, boolean stay)
            throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        List<SteveFriendEntity> steves = mySteves(source.getLevel(), player);

        if (steves.isEmpty()) {
            source.sendFailure(Component.translatable("herobrine.command.steve.none"));
            return 0;
        }

        steves.forEach(steve -> steve.setStaying(stay));
        source.sendSuccess(() -> Component.translatable(stay
                ? "herobrine.command.steve.stay"
                : "herobrine.command.steve.follow"), true);
        return 1;
    }

    private static int steveStatus(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        List<SteveFriendEntity> steves = mySteves(source.getLevel(), player);

        if (steves.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("herobrine.command.steve.help"), false);
            return 1;
        }

        boolean staying = steves.stream().allMatch(SteveFriendEntity::isStaying);
        source.sendSuccess(() -> Component.translatable("herobrine.command.steve.status",
                Component.translatable(staying
                        ? "herobrine.command.steve.state.stay"
                        : "herobrine.command.steve.state.follow")), false);
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

    private static int give(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = source.getLevel();
        String name = StringArgumentType.getString(context, "item").toLowerCase(Locale.ROOT);

        Item item = switch (name) {
            case "iron_stick" -> ModItems.IRON_STICK;
            case "iron_sword", "sword" -> ModItems.IRON_SWORD;
            case "iron_helmet", "helmet" -> ModItems.IRON_HELMET;
            case "iron_chestplate", "chestplate" -> ModItems.IRON_CHESTPLATE;
            case "iron_leggings", "leggings" -> ModItems.IRON_LEGGINGS;
            case "iron_boots", "boots" -> ModItems.IRON_BOOTS;
            case "holy_water" -> ModItems.HOLY_WATER;
            case "the_cross", "cross" -> ModItems.THE_CROSS;
            default -> null;
        };

        if (item == null) {
            source.sendFailure(Component.translatable("herobrine.command.give.unknown", name));
            return 0;
        }

        ItemStack stack = new ItemStack(item);

        if (!player.getInventory().add(stack)) {
            level.addFreshEntity(new ItemEntity(level, player.getX(), player.getY() + 0.5, player.getZ(), stack));
        }

        source.sendSuccess(() -> Component.translatable("herobrine.command.give.success",
                stack.getHoverName(), player.getDisplayName()), true);
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
