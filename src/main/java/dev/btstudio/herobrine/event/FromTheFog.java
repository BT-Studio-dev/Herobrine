package dev.btstudio.herobrine.event;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

import dev.btstudio.herobrine.world.HerobrineSettings;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

/**
 * Runs the bundled <i>From The Fog</i> content - and only when a player has asked for it.
 *
 * <p>From The Fog (by Lunar Eclipse Studios, CC BY-NC-SA 4.0) ships as a datapack whose tick and
 * load functions are normally registered through {@code data/minecraft/tags/function/tick.json} and
 * {@code load.json}. We deliberately do not ship those two files: dropping them in would let those
 * functions run the moment the world loads, which is exactly what they must not do here. Two
 * hauntings ticking at once is not a feature.
 *
 * <p>So the entry points are invoked from here instead, and only while the per-dimension
 * <b>From The Fog</b> setting is on (default off, see {@link HerobrineSettings#isFog()}):
 *
 * <ul>
 *   <li>{@code lunareclipse.watching:main/tick} - once per server tick, while at least one
 *       dimension has the setting on.</li>
 *   <li>{@code lunareclipse.watching:main/reload} and {@code lunareclipse.utils:main/reload} - once,
 *       after the data packs are in place, which is what creates the scoreboards, teams and
 *       scheduled functions that content expects.</li>
 * </ul>
 *
 * <p>Nothing here can fail loudly: a missing or broken function is swallowed, because the content is
 * optional and its absence must never spam the console or chat.
 */
public final class FromTheFog {
    private static final String TICK = "function lunareclipse.watching:main/tick";

    /** The two load-time functions, in the order the original {@code load.json} lists them. */
    private static final List<String> LOAD = List.of(
            "function lunareclipse.watching:main/reload",
            "function lunareclipse.utils:main/reload");

    /**
     * Servers whose load functions have already been run once. Weakly held, and cleared on
     * {@code /reload}, so a fresh data pack load re-creates the scoreboards and schedules.
     */
    private static final Set<MinecraftServer> READY = Collections.newSetFromMap(new WeakHashMap<>());

    private FromTheFog() {
    }

    public static void register() {
        // A reload replaces every function, so whatever we did before has to be done again.
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
            READY.remove(server);

            if (success && anyDimensionEnabled(server)) {
                load(server);
            }
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> READY.remove(server));

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            boolean enabled = anyDimensionEnabled(server);

            if (!enabled) {
                return;
            }

            if (!READY.contains(server)) {
                load(server);
            }

            for (ServerLevel level : server.getAllLevels()) {
                if (HerobrineSettings.fog(level)) {
                    run(server, TICK);
                    break;
                }
            }
        });
    }

    /** True when at least one dimension has the setting on. */
    private static boolean anyDimensionEnabled(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            if (HerobrineSettings.fog(level)) {
                return true;
            }
        }

        return false;
    }

    private static void load(MinecraftServer server) {
        READY.add(server);

        for (String command : LOAD) {
            run(server, command);
        }
    }

    private static void run(MinecraftServer server, String command) {
        try {
            server.getCommands().getDispatcher().execute(command, server.createCommandSourceStack());
        } catch (CommandSyntaxException exception) {
            // The bundled content is optional; if a function is missing or malformed, stay quiet.
        }
    }
}
