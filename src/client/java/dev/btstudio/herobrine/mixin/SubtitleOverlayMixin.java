package dev.btstudio.herobrine.mixin;

import dev.btstudio.herobrine.client.HerobrineClientState;
import dev.btstudio.herobrine.world.HerobrineName;

import net.minecraft.client.gui.components.SubtitleOverlay;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Puts his name into the one subtitle that carries it.
 *
 * <p>Subtitles are looked up from a lang key built out of the sound's id, so there is nowhere to
 * pass an argument in: {@code subtitles.herobrine.jumpscare} is a fixed string in a language file.
 * This rewrites that component on its way into the overlay, and only that one - every other
 * subtitle is handed back untouched.
 *
 * <p>The injection is deliberately allowed to fail ({@code require = 0}). It is a nicety on top of
 * a lang entry that still reads correctly without it, so a Minecraft update that moves the code
 * around should cost a renamed subtitle, not a client that will not start.
 */
@Mixin(SubtitleOverlay.class)
public class SubtitleOverlayMixin {
    private static final String JUMPSCARE_KEY = "subtitles.herobrine.jumpscare";

    @ModifyVariable(method = "onPlaySound", at = @At("STORE"), ordinal = 0, require = 0)
    private Component herobrine$nameTheJumpscare(Component text) {
        if (text == null || HerobrineName.DEFAULT.equals(HerobrineClientState.name())) {
            return text;
        }

        if (text.getContents() instanceof TranslatableContents translatable
                && JUMPSCARE_KEY.equals(translatable.getKey())) {
            return HerobrineName.component(HerobrineClientState.name());
        }

        return text;
    }
}
