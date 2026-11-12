package com.herobrinemod.herobrine.client.entities.renderers;

import com.herobrinemod.herobrine.HerobrineMod;
import com.herobrinemod.herobrine.client.entities.models.HerobrineModelLayers;
import com.herobrinemod.herobrine.client.entities.models.InfectedArmadilloEntityModel;
import com.herobrinemod.herobrine.entities.InfectedArmadilloEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class InfectedArmadilloEntityRenderer extends MobEntityRenderer<InfectedArmadilloEntity, InfectedArmadilloEntityModel> {
    public InfectedArmadilloEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new InfectedArmadilloEntityModel(context.getPart(HerobrineModelLayers.INFECTED_ARMADILLO_MODEL_LAYER)), 0.4f);
        this.addFeature(new InfectedArmadilloEyesFeatureRenderer(this));
    }

    @Override
    public Identifier getTexture(InfectedArmadilloEntity armadilloEntity) {
        return Identifier.of(HerobrineMod.MODID, "textures/entity/infected_armadillo.png");
    }
}
