package dev.btstudio.herobrine.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * The fog that shader packs cannot switch off.
 *
 * <p>{@code FogRendererMixin} thickens the fog by writing into the {@code FogData} vanilla builds,
 * which works everywhere except where it matters most to some players: a shader pack replaces the
 * fog pipeline completely, so those numbers are quietly thrown away and he can stand ten blocks
 * away in clear air.
 *
 * <p>So when the mixin reports that it never ran, the same closeness is drawn here instead - in the
 * HUD layer, in screen space, as a vignette that darkens and narrows towards the edges of the view.
 * No shader pack touches HUD rendering, so this one always gets through. It is drawn as a handful of
 * concentric bands rather than a flat wash, because a flat wash reads as "night fell" while a
 * vignette reads as "the fog is closing in".
 *
 * <p>Nothing changes for anyone not running shaders: the moment the mixin does its job, this draws
 * nothing at all.
 */
public final class HerobrineFogOverlay {
    /** How many bands the vignette is built from: more bands, smoother falloff, more draw calls. */
    private static final int BANDS = 4;

    /** How dark the outermost band gets when he is standing right on top of you. */
    private static final float MAX_ALPHA = 0.45F;

    private HerobrineFogOverlay() {
    }

    /** Registered before the vanilla misc overlays so it sits under the pumpkin and portal effects. */
    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();

        // The vanilla fog path did its job this frame - nothing to add.
        if (HerobrineFog.consumeApplied()) {
            return;
        }

        float closeness = HerobrineFog.closeness(minecraft.getCameraEntity());

        if (closeness <= 0.0F) {
            return;
        }

        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        int thickness = Math.max(2, Math.min(width, height) / (BANDS * 5));

        for (int band = 0; band < BANDS; band++) {
            int alpha = Math.round(closeness * MAX_ALPHA * 255.0F * (BANDS - band) / BANDS);

            if (alpha <= 0) {
                continue;
            }

            // One ring, drawn as four rectangles so that every pixel is covered exactly once and
            // the corners do not end up darker than the edges.
            int inset = band * thickness;
            int left = inset;
            int top = inset;
            int right = width - inset;
            int bottom = height - inset;
            int colour = alpha << 24;

            graphics.fill(left, top, right, top + thickness, colour);
            graphics.fill(left, bottom - thickness, right, bottom, colour);
            graphics.fill(left, top + thickness, left + thickness, bottom - thickness, colour);
            graphics.fill(right - thickness, top + thickness, right, bottom - thickness, colour);
        }
    }
}
