package dev.btstudio.herobrine.network;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import dev.btstudio.herobrine.event.HauntingEvents;
import dev.btstudio.herobrine.world.Feature;
import dev.btstudio.herobrine.world.HerobrineSettings;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Sends the two settings the client cannot work out for itself to whoever needs them.
 *
 * <p>The whole point is that nothing is sent twice: the last state each player was given is kept
 * and compared once a second, so a player standing still in a dimension nobody is changing gets no
 * packets at all, while a change of dimension, a rename or an operator flipping the fog switch is
 * picked up within that second. Joining a world sends it straight away, so nobody waits.
 */
public final class HerobrineNetwork {
    /** How often, in ticks, we look for players whose state has moved on. */
    private static final int SYNC_INTERVAL = 20;

    private static final Map<UUID, HerobrineStatePayload> LAST_SENT = new HashMap<>();

    private HerobrineNetwork() {
    }

    public static void register() {
        // The codec has to be registered on both sides before anything is sent or received, and
        // this class is loaded from the common initialiser, which runs on both.
        PayloadTypeRegistry.clientboundPlay()
                .register(HerobrineStatePayload.TYPE, HerobrineStatePayload.CODEC);

        ServerPlayConnectionEvents.JOIN.register((listener, sender, server) -> {
            HerobrineStatePayload payload = stateFor(listener.player);
            LAST_SENT.put(listener.player.getUUID(), payload);
            ServerPlayNetworking.send(listener.player, payload);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((listener, server) -> {
            LAST_SENT.remove(listener.player.getUUID());
            HauntingEvents.forgetMusic(listener.player.getUUID());
        });

        ServerTickEvents.END_SERVER_TICK.register(HerobrineNetwork::syncAll);
    }

    private static void syncAll(MinecraftServer server) {
        if (server.getTickCount() % SYNC_INTERVAL != 0) {
            return;
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            HerobrineStatePayload payload = stateFor(player);

            if (!payload.equals(LAST_SENT.get(player.getUUID()))) {
                LAST_SENT.put(player.getUUID(), payload);
                ServerPlayNetworking.send(player, payload);
            }
        }
    }

    /** The state of the dimension this player is standing in, right now. */
    private static HerobrineStatePayload stateFor(ServerPlayer player) {
        ServerLevel level = player.level();

        return new HerobrineStatePayload(
                HerobrineSettings.enabled(level, Feature.FOG_CLOSING_IN),
                HerobrineSettings.name(level));
    }
}
