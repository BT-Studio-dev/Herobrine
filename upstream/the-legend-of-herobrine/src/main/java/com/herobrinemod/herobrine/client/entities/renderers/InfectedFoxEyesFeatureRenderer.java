package com.herobrinemod.herobrine.client.entities.renderers;

import com.herobrinemod.herobrine.HerobrineMod;
import com.herobrinemod.herobrine.client.entities.models.InfectedFoxEntityModel;
import com.herobrinemod.herobrine.entities.InfectedFoxEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.feature.EyesFeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.util.Identifier;

public class InfectedFoxEyesFeatureRenderer extends EyesFeatureRenderer<InfectedFoxEntity, InfectedFoxEntityModel> {
    public InfectedFoxEyesFeatureRenderer(FeatureRendererContext<InfectedFoxEntity, InfectedFoxEntityModel> featureRendererContext) {
        super(featureRendererContext);
    }

    @Override
    public RenderLayer getEyesTexture() {
        return RenderLayer.getEyes(Identifier.of(HerobrineMod.MODID, "textures/entity/eyes/infected_fox.png"));
    }
}
