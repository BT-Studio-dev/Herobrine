package com.herobrinemod.herobrine.client.entities.renderers;

import com.herobrinemod.herobrine.HerobrineMod;
import com.herobrinemod.herobrine.client.entities.models.HerobrineModelLayers;
import com.herobrinemod.herobrine.client.entities.models.InfectedBatEntityModel;
import com.herobrinemod.herobrine.entities.InfectedBatEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class InfectedBatEntityRenderer extends MobEntityRenderer<InfectedBatEntity, InfectedBatEntityModel> {
    public InfectedBatEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new InfectedBatEntityModel(context.getPart(HerobrineModelLayers.INFECTED_BAT_MODEL_LAYER)), 0.25f);
        this.addFeature(new InfectedBatEyesFeatureRenderer(this));
    }

    @Override
    public Identifier getTexture(InfectedBatEntity entity) {
        return Identifier.of(HerobrineMod.MODID, "textures/entity/infected_bat.png");
    }
}
