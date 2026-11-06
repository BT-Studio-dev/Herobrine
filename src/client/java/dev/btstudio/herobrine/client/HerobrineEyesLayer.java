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
 * has to hand the model to the node collector explicitly, and the texture has to be fixed when the
 * layer is built because {@code submit} never gets to choose it. Four layers, one per form, each
 * only submitting while the render state matches.
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
        submitNodeCollector.order(1).submitModelPart(this.getParentModel().head, poseStack,
                this.renderType, lightCoords, OverlayTexture.NO_OVERLAY, null);
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
