package dev.btstudio.herobrine.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import dev.btstudio.herobrine.world.HerobrineSettings;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * The settings screen, reachable from Mod Menu.
 *
 * <p>Every switch maps to an existing command, so the screen never keeps its own copy of the state:
 * pressing a button runs {@code /herobrine ...} and the world's saved settings do the rest. In
 * singleplayer the labels can show the current value; on a server the client has no business
 * reading the world file, so the labels stay neutral and the chat confirmation reports the state.</p>
 */
public class HerobrineConfigScreen extends Screen {
    private static final int ROW_WIDTH = 220;
    private static final int ROW_HEIGHT = 20;
    private static final int ROW_GAP = 4;
    private static final int REFRESH_TICKS = 8;

    private final Screen parent;
    private final List<Row> rows = new ArrayList<>();
    private Button volumeButton;
    private int refreshIn = -1;

    /** One button, the translation key it reads, and (optionally) how to read its value. */
    private record Row(Button button, String labelKey, Predicate<HerobrineSettings> getter) {
    }

    public HerobrineConfigScreen(Screen parent) {
        super(Component.translatable("herobrine.screen.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        HerobrineSettings settings = currentSettings();
        int x = this.width / 2 - ROW_WIDTH / 2;
        int y = 54;

        y = this.addToggle(x, y, "herobrine.screen.herd", "herobrine config herd",
                HerobrineSettings::isHerd);
        y = this.addToggle(x, y, "herobrine.screen.miner", "herobrine config miner",
                HerobrineSettings::isMiner);
        y = this.addToggle(x, y, "herobrine.screen.arson", "herobrine config arson",
                HerobrineSettings::isArson);
        y = this.addToggle(x, y, "herobrine.screen.schizo", "herobrine schizo",
                HerobrineSettings::isSchizo);

        y += ROW_GAP;

        y = this.addToggle(x, y, "herobrine.screen.music", "herobrine config music",
                HerobrineSettings::isMusic);
        this.volumeButton = Button.builder(volumeLabel(currentSettings()), pressed -> {
                    runCommand("herobrine config musicvolume");
                    this.refreshIn = REFRESH_TICKS;
                })
                .pos(x, y)
                .size(ROW_WIDTH, ROW_HEIGHT)
                .build();
        this.addRenderableWidget(this.volumeButton);
        y += ROW_HEIGHT + ROW_GAP + ROW_GAP;

        y = this.addToggle(x, y, "herobrine.screen.purist", "herobrine mode purist", null);
        y = this.addToggle(x, y, "herobrine.screen.banished", "herobrine banish", null);

        this.addRenderableWidget(Button.builder(Component.translatable("herobrine.screen.done"),
                        button -> this.onClose())
                .pos(this.width / 2 - ROW_WIDTH / 2, y + 14)
                .size(ROW_WIDTH, ROW_HEIGHT)
                .build());
    }

    private int addToggle(int x, int y, String labelKey, String command,
                          Predicate<HerobrineSettings> getter) {
        Button button = Button.builder(label(labelKey, currentSettings(), getter), pressed -> {
                    runCommand(command);
                    this.refreshIn = REFRESH_TICKS;
                })
                .pos(x, y)
                .size(ROW_WIDTH, ROW_HEIGHT)
                .build();

        this.rows.add(new Row(button, labelKey, getter));
        this.addRenderableWidget(button);
        return y + ROW_HEIGHT + ROW_GAP;
    }

    /** "Music volume: 70%" - the one switch that has a value rather than a state. */
    private static Component volumeLabel(HerobrineSettings settings) {
        Component name = Component.translatable("herobrine.screen.musicvolume");

        if (settings == null) {
            return name;
        }

        return Component.empty().append(name).append(": ")
                .append(Component.literal(Integer.toString(settings.musicVolume())));
    }

    private static Component label(String labelKey, HerobrineSettings settings,
                                   Predicate<HerobrineSettings> getter) {
        Component name = Component.translatable(labelKey);

        if (settings == null || getter == null) {
            return name;
        }

        String stateKey = getter.test(settings) ? "herobrine.screen.on" : "herobrine.screen.off";
        return Component.empty().append(name).append(": ").append(Component.translatable(stateKey));
    }

    @Override
    public void tick() {
        if (this.refreshIn > 0 && --this.refreshIn == 0) {
            // The command lands a tick or two later, so re-read the labels once it has.
            HerobrineSettings settings = currentSettings();

            for (Row row : this.rows) {
                row.button().setMessage(label(row.labelKey(), settings, row.getter()));
            }

            if (this.volumeButton != null) {
                this.volumeButton.setMessage(volumeLabel(settings));
            }
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                   float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.centeredText(this.font, this.title, this.width / 2, 22, 0xFFFFFFFF);

        Component subtitle = currentSettings() == null
                ? Component.translatable("herobrine.screen.multiplayer")
                : Component.translatable("herobrine.screen.subtitle");
        graphics.centeredText(this.font, subtitle, this.width / 2, 36, 0xFF9A9A9A);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            // 26.x routes screen changes through the gui manager rather than Minecraft itself.
            this.minecraft.gui.setScreen(this.parent);
        }
    }

    private void runCommand(String command) {
        if (this.minecraft == null || this.minecraft.player == null) {
            return;
        }

        var connection = this.minecraft.player.connection;

        if (connection != null) {
            connection.sendCommand(command);
        }
    }

    /** The world's saved settings, but only in singleplayer - on a server they are not ours to read. */
    private static HerobrineSettings currentSettings() {
        Minecraft minecraft = Minecraft.getInstance();
        var server = minecraft.getSingleplayerServer();

        if (server == null) {
            return null;
        }

        try {
            var level = server.overworld();
            return level == null ? null : HerobrineSettings.of(level);
        } catch (RuntimeException exception) {
            return null;
        }
    }
}
