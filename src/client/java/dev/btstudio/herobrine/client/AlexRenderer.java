package dev.btstudio.herobrine.client;

import dev.btstudio.herobrine.HerobrineMod;
import dev.btstudio.herobrine.entity.AlexFriendEntity;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.Identifier;

/**
 * Renders Alex with the original slim player model and skin.
 */
public class AlexRenderer extends HumanoidMobRenderer<AlexFriendEntity, AlexRenderState,
        HumanoidModel<AlexRenderState>> {
    private static final Identifier TEXTURE = HerobrineMod.id("textures/entity/alex_friend.png");

    public AlexRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM)), 0.5F);
    }

    @Override
    public AlexRenderState createRenderState() {
        return new AlexRenderState();
    }

    @Override
    public Identifier getTextureLocation(AlexRenderState state) {
        return TEXTURE;
    }
}
