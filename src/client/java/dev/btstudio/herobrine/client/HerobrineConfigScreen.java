package dev.btstudio.herobrine.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import dev.btstudio.herobrine.world.Feature;
import dev.btstudio.herobrine.world.HerobrineName;
import dev.btstudio.herobrine.world.HerobrineSettings;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * In-world settings, opened with Mod Menu, H or /herobrine_settings.
 *
 * <p>One tab per group of switches - the traces he leaves in the world, the ways he shows himself,
 * sound, the bigger pieces of the mod, and the name he goes by - each with an On/Off button that
 * sends the matching {@code /herobrine config} command. There is a switch for every part of the
 * haunting, so the world can be tuned right down to a single effect, and an <b>All on</b> /
 * <b>All off</b> pair for the two things most people actually want.
 *
 * <p>Changes are applied immediately through server commands. Multiplayer state is deliberately
 * shown as unknown: only the server owns it, and commands may be rejected without operator
 * permission.
 */
public class HerobrineConfigScreen extends Screen {
    private static final int CONTROL_HEIGHT = 20;
    private static final int REFRESH_TICKS = 20;
    private static final int VOLUME_DEBOUNCE = 8;
    /** Height of a setting row; tall enough for the icon, the title and its description. */
    private static final int ROW_HEIGHT = 38;
    private static final int BUTTON_WIDTH = 76;

    /**
     * A tabs-worth of switches. The last tab is not a group of switches at all: it holds the name
     * editor, which needs a text box rather than a row of buttons.
     */
    private enum Page {
        WORLD("world", Feature.Group.WORLD),
        APPARITIONS("apparitions", Feature.Group.APPARITIONS),
        SOUND("sound", Feature.Group.SOUND),
        PRESENCE("presence", Feature.Group.PRESENCE),
        NAME("name", null);

        private final String key;
        private final Feature.Group group;

        Page(String key, Feature.Group group) {
            this.key = key;
            this.group = group;
        }

        String tabKey() {
            return "herobrine.screen.tab." + this.key;
        }

        String sectionKey() {
            return "herobrine.screen.section." + this.key;
        }
    }

    /** One row of the panel: what it is called, what it does, and the command that flips it. */
    private record Entry(String titleKey, String descriptionKey, ItemStack icon, String command,
                        Predicate<HerobrineSettings> getter) { }

    private record Row(int y, String titleKey, String descriptionKey, ItemStack icon, Button button,
                       Predicate<HerobrineSettings> getter) { }

    private final Screen parent;
    private final List<Row> rows = new ArrayList<>();
    private Page page = Page.WORLD;
    private VolumeSlider volumeSlider;
    private EditBox nameBox;
    private String nameError;
    private int refreshIn = REFRESH_TICKS;
    private int volumeDebounce = -1;
    private int pendingVolume = -1;
    private int volumeSyncDelay;
    private int scroll;
    private int maxScroll;
    private int panelLeft;
    private int panelRight;
    private int panelBottom;
    private int rowTop;

    public HerobrineConfigScreen(Screen parent) {
        super(Component.translatable("herobrine.screen.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.rows.clear();
        this.volumeSlider = null;
        this.nameBox = null;
        this.nameError = null;
        int sidebarWidth = Math.min(110, Math.max(76, this.width / 4));
        this.panelLeft = sidebarWidth + 16;
        this.panelRight = this.width - 10;
        this.rowTop = 58;
        this.panelBottom = this.height - CONTROL_HEIGHT - 20;

        int tabY = this.rowTop;

        for (Page candidate : Page.values()) {
            Button tab = Button.builder(Component.translatable(candidate.tabKey()),
                            b -> this.changePage(candidate))
                    .pos(8, tabY).size(sidebarWidth, CONTROL_HEIGHT).build();
            tab.active = this.page != candidate;
            this.addRenderableWidget(tab);
            tabY += CONTROL_HEIGHT + 4;
        }

        if (this.page == Page.NAME) {
            this.initNamePage();
        } else {
            this.initRows();
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

        int x = this.panelRight;

        this.addRenderableWidget(Button.builder(Component.translatable("herobrine.screen.done"),
                        b -> this.onClose())
                .pos(x - BUTTON_WIDTH, bottomY).size(BUTTON_WIDTH, CONTROL_HEIGHT).build());
        x -= BUTTON_WIDTH + 8;
        this.addRenderableWidget(Button.builder(Component.translatable("herobrine.screen.all_off"),
                        b -> this.setAll(false))
                .pos(x - BUTTON_WIDTH, bottomY).size(BUTTON_WIDTH, CONTROL_HEIGHT).build());
        x -= BUTTON_WIDTH + 8;
        this.addRenderableWidget(Button.builder(Component.translatable("herobrine.screen.all_on"),
                        b -> this.setAll(true))
                .pos(x - BUTTON_WIDTH, bottomY).size(BUTTON_WIDTH, CONTROL_HEIGHT).build());
    }

    /** The switch rows for the open tab, with the volume slider filed under sound. */
    private void initRows() {
        List<Entry> entries = new ArrayList<>();

        if (this.page.group != null) {
            for (Feature feature : Feature.inGroup(this.page.group)) {
                entries.add(new Entry(feature.titleKey(), feature.descriptionKey(),
                        new ItemStack(feature.icon()), "herobrine config " + feature.key(),
                        settings -> settings.enabled(feature)));
            }
        }

        if (this.page == Page.SOUND) {
            // The music volume is not a switch, so it is a sentinel row with no command.
            entries.add(new Entry("herobrine.screen.musicvolume", "herobrine.screen.desc.volume",
                    new ItemStack(Items.NOTE_BLOCK), null, null));
        }

        if (this.page == Page.PRESENCE) {
            entries.add(new Entry("herobrine.screen.purist", "herobrine.screen.desc.purist",
                    new ItemStack(Items.ENDER_EYE), "herobrine mode toggle",
                    HerobrineSettings::isPurist));
            entries.add(new Entry("herobrine.screen.banished", "herobrine.screen.desc.banished",
                    new ItemStack(Items.BARRIER), "herobrine banish", HerobrineSettings::isBanished));
        }

        // How many rows fit decides the scroll: nothing is squeezed, and no empty card is left over.
        int visible = Math.max(1, (this.panelBottom - this.rowTop) / ROW_HEIGHT);
        this.maxScroll = Math.max(0, entries.size() - visible);
        this.scroll = Math.min(this.scroll, this.maxScroll);

        for (int index = this.scroll; index < Math.min(entries.size(), this.scroll + visible); index++) {
            this.addEntry(index - this.scroll, entries.get(index));
        }
    }

    private void addEntry(int index, Entry entry) {
        int y = this.rowTop + index * ROW_HEIGHT;

        if (entry.command() == null) {
            this.addVolume(y, entry);
            return;
        }

        int width = this.controlWidth();
        Button button = Button.builder(toggleLabel(currentSettings(), entry.getter()), pressed -> {
                    this.runCommand(entry.command());
                    this.refreshIn = 8;
                }).pos(this.panelRight - width - 8, y + (ROW_HEIGHT - CONTROL_HEIGHT) / 2)
                .size(width, CONTROL_HEIGHT).build();
        button.active = this.inWorld();
        this.addRenderableWidget(button);
        this.rows.add(new Row(y, entry.titleKey(), entry.descriptionKey(), entry.icon(), button,
                entry.getter()));
    }

    private void addVolume(int y, Entry entry) {
        int width = this.controlWidth();
        HerobrineSettings settings = currentSettings();
        int initial = this.pendingVolume >= 0 ? this.pendingVolume : settings != null ? settings.musicVolume() : 70;
        this.volumeSlider = new VolumeSlider(this.panelRight - width - 8,
                y + (ROW_HEIGHT - CONTROL_HEIGHT) / 2, width, initial, settings != null);
        this.volumeSlider.active = this.inWorld();
        this.addRenderableWidget(this.volumeSlider);
        this.rows.add(new Row(y, entry.titleKey(), entry.descriptionKey(), entry.icon(), null, null));
    }

    /** The name editor: a text box, the rule it has to follow, and a way back to the default. */
    private void initNamePage() {
        int left = this.panelLeft + 10;
        int width = Math.min(220, this.panelRight - this.panelLeft - 20);
        int y = this.rowTop + 12;
        Font font = this.minecraft == null ? Minecraft.getInstance().font : this.font;
        this.nameBox = new EditBox(font, left, y, width, CONTROL_HEIGHT,
                Component.translatable("herobrine.screen.name"));
        this.nameBox.setMaxLength(HerobrineName.MAX_LENGTH);
        this.nameBox.setHint(Component.translatable("herobrine.name.default"));
        this.nameBox.setValue(HerobrineName.DEFAULT);
        this.nameBox.active = this.inWorld();
        this.nameBox.setCanLoseFocus(true);
        this.addRenderableWidget(this.nameBox);

        Button reset = Button.builder(Component.translatable("herobrine.screen.name.reset"), b -> {
                    this.nameBox.setValue(Component.translatable("herobrine.name.default").getString());
                    this.applyName();
                }).pos(left, y + CONTROL_HEIGHT + 8)
                .size(Math.min(140, width), CONTROL_HEIGHT).build();
        reset.active = this.inWorld();
        this.addRenderableWidget(reset);

        // The stored name is only known in singleplayer, so the box starts from the default and is
        // corrected the moment the world is readable.
        HerobrineSettings settings = currentSettings();

        if (settings != null) {
            this.nameBox.setValue(settings.name());
        }
    }

    /** Every switch at once, from the two buttons on the bottom row. */
    private void setAll(boolean on) {
        this.runCommand(on ? "herobrine config all on" : "herobrine config all off");
        this.refreshIn = 8;
    }

    private void changePage(Page next) {
        if (this.page != next) {
            this.flushName();
            this.page = next;
            this.scroll = 0;
            this.rebuildWidgets();
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.page == Page.NAME && this.nameBox != null && this.nameBox.isFocused()
                && (event.key() == InputConstants.KEY_RETURN
                || event.key() == InputConstants.KEY_NUMPADENTER)) {
            this.applyName();
            return true;
        }

        return super.keyPressed(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.page != Page.NAME && this.maxScroll > 0 && scrollY != 0.0) {
            int next = Math.max(0, Math.min(this.maxScroll, this.scroll + (scrollY > 0 ? -1 : 1)));

            if (next != this.scroll) {
                this.scroll = next;
                this.rebuildWidgets();
            }
        }

        return true;
    }

    /**
     * Sends the name if it is one he can go by. A name that will not do is left in the box with a
     * single line under it explaining why, rather than silently changed into something else.
     */
    private void applyName() {
        if (this.nameBox == null || !this.inWorld()) {
            return;
        }

        String typed = this.nameBox.getValue().trim();

        if (!HerobrineName.isValid(typed)) {
            this.nameError = "herobrine.screen.name.invalid";
            return;
        }

        this.nameError = null;
        this.runCommand("herobrine config name " + typed);
        this.refreshIn = 8;
    }

    /** Writes the box back to the world on the way out, so nothing typed is quietly dropped. */
    private void flushName() {
        if (this.nameBox != null && this.nameBox.isFocused()) {
            this.nameBox.setFocused(false);
            this.applyName();
        }
    }

    private int controlWidth() {
        return Math.min(108, Math.max(64, (this.panelRight - this.panelLeft) / 3));
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
        int visible = Math.max(1, (this.panelBottom - this.rowTop) / ROW_HEIGHT);

        for (int i = 0; i < visible; i++) {
            int y = this.rowTop + i * ROW_HEIGHT;
            graphics.fill(this.panelLeft + 4, y, this.panelRight - 4, y + ROW_HEIGHT - 2, 0xC00C0E12);
        }

        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.centeredText(this.font, this.title, this.width / 2, 9, 0xFFFFFFFF);
        String subtitle = !this.inWorld() ? "herobrine.screen.no_world"
                : currentSettings() == null ? "herobrine.screen.multiplayer" : "herobrine.screen.subtitle";
        graphics.centeredText(this.font, Component.translatable(subtitle), this.width / 2, 24, 0xFFAAAAAA);
        graphics.text(this.font, Component.translatable(this.page.sectionKey()),
                this.panelLeft + 10, 43, 0xFF9AABF0);
        int maxTextWidth = this.panelRight - this.panelLeft - this.controlWidth() - 48;

        for (Row row : this.rows) {
            graphics.item(row.icon(), this.panelLeft + 8, row.y() + (ROW_HEIGHT - 16) / 2);
            String title = Component.translatable(row.titleKey()).getString();
            graphics.text(this.font, this.font.plainSubstrByWidth(title, Math.max(0, maxTextWidth)),
                    this.panelLeft + 30, row.y() + 7, 0xFFFFFFFF);
            String description = Component.translatable(row.descriptionKey()).getString();
            graphics.text(this.font, this.font.plainSubstrByWidth(description, Math.max(0, maxTextWidth)),
                    this.panelLeft + 30, row.y() + 19, 0xFFAAAAAA);
        }

        if (this.page == Page.NAME) {
            this.renderNamePage(graphics, maxTextWidth);
        } else if (this.maxScroll > 0) {
            this.renderScrollbar(graphics);
        }
    }

    /** The rules under the name box, and the one line of complaint when a name will not do. */
    private void renderNamePage(GuiGraphicsExtractor graphics, int maxTextWidth) {
        int left = this.panelLeft + 30;
        int y = this.rowTop + CONTROL_HEIGHT + 34;
        String intro = Component.translatable("herobrine.screen.desc.name").getString();
        graphics.text(this.font, this.font.plainSubstrByWidth(intro, Math.max(0, maxTextWidth)),
                this.panelLeft + 10, y, 0xFFAAAAAA);

        if (this.nameError != null) {
            graphics.text(this.font, Component.translatable(this.nameError, HerobrineName.MAX_LENGTH),
                    this.panelLeft + 10, y + 14, 0xFFFF5555);
        }
    }

    /** A plain track and thumb on the right edge of the panel, so scrolling is not invisible. */
    private void renderScrollbar(GuiGraphicsExtractor graphics) {
        int trackTop = this.rowTop;
        int trackBottom = this.panelBottom;
        int trackX = this.panelRight - 5;
        graphics.fill(trackX, trackTop, trackX + 3, trackBottom, 0xCC000000);

        int span = trackBottom - trackTop;
        int thumb = Math.max(12, span / (this.maxScroll + 1));
        int travel = span - thumb;
        int top = trackTop + (this.maxScroll == 0 ? 0 : travel * this.scroll / this.maxScroll);
        graphics.fill(trackX, top, trackX + 3, top + thumb, 0xFF8A8A8A);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        this.flushVolume();
        this.flushName();
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

    /** The settings of the singleplayer world we are in, or null on a server. */
    private static HerobrineSettings currentSettings() {
        return HerobrineClientState.singleplayerSettings();
    }

    private static Component toggleLabel(HerobrineSettings settings, Predicate<HerobrineSettings> getter) {
        if (settings == null) {
            return Component.translatable("herobrine.screen.toggle");
        }
        boolean enabled = getter.test(settings);
        return Component.translatable(enabled ? "herobrine.screen.on" : "herobrine.screen.off")
                .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED);
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
