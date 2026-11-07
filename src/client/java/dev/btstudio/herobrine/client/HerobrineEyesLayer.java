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
 * Animated right-eye glow. {@link RenderTypes#eyes(Identifier)} is the unlit render type vanilla
 * uses for spider eyes. A high-resolution, translucent eye mask gives the glow a soft halo on the
 * face; it is not a shader-pack bloom effect and does not illuminate the world.
 *
 * <p>Four layers, one per form, each submit only while the render state matches. The emissive
 * layer pulses gently without changing the underlying skin or the eye color for each form.
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

        // The head uses the standard skin UVs. The 256x256 mask is four times the resolution of
        // the 64x64 skin, providing a softer halo on the character's right (viewer's left) eye.
        // Tinting the emissive pixels leaves the other eye and the body skin unchanged. The cycle
        // is three seconds at 20 ticks per second, with partial ticks for smooth animation.
        double phase = (state.ageInTicks + state.eyeGlowPhase) * (2.0 * Math.PI / 60.0);
        int brightness = (int) Math.round(255.0 * (0.7 + 0.3 * Math.sin(phase)));
        int tint = 0xFF000000 | (brightness << 16) | (brightness << 8) | brightness;
        submitNodeCollector.order(1).submitModelPart(this.getParentModel().head, poseStack,
                this.renderType, lightCoords, OverlayTexture.NO_OVERLAY, null, tint, 0);
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
