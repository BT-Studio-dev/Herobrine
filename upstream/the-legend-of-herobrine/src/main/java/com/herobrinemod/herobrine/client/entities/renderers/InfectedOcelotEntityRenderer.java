package com.herobrinemod.herobrine.client.entities.renderers;

import com.herobrinemod.herobrine.HerobrineMod;
import com.herobrinemod.herobrine.client.entities.models.HerobrineModelLayers;
import com.herobrinemod.herobrine.entities.InfectedOcelotEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.OcelotEntityModel;
import net.minecraft.util.Identifier;

public class InfectedOcelotEntityRenderer extends MobEntityRenderer<InfectedOcelotEntity, OcelotEntityModel<InfectedOcelotEntity>> {
    public InfectedOcelotEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new OcelotEntityModel<>(context.getPart(HerobrineModelLayers.INFECTED_OCELOT_MODEL_LAYER)), 0.4f);
        this.addFeature(new InfectedOcelotEyesFeatureRenderer(this));
    }

    @Override
    public Identifier getTexture(InfectedOcelotEntity entity) {
        return Identifier.of(HerobrineMod.MODID, "textures/entity/infected_ocelot.png");
    }
}