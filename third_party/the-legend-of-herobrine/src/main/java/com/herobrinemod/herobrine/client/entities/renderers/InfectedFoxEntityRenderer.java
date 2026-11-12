package com.herobrinemod.herobrine.client.entities.renderers;

import com.herobrinemod.herobrine.HerobrineMod;
import com.herobrinemod.herobrine.client.entities.models.HerobrineModelLayers;
import com.herobrinemod.herobrine.client.entities.models.InfectedFoxEntityModel;
import com.herobrinemod.herobrine.entities.InfectedFoxEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.entity.passive.FoxEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

public class InfectedFoxEntityRenderer extends MobEntityRenderer<InfectedFoxEntity, InfectedFoxEntityModel> {
    public InfectedFoxEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new InfectedFoxEntityModel(context.getPart(HerobrineModelLayers.INFECTED_FOX_MODEL_LAYER)), 0.4f);
        this.addFeature(new InfectedFoxEyesFeatureRenderer(this));
    }

    @Override
    public Identifier getTexture(@NotNull InfectedFoxEntity foxEntity) {
        return foxEntity.getVariant() == FoxEntity.Type.RED ? Identifier.of(HerobrineMod.MODID, "textures/entity/infected_fox/fox.png") : Identifier.of(HerobrineMod.MODID, "textures/entity/infected_fox/snow_fox.png");
    }
}
