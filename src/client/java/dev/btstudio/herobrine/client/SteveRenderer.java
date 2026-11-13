package dev.btstudio.herobrine.client;

import dev.btstudio.herobrine.HerobrineMod;
import dev.btstudio.herobrine.entity.SteveFriendEntity;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.Identifier;

/**
 * Renders Steve with the vanilla wide player mesh.
 *
 * This asks the entity model set for the shared {@link ModelLayers#PLAYER} layer rather than
 * baking a private copy of the same mesh. The two are identical on a vanilla client -
 * {@code HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F)} at 64x64 is exactly what the
 * layer is defined as - so nothing looks different by default.
 *
 * What does change is that Steve now goes through the model set, which is where Entity Model
 * Features attaches its overrides. Alex already used {@link ModelLayers#PLAYER_SLIM} and so was
 * picked up by packs like Fresh Moves; Steve and Herobrine baked their own meshes, missed the
 * hook entirely and stayed stiff. Steve is Alex's wide counterpart, so the two should be treated
 * alike. Herobrine keeps his own mesh on purpose: he is not meant to move like a player.
 */
public class SteveRenderer extends HumanoidMobRenderer<SteveFriendEntity, SteveRenderState,
        HumanoidModel<SteveRenderState>> {
    private static final Identifier TEXTURE = HerobrineMod.id("textures/entity/steve_friend.png");

    public SteveRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
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
