package com.herobrinemod.herobrine.client.entities.renderers;

import com.herobrinemod.herobrine.client.entities.models.InfectedSheepEntityModel;
import com.herobrinemod.herobrine.client.entities.models.InfectedSheepWoolEntityModel;
import com.herobrinemod.herobrine.entities.InfectedSheepEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.EntityModelLoader;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import org.jetbrains.annotations.NotNull;

@Environment(EnvType.CLIENT)
public class InfectedSheepWoolFeatureRenderer extends FeatureRenderer<InfectedSheepEntity, InfectedSheepEntityModel> {
    private static final Identifier SKIN = Identifier.of("textures/entity/sheep/sheep_fur.png");
    private final InfectedSheepWoolEntityModel model;
    public InfectedSheepWoolFeatureRenderer(FeatureRendererContext<InfectedSheepEntity, InfectedSheepEntityModel> context, @NotNull EntityModelLoader loader) {
        super(context);
        this.model = new InfectedSheepWoolEntityModel(loader.getModelPart(EntityModelLayers.SHEEP_FUR));
    }

    @Override
    public void render(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, @NotNull InfectedSheepEntity entity, float f, float g, float h, float j, float k, float l) {
        if (!entity.isSheared()) {
            if (entity.isInvisible()) {
                MinecraftClient minecraftClient = MinecraftClient.getInstance();
                boolean bl = minecraftClient.hasOutline(entity);
                if (bl) {
                    this.getContextModel().copyStateTo(this.model);
                    this.model.animateModel(entity, f, g, h);
                    this.model.setAngles(entity, f, g, j, k, l);
                    VertexConsumer vertexConsumer = vertexConsumerProvider.getBuffer(RenderLayer.getOutline(SKIN));
                    this.model.render(matrixStack, vertexConsumer, i, LivingEntityRenderer.getOverlay(entity, 0.0F), -16777216);
                }
            } else {
                int u;
                if (entity.hasCustomName() && "jeb_".equals(entity.getName().getString())) {
                    int n = entity.age / 25 + entity.getId();
                    int o = DyeColor.values().length;
                    int p = n % o;
                    int q = (n + 1) % o;
                    float r = ((float)(entity.age % 25) + h) / 25.0F;
                    int s = InfectedSheepEntity.getRgbColor(DyeColor.byId(p));
                    int t = InfectedSheepEntity.getRgbColor(DyeColor.byId(q));
                    u = ColorHelper.Argb.lerp(r, s, t);
                } else {
                    u = InfectedSheepEntity.getRgbColor(entity.getColor());
                }

                render(this.getContextModel(), this.model, SKIN, matrixStack, vertexConsumerProvider, i, entity, f, g, j, k, l, h, u);
            }
        }
    }
}
