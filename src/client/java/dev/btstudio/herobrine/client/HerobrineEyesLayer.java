package dev.btstudio.herobrine.client;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.btstudio.herobrine.HerobrineMod;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/**
 * Animated glowing eyes. {@link RenderTypes#eyes(Identifier)} is the unlit render type vanilla
 * uses for spider eyes. A high-resolution, translucent eye mask gives <b>both</b> eyes a soft halo
 * on the face; it is not a shader-pack bloom effect and does not illuminate the world.
 *
 * <p>Three layers, one per form, each submit only while the render state matches. The emissive
 * layer pulses gently without changing the underlying skin or the eye color for each form.
 *
 * <p>It extends vanilla's {@link EyesLayer} rather than reimplementing {@code RenderLayer}, so the
 * eyes travel the same code path as an enderman's or a spider's - the one renderer patches, shader
 * packs among them, already know how to handle.
 */
public class HerobrineEyesLayer extends EyesLayer<HerobrineRenderState,
        HumanoidModel<HerobrineRenderState>> {
    private static final Identifier EYES_STALKER = HerobrineMod.id("textures/entity/herobrine_eyes.png");
    private static final Identifier EYES_HOSTILE = HerobrineMod.id("textures/entity/herobrine_hostile_eyes.png");
    private static final Identifier EYES_ELITE = HerobrineMod.id("textures/entity/herobrine_elite_eyes.png");

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
        });
    }

    /**
     * The render type for this layer's form. Vanilla's {@link EyesLayer} calls this itself, and
     * renderer patches - shader packs included - recognise the class, so the eyes go down the same
     * path an enderman's or a spider's do instead of one only this mod knows about.
     */
    @Override
    public RenderType renderType() {
        return this.renderType;
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords,
                       HerobrineRenderState state, float yRot, float xRot) {
        if (!this.form.matches(state)) {
            return;
        }

        // The head uses the standard skin UVs. The 256x256 mask is four times the resolution of
        // the 64x64 skin, so the halo can fade out softly instead of in whole pixels. The head's
        // front face is skin 8,8 to 16,16, which is x 32-63, y 32-63 on the mask, and the mask
        // carries both eyes as mirrors of each other about the centre of that face: the bridge of
        // the nose sits between x=47 and x=48, so a pixel at x pairs with 95 - x. Skin pixels 9-10
        // glow through mask x 36-43, and skin pixels 13-14 through mask x 52-59.
        // Tinting the emissive pixels leaves the rest of the skin unchanged. The cycle is three
        // seconds at 20 ticks per second, with partial ticks for smooth animation.
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
        ELITE;

        boolean matches(HerobrineRenderState state) {
            return switch (this) {
                case STALKER -> !state.hostile && !state.elite;
                case HOSTILE -> state.hostile && !state.elite;
                case ELITE -> state.elite;
            };
        }
    }
}
