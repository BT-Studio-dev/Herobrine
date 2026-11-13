package dev.btstudio.herobrine.client;

import dev.btstudio.herobrine.network.HerobrineStatePayload;
import dev.btstudio.herobrine.world.Feature;
import dev.btstudio.herobrine.world.HerobrineSettings;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerLevel;

/**
 * What the client knows about the dimension it is in.
 *
 * <p>On a server the only way to know is to be told, so the values arrive in
 * {@link HerobrineStatePayload} once a second at most - and until the first one lands the client
 * assumes the defaults, which is why nothing flickers on joining.
 *
 * <p>In singleplayer the server is sitting in the same process, so there is no reason to wait for a
 * packet: both values are read straight off the world. That is also what the settings screen does,
 * and both go through {@link #singleplayerSettings()} to keep it in one place.
 */
public final class HerobrineClientState {
    private static HerobrineStatePayload received = HerobrineStatePayload.fallback();

    private HerobrineClientState() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(HerobrineStatePayload.TYPE,
                (payload, context) -> received = payload);
    }

    /**
     * The settings of the dimension the client is in, or null on a server (where only the server
     * owns them) and outside a world.
     */
    public static HerobrineSettings singleplayerSettings() {
        Minecraft minecraft = Minecraft.getInstance();
        var server = minecraft.getSingleplayerServer();

        if (server == null || minecraft.level == null) {
            return null;
        }

        try {
            ServerLevel level = server.getLevel(minecraft.level.dimension());
            return level == null ? null : HerobrineSettings.of(level);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    /** Whether the fog is allowed to close in as he gets closer. */
    public static boolean fogClosesIn() {
        HerobrineSettings settings = singleplayerSettings();
        return settings != null ? settings.enabled(Feature.FOG_CLOSING_IN) : received.fogClosesIn();
    }

    /** The name he goes by in this world. */
    public static String name() {
        HerobrineSettings settings = singleplayerSettings();
        return settings != null ? settings.name() : received.name();
    }
}
