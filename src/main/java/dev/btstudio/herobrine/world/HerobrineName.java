package dev.btstudio.herobrine.world;

import java.util.regex.Pattern;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * The name Herobrine goes by when he pretends to be a player.
 *
 * <p>It is one name per dimension, stored with the settings and owned by the server: any operator
 * may change it, every player may read it with {@code /herobrine settings}, and it is used wherever
 * he turns up as a person - the fake <i>joined the game</i> and <i>left the game</i> lines, his one
 * line of chat, his nametag, the subtitle on the jumpscare, his death messages and the signs he
 * leaves behind.
 *
 * <p>The rules are the same ones Minecraft applies to a player's own name: letters, digits and
 * underscores, at most 16 characters. That keeps the fakes believable - a name that could not have
 * been a player's is a name that gives the joke away.
 */
public final class HerobrineName {
    /** The name he has always gone by, and the one {@code /herobrine config name reset} restores. */
    public static final String DEFAULT = "Herobrine";

    /** Minecraft's own limit for a player name. */
    public static final int MAX_LENGTH = 16;

    private static final Pattern VALID = Pattern.compile("[A-Za-z0-9_]{1," + MAX_LENGTH + '}');

    private HerobrineName() {
    }

    /** True when the name may be used: not empty, not too long, nothing but letters, digits and _. */
    public static boolean isValid(String name) {
        return name != null && VALID.matcher(name).matches();
    }

    /**
     * The stored form of a name: itself when it is usable, otherwise the default. A hand-edited
     * world file can therefore never put an empty or badly shaped name into the world.
     */
    public static String sanitize(String name) {
        return isValid(name) ? name : DEFAULT;
    }

    /** His name as a component, ready to drop into a message or a nametag. */
    public static MutableComponent component(String name) {
        return Component.literal(sanitize(name));
    }
}
