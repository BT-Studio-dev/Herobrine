package dev.btstudio.herobrine.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.btstudio.herobrine.HerobrineMod;
import dev.btstudio.herobrine.entity.ModEntities;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderers;

public class HerobrineClient implements ClientModInitializer {
    /**
     * The settings screen has to be reachable without Mod Menu - that mod is a convenience, not a
     * requirement, and a screen that only exists when another mod is installed is a screen half the
     * players never find. A rebindable key is always present.
     */
    private static final KeyMapping OPEN_SETTINGS = KeyMappingHelper.registerKeyMapping(
            new KeyMapping("key.herobrine.settings", InputConstants.Type.KEYBOARD,
                    InputConstants.KEY_H, KeyMapping.Category.MISC));

    @Override
    public void onInitializeClient() {
        // EntityRenderers#register is widened for mods by Fabric API's transitive access wideners.
        EntityRenderers.register(ModEntities.HEROBRINE, HerobrineRenderer::new);
        EntityRenderers.register(ModEntities.STEVE_FRIEND, SteveRenderer::new);
        EntityRenderers.register(ModEntities.ALEX_FRIEND, AlexRenderer::new);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN_SETTINGS.consumeClick()) {
                openSettings(client);
            }
        });

        // The fog switch and his name, sent by the server for the dimension we are standing in.
        HerobrineClientState.register();

        // The fog, drawn in screen space for as long as a shader pack is refusing to draw it.
        HudElementRegistry.attachElementBefore(VanillaHudElements.MISC_OVERLAYS,
                HerobrineMod.id("closing_fog"), HerobrineFogOverlay::render);

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> { });
    }

    private static void openSettings(Minecraft client) {
        if (client == null || client.gui.screen() != null) {
            return;
        }

        client.gui.setScreen(new HerobrineConfigScreen(null));
    }
}
