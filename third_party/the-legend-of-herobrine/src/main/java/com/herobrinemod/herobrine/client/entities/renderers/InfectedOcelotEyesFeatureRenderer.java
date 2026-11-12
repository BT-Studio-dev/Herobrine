package com.herobrinemod.herobrine.client.entities.renderers;

import com.herobrinemod.herobrine.HerobrineMod;
import com.herobrinemod.herobrine.entities.InfectedOcelotEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.feature.EyesFeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.OcelotEntityModel;
import net.minecraft.util.Identifier;

public class InfectedOcelotEyesFeatureRenderer extends EyesFeatureRenderer<InfectedOcelotEntity, OcelotEntityModel<InfectedOcelotEntity>> {
    public InfectedOcelotEyesFeatureRenderer(FeatureRendererContext<InfectedOcelotEntity, OcelotEntityModel<InfectedOcelotEntity>> featureRendererContext) {
        super(featureRendererContext);
    }

    @Override
    public RenderLayer getEyesTexture() {
        return RenderLayer.getEyes(Identifier.of(HerobrineMod.MODID, "textures/entity/eyes/infected_ocelot.png"));
    }
}
