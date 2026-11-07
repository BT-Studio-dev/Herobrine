package dev.btstudio.herobrine.client;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.btstudio.herobrine.HerobrineMod;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/**
 * Glowing eyes. {@link RenderTypes#eyes(Identifier)} is the unlit render type vanilla uses for a
 * spider's eyes, so these pixels are drawn at full brightness no matter how dark the world is: in a
 * pitch black cave or at 3am the only thing you see is two burning eyes coming at you.
 *
 * <p>The layer submits the model itself rather than calling into {@code EyesLayer}: in 26.x a layer
 * has to hand the model to the node collector explicitly. Four layers, one per form, each only
 * submitting while the render state matches. The emissive layer pulses gently without changing the
 * underlying skin or the eye color for each form.
 */
public class HerobrineEyesLayer extends RenderLayer<HerobrineRenderState,
        HumanoidModel<HerobrineRenderState>> {
    private static final Identifier EYES_STALKER = HerobrineMod.id("textures/entity/herobrine_eyes.png");
    private static final Identifier EYES_HOSTILE = HerobrineMod.id("textures/entity/herobrine_hostile_eyes.png");
    private static final Identifier EYES_ELITE = HerobrineMod.id("textures/entity/herobrine_elite_eyes.png");
    private static final Identifier EYES_INFERNO = HerobrineMod.id("textures/entity/herobrine_inferno_eyes.png");

    private final Form form;
    private final RenderType renderType;

    public HerobrineEyesLayer(RenderLayerParent<HerobrineRenderState, HumanoidModel<HerobrineRenderState>> parent,
                              Form form) {
        super(parent);
        this.form = form;
        this.renderType = RenderTypes.eyes(switch (form) {
            case STALKER -> EYES_STALKER;
            case HOSTILE -> EYES_HOSTILE;
            case ELITE -> EYES_ELITE;
            case INFERNO -> EYES_INFERNO;
        });
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords,
                       HerobrineRenderState state, float yRot, float xRot) {
        if (!this.form.matches(state)) {
            return;
        }

        // Only the head is submitted. The eyes live in a four pixel strip of a 64x64 skin, so
        // drawing the whole body with that texture is asking for those pixels to land somewhere
        // else; the head part carries exactly the UVs the strip was painted for.
        // ageInTicks includes the partial tick, making this smooth between game ticks. Tint the
        // emissive pixels rather than changing alpha, so the transparent rest of the skin remains
        // transparent and white, red and orange eyes retain their own colors. The cycle is three
        // seconds at 20 ticks per second and never goes completely dark.
        double phase = (state.ageInTicks + state.eyeGlowPhase) * (2.0 * Math.PI / 60.0);
        int brightness = (int) Math.round(255.0 * (0.7 + 0.3 * Math.sin(phase)));
        int tint = 0xFF000000 | (brightness << 16) | (brightness << 8) | brightness;
        submitNodeCollector.order(1).submitModelPart(this.getParentModel().head, poseStack,
                this.renderType, lightCoords, OverlayTexture.NO_OVERLAY, null, tint, null);
    }

    public enum Form {
        /** White eyes, pupil-less: the classic look. */
        STALKER,
        /** Red eyes for the gold shrine's hunter. */
        HOSTILE,
        /** Red eyes on an all-black body for the elite. */
        ELITE,
        /** Burning orange eyes for the inferno form. */
        INFERNO;

        boolean matches(HerobrineRenderState state) {
            return switch (this) {
                case STALKER -> !state.hostile && !state.elite && !state.inferno;
                case HOSTILE -> state.hostile && !state.elite && !state.inferno;
                case ELITE -> state.elite && !state.inferno;
                case INFERNO -> state.inferno;
            };
        }
    }
}
