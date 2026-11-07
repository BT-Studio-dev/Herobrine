package dev.btstudio.herobrine.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import dev.btstudio.herobrine.world.HerobrineSettings;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * In-world settings, opened with Mod Menu, H or /herobrine_settings.
 * Changes are applied immediately through server commands. Multiplayer state is deliberately shown
 * as unknown: only the server owns it, and commands may be rejected without operator permission.
 */
public class HerobrineConfigScreen extends Screen {
    private static final int CONTROL_HEIGHT = 20;
    private static final int REFRESH_TICKS = 20;
    private static final int VOLUME_DEBOUNCE = 8;

    private enum Page { HAUNTING, AUDIO_WORLD }

    private record Row(int y, String titleKey, String descriptionKey, ItemStack icon, Button button,
                       Predicate<HerobrineSettings> getter) { }

    private final Screen parent;
    private final List<Row> rows = new ArrayList<>();
    private Page page = Page.HAUNTING;
    private VolumeSlider volumeSlider;
    private int refreshIn = REFRESH_TICKS;
    private int volumeDebounce = -1;
    private int pendingVolume = -1;
    private int volumeSyncDelay;
    private int panelLeft;
    private int panelRight;
    private int rowTop;
    private int rowHeight;

    public HerobrineConfigScreen(Screen parent) {
        super(Component.translatable("herobrine.screen.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.rows.clear();
        this.volumeSlider = null;
        int sidebarWidth = Math.min(110, Math.max(76, this.width / 4));
        this.panelLeft = sidebarWidth + 16;
        this.panelRight = this.width - 10;
        this.rowTop = 58;
        this.rowHeight = Math.max(23, Math.min(38, (this.height - 100) / 4));

        this.addRenderableWidget(Button.builder(Component.translatable("herobrine.screen.tab.haunting"),
                        b -> this.changePage(Page.HAUNTING))
                .pos(8, 58).size(sidebarWidth, CONTROL_HEIGHT).build()).active = this.page != Page.HAUNTING;
        this.addRenderableWidget(Button.builder(Component.translatable("herobrine.screen.tab.audio"),
                        b -> this.changePage(Page.AUDIO_WORLD))
                .pos(8, 82).size(sidebarWidth, CONTROL_HEIGHT).build()).active = this.page != Page.AUDIO_WORLD;

        if (this.page == Page.HAUNTING) {
            this.addToggle(0, "herobrine.screen.herd", "herobrine.screen.desc.herd",
                    "herobrine config herd", HerobrineSettings::isHerd, new ItemStack(Items.COW_SPAWN_EGG));
            this.addToggle(1, "herobrine.screen.miner", "herobrine.screen.desc.miner",
                    "herobrine config miner", HerobrineSettings::isMiner, new ItemStack(Items.IRON_PICKAXE));
            this.addToggle(2, "herobrine.screen.arson", "herobrine.screen.desc.arson",
                    "herobrine config arson", HerobrineSettings::isArson, new ItemStack(Items.FLINT_AND_STEEL));
            this.addToggle(3, "herobrine.screen.schizo", "herobrine.screen.desc.schizo",
                    "herobrine schizo", HerobrineSettings::isSchizo, new ItemStack(Items.SOUL_LANTERN));
        } else {
            this.addToggle(0, "herobrine.screen.music", "herobrine.screen.desc.music",
                    "herobrine config music", HerobrineSettings::isMusic, new ItemStack(Items.MUSIC_DISC_13));
            this.addVolume(1);
            this.addToggle(2, "herobrine.screen.purist", "herobrine.screen.desc.purist",
                    "herobrine mode toggle", HerobrineSettings::isPurist, new ItemStack(Items.ENDER_EYE));
            this.addToggle(3, "herobrine.screen.banished", "herobrine.screen.desc.banished",
                    "herobrine banish", HerobrineSettings::isBanished, new ItemStack(Items.BARRIER));
        }

        int bottomY = this.height - CONTROL_HEIGHT - 8;
        Button reset = Button.builder(Component.translatable("herobrine.screen.reset"), b -> {
                    this.runCommand("herobrine config reset");
                    this.pendingVolume = -1;
                    this.volumeDebounce = -1;
                    this.volumeSyncDelay = 30;
                    this.refreshIn = 8;
                })
                .pos(8, bottomY).size(sidebarWidth, CONTROL_HEIGHT).build();
        reset.active = this.inWorld();
        this.addRenderableWidget(reset);
        this.addRenderableWidget(Button.builder(Component.translatable("herobrine.screen.done"),
                        b -> this.onClose())
                .pos(this.panelRight - 92, bottomY).size(92, CONTROL_HEIGHT).build());
    }

    private void changePage(Page next) {
        if (this.page != next) {
            this.page = next;
            this.rebuildWidgets();
        }
    }

    private int controlWidth() {
        return Math.min(108, Math.max(64, (this.panelRight - this.panelLeft) / 3));
    }

    private void addToggle(int index, String titleKey, String descriptionKey, String command,
                           Predicate<HerobrineSettings> getter, ItemStack icon) {
        int y = this.rowTop + index * this.rowHeight;
        int width = this.controlWidth();
        Button button = Button.builder(toggleLabel(currentSettings(), getter), pressed -> {
                    this.runCommand(command);
                    this.refreshIn = 8;
                }).pos(this.panelRight - width - 8, y + (this.rowHeight - CONTROL_HEIGHT) / 2)
                .size(width, CONTROL_HEIGHT).build();
        button.active = this.inWorld();
        this.addRenderableWidget(button);
        this.rows.add(new Row(y, titleKey, descriptionKey, icon, button, getter));
    }

    private void addVolume(int index) {
        int y = this.rowTop + index * this.rowHeight;
        int width = this.controlWidth();
        HerobrineSettings settings = currentSettings();
        int initial = this.pendingVolume >= 0 ? this.pendingVolume : settings != null ? settings.musicVolume() : 70;
        this.volumeSlider = new VolumeSlider(this.panelRight - width - 8,
                y + (this.rowHeight - CONTROL_HEIGHT) / 2, width, initial, settings != null);
        this.volumeSlider.active = this.inWorld();
        this.addRenderableWidget(this.volumeSlider);
        this.rows.add(new Row(y, "herobrine.screen.musicvolume", "herobrine.screen.desc.volume", new ItemStack(Items.NOTE_BLOCK), null, null));
    }

    private static Component toggleLabel(HerobrineSettings settings, Predicate<HerobrineSettings> getter) {
        if (settings == null) {
            return Component.translatable("herobrine.screen.toggle");
        }
        boolean enabled = getter.test(settings);
        return Component.translatable(enabled ? "herobrine.screen.on" : "herobrine.screen.off")
                .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.volumeSyncDelay > 0) {
            this.volumeSyncDelay--;
        }
        if (this.volumeDebounce > 0 && --this.volumeDebounce == 0) {
            this.flushVolume();
        }
        if (--this.refreshIn <= 0) {
            this.refreshIn = REFRESH_TICKS;
            HerobrineSettings settings = currentSettings();
            for (Row row : this.rows) {
                if (row.button() != null) {
                    row.button().setMessage(toggleLabel(settings, row.getter()));
                }
            }
            if (this.volumeSlider != null && settings != null && this.pendingVolume < 0
                    && this.volumeSyncDelay == 0) {
                this.volumeSlider.sync(settings.musicVolume());
            }
        }
    }

    private void flushVolume() {
        if (this.pendingVolume >= 0) {
            this.runCommand("herobrine config musicvolume " + this.pendingVolume);
            this.pendingVolume = -1;
            this.volumeSyncDelay = 30;
        }
        this.volumeDebounce = -1;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        // Dark card layout inspired by Minecraft's settings panels; widgets stay native and accessible.
        graphics.fill(4, 38, this.panelLeft - 5, this.height - 32, 0xD5101115);
        graphics.fill(this.panelLeft, 38, this.panelRight, this.height - 32, 0xE0181A20);
        graphics.outline(this.panelLeft, 38, this.panelRight - this.panelLeft, this.height - 70, 0xFF454955);
        for (int i = 0; i < 4; i++) {
            int y = this.rowTop + i * this.rowHeight;
            graphics.fill(this.panelLeft + 4, y, this.panelRight - 4, y + this.rowHeight - 2, 0xC00C0E12);
        }
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.centeredText(this.font, this.title, this.width / 2, 9, 0xFFFFFFFF);
        String subtitle = !this.inWorld() ? "herobrine.screen.no_world"
                : currentSettings() == null ? "herobrine.screen.multiplayer" : "herobrine.screen.subtitle";
        graphics.centeredText(this.font, Component.translatable(subtitle), this.width / 2, 24, 0xFFAAAAAA);
        graphics.text(this.font, Component.translatable(this.page == Page.HAUNTING
                ? "herobrine.screen.section.haunting" : "herobrine.screen.section.sound"),
                this.panelLeft + 10, 43, 0xFF9AABF0);
        int maxTextWidth = this.panelRight - this.panelLeft - this.controlWidth() - 48;
        for (Row row : this.rows) {
            graphics.item(row.icon(), this.panelLeft + 8, row.y() + (this.rowHeight - 16) / 2);
            String title = Component.translatable(row.titleKey()).getString();
            graphics.text(this.font, this.font.plainSubstrByWidth(title, Math.max(0, maxTextWidth)),
                    this.panelLeft + 30, row.y() + (this.rowHeight >= 32 ? 5 : 7), 0xFFFFFFFF);
            if (this.rowHeight >= 32) {
                String description = Component.translatable(row.descriptionKey()).getString();
                graphics.text(this.font, this.font.plainSubstrByWidth(description, Math.max(0, maxTextWidth)),
                        this.panelLeft + 30, row.y() + 18, 0xFFAAAAAA);
            }
        }
    }

    /** The server must continue ticking to process commands while the screen is open. */
    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        this.flushVolume();
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(this.parent);
        }
    }

    private void runCommand(String command) {
        if (this.inWorld() && this.minecraft.player.connection != null) {
            this.minecraft.player.connection.sendCommand(command);
        }
    }

    private boolean inWorld() {
        return this.minecraft != null && this.minecraft.player != null && this.minecraft.level != null;
    }

    private static HerobrineSettings currentSettings() {
        Minecraft minecraft = Minecraft.getInstance();
        var server = minecraft.getSingleplayerServer();
        if (server == null || minecraft.level == null) {
            return null;
        }
        try {
            var level = server.getLevel(minecraft.level.dimension());
            return level == null ? null : HerobrineSettings.of(level);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private class VolumeSlider extends AbstractSliderButton {
        private boolean known;

        VolumeSlider(int x, int y, int width, int initial, boolean known) {
            super(x, y, width, CONTROL_HEIGHT, Component.empty(), initial / 100.0);
            this.known = known;
            this.updateMessage();
        }

        @Override
        protected void updateMessage() {
            String valueText = this.known || HerobrineConfigScreen.this.pendingVolume >= 0
                    ? Math.round(this.value * 100) + "%" : "?";
            this.setMessage(Component.translatable("herobrine.screen.volume.value", valueText));
        }

        @Override
        protected void applyValue() {
            HerobrineConfigScreen.this.pendingVolume = (int) Math.round(this.value * 100);
            HerobrineConfigScreen.this.volumeDebounce = VOLUME_DEBOUNCE;
            HerobrineConfigScreen.this.volumeSyncDelay = 30;
        }

        void sync(int volume) {
            this.value = Math.max(0, Math.min(100, volume)) / 100.0;
            this.known = true;
            this.updateMessage();
        }
    }
}
