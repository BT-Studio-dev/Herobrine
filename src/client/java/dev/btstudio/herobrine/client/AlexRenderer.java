package dev.btstudio.herobrine.client;

import dev.btstudio.herobrine.HerobrineMod;
import dev.btstudio.herobrine.entity.AlexFriendEntity;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.Identifier;

/**
 * Renders Alex with the slim player model - narrower arms, the vanilla Alex silhouette - and the
 * same plain face as Steve: white eyes, blue pupils, no glow. She is meant to read as a person
 * standing next to you, not as another thing in the dark.
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
