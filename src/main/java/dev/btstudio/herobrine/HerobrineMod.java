package dev.btstudio.herobrine;

import dev.btstudio.herobrine.command.HerobrineCommand;
import dev.btstudio.herobrine.entity.HerobrineEntity;
import dev.btstudio.herobrine.entity.ModEntities;
import dev.btstudio.herobrine.event.BedWatcher;
import dev.btstudio.herobrine.event.FromTheFog;
import dev.btstudio.herobrine.event.HauntingEvents;
import dev.btstudio.herobrine.event.HolyWater;
import dev.btstudio.herobrine.event.NotchAppleRitual;
import dev.btstudio.herobrine.item.ModItems;
import dev.btstudio.herobrine.sound.ModSounds;
import dev.btstudio.herobrine.event.ShrineHandler;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HerobrineMod implements ModInitializer {
    public static final String MOD_ID = "herobrine";

    /** How close Herobrine has to be before he stands outside your window and keeps you awake. */
    private static final double SLEEP_BLOCK_RADIUS = 48.0;
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        // Registers the entity type and its default attributes.
        ModEntities.register();
        // Iron stick, holy water and the cross (Herobrine: Forgotten).
        ModItems.register();

        HerobrineCommand.register();
        HauntingEvents.register();
        ShrineHandler.register();
        // Burning a Notch Apple on a netherrack fire banishes him - or calls him back.
        NotchAppleRitual.register();
        // Bottling holy water out of a church cauldron.
        HolyWater.register();
        // Bed sightings: he is already standing there when you get in.
        BedWatcher.register();
        // The mod's own sound set (assets/herobrine/sounds.json).
        ModSounds.register();
        // Bundled From The Fog content: ticks only where the per-dimension setting allows it.
        FromTheFog.register();

        // "No sleep": a bed does not work while he is standing nearby.
        EntitySleepEvents.ALLOW_SLEEPING.register((player, sleepingPos) ->
                player instanceof ServerPlayer serverPlayer && HerobrineEntity.isNear(serverPlayer, SLEEP_BLOCK_RADIUS)
                        ? Player.BedSleepingProblem.NOT_SAFE
                        : null);

        LOGGER.info("Herobrine is watching...");
    }
}
