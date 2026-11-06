package dev.btstudio.herobrine.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Puts the settings button on the mod's entry in Mod Menu. Mod Menu is a soft dependency: if it is
 * not installed nobody looks up this entrypoint, so the class is never loaded.
 */
public class HerobrineModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return HerobrineConfigScreen::new;
    }
}
