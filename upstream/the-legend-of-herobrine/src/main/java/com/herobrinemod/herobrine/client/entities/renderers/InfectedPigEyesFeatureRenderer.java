package com.herobrinemod.herobrine.client.entities.renderers;

import com.herobrinemod.herobrine.HerobrineMod;
import com.herobrinemod.herobrine.client.entities.models.InfectedPigEntityModel;
import com.herobrinemod.herobrine.entities.InfectedPigEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.feature.EyesFeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class InfectedPigEyesFeatureRenderer extends EyesFeatureRenderer<InfectedPigEntity, InfectedPigEntityModel> {
    public InfectedPigEyesFeatureRenderer(FeatureRendererContext<InfectedPigEntity, InfectedPigEntityModel> featureRendererContext) {
        super(featureRendererContext);
    }

    @Override
    public RenderLayer getEyesTexture() {
        return RenderLayer.getEyes(Identifier.of(HerobrineMod.MODID, "textures/entity/eyes/infected_pig.png"));
    }
}
