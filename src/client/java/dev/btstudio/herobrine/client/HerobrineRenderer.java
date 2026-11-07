package dev.btstudio.herobrine.client;

import dev.btstudio.herobrine.HerobrineMod;
import dev.btstudio.herobrine.entity.HerobrineEntity;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.Identifier;

/**
 * Renders Herobrine with a plain humanoid model: white eyes for the stalker, red eyes for the
 * gold shrine's hostile form, and black with red eyes for the elite.
 */
public class HerobrineRenderer extends HumanoidMobRenderer<HerobrineEntity, HerobrineRenderState,
        HumanoidModel<HerobrineRenderState>> {
    private static final Identifier TEXTURE = HerobrineMod.id("textures/entity/herobrine.png");
    private static final Identifier TEXTURE_HOSTILE = HerobrineMod.id("textures/entity/herobrine_hostile.png");
    private static final Identifier TEXTURE_ELITE = HerobrineMod.id("textures/entity/herobrine_elite.png");

    /** The vanilla skin layout is 64x64, including the hat/sleeve overlay areas. */
    private static final int TEXTURE_WIDTH = 64;
    private static final int TEXTURE_HEIGHT = 64;

    public HerobrineRenderer(EntityRendererProvider.Context context) {
        super(context, createModel(), 0.5F);

        // Glowing eyes: unlit, so they stay lit in the dark instead of fading into it.
        this.addLayer(new HerobrineEyesLayer(this, HerobrineEyesLayer.Form.STALKER));
        this.addLayer(new HerobrineEyesLayer(this, HerobrineEyesLayer.Form.HOSTILE));
        this.addLayer(new HerobrineEyesLayer(this, HerobrineEyesLayer.Form.ELITE));
    }

    /**
     * Bakes our own humanoid mesh instead of reusing {@code ModelLayers.PLAYER}. That vanilla layer
     * is built for the player's own {@code PlayerModel} (sleeves, jacket, ear, cloak overlays), and
     * rendering it through a plain {@link HumanoidModel} does not produce a correct figure. Baking
     * the standard humanoid mesh gives the mob exact vanilla player proportions, arms hanging down,
     * and the normal 64x64 skin layout.
     */
    private static HumanoidModel<HerobrineRenderState> createModel() {
        LayerDefinition layer = LayerDefinition.create(
                HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F), TEXTURE_WIDTH, TEXTURE_HEIGHT);

        return new HumanoidModel<>(layer.bakeRoot());
    }

    @Override
    public HerobrineRenderState createRenderState() {
        return new HerobrineRenderState();
    }

    @Override
    public void extractRenderState(HerobrineEntity entity, HerobrineRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.hostile = entity.isHostile();
        state.elite = entity.isElite();
        state.eyeGlowPhase = Math.floorMod(entity.getId(), 60);
    }

    @Override
    public Identifier getTextureLocation(HerobrineRenderState state) {
        if (state.elite) {
            return TEXTURE_ELITE;
        }

        return state.hostile ? TEXTURE_HOSTILE : TEXTURE;
    }
}
