package dev.btstudio.herobrine.client;

import dev.btstudio.herobrine.HerobrineMod;
import dev.btstudio.herobrine.entity.SteveFriendEntity;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.Identifier;

/**
 * Renders Steve with the original plain humanoid player mesh and skin.
 */
public class SteveRenderer extends HumanoidMobRenderer<SteveFriendEntity, SteveRenderState,
        HumanoidModel<SteveRenderState>> {
    private static final Identifier TEXTURE = HerobrineMod.id("textures/entity/steve_friend.png");
    private static final int TEXTURE_WIDTH = 64;
    private static final int TEXTURE_HEIGHT = 64;

    public SteveRenderer(EntityRendererProvider.Context context) {
        super(context, createModel(), 0.5F);
    }

    private static HumanoidModel<SteveRenderState> createModel() {
        LayerDefinition layer = LayerDefinition.create(
                HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F), TEXTURE_WIDTH, TEXTURE_HEIGHT);
        return new HumanoidModel<>(layer.bakeRoot());
    }

    @Override
    public SteveRenderState createRenderState() {
        return new SteveRenderState();
    }

    @Override
    public Identifier getTextureLocation(SteveRenderState state) {
        return TEXTURE;
    }
}
