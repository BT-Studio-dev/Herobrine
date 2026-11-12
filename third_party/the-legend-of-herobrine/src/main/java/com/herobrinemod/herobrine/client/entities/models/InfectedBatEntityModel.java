package com.herobrinemod.herobrine.client.entities.models;

import com.herobrinemod.herobrine.entities.InfectedBatEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.*;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.animation.BatAnimations;
import net.minecraft.client.render.entity.model.EntityModelPartNames;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import org.jetbrains.annotations.NotNull;

@Environment(EnvType.CLIENT)
public class InfectedBatEntityModel extends SinglePartEntityModel<InfectedBatEntity> {
    private final ModelPart root;
    private final ModelPart head;

    public InfectedBatEntityModel(@NotNull ModelPart root) {
        super(RenderLayer::getEntityCutout);
        this.root = root;
        this.head = root.getChild(EntityModelPartNames.HEAD);
    }

    public static @NotNull TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData modelPartData = modelData.getRoot();
        ModelPartData modelPartData2 = modelPartData.addChild(
                EntityModelPartNames.BODY, ModelPartBuilder.create().uv(0, 0).cuboid(-1.5F, 0.0F, -1.0F, 3.0F, 5.0F, 2.0F), ModelTransform.pivot(0.0F, 17.0F, 0.0F)
        );
        ModelPartData modelPartData3 = modelPartData.addChild(
                EntityModelPartNames.HEAD, ModelPartBuilder.create().uv(0, 7).cuboid(-2.0F, -3.0F, -1.0F, 4.0F, 3.0F, 2.0F), ModelTransform.pivot(0.0F, 17.0F, 0.0F)
        );
        modelPartData3.addChild(
                EntityModelPartNames.RIGHT_EAR, ModelPartBuilder.create().uv(1, 15).cuboid(-2.5F, -4.0F, 0.0F, 3.0F, 5.0F, 0.0F), ModelTransform.pivot(-1.5F, -2.0F, 0.0F)
        );
        modelPartData3.addChild(
                EntityModelPartNames.LEFT_EAR, ModelPartBuilder.create().uv(8, 15).cuboid(-0.1F, -3.0F, 0.0F, 3.0F, 5.0F, 0.0F), ModelTransform.pivot(1.1F, -3.0F, 0.0F)
        );
        ModelPartData modelPartData4 = modelPartData2.addChild(
                EntityModelPartNames.RIGHT_WING, ModelPartBuilder.create().uv(12, 0).cuboid(-2.0F, -2.0F, 0.0F, 2.0F, 7.0F, 0.0F), ModelTransform.pivot(-1.5F, 0.0F, 0.0F)
        );
        modelPartData4.addChild(
                EntityModelPartNames.RIGHT_WING_TIP,
                ModelPartBuilder.create().uv(16, 0).cuboid(-6.0F, -2.0F, 0.0F, 6.0F, 8.0F, 0.0F),
                ModelTransform.pivot(-2.0F, 0.0F, 0.0F)
        );
        ModelPartData modelPartData5 = modelPartData2.addChild(
                EntityModelPartNames.LEFT_WING, ModelPartBuilder.create().uv(12, 7).cuboid(0.0F, -2.0F, 0.0F, 2.0F, 7.0F, 0.0F), ModelTransform.pivot(1.5F, 0.0F, 0.0F)
        );
        modelPartData5.addChild(
                EntityModelPartNames.LEFT_WING_TIP, ModelPartBuilder.create().uv(16, 8).cuboid(0.0F, -2.0F, 0.0F, 6.0F, 8.0F, 0.0F), ModelTransform.pivot(2.0F, 0.0F, 0.0F)
        );
        modelPartData2.addChild(
                EntityModelPartNames.FEET, ModelPartBuilder.create().uv(16, 16).cuboid(-1.5F, 0.0F, 0.0F, 3.0F, 2.0F, 0.0F), ModelTransform.pivot(0.0F, 5.0F, 0.0F)
        );
        return TexturedModelData.of(modelData, 32, 32);
    }

    @Override
    public ModelPart getPart() {
        return this.root;
    }

    @Override
    public void setAngles(InfectedBatEntity entity, float f, float g, float h, float i, float j) {
        this.getPart().traverse().forEach(ModelPart::resetTransform);
        if (entity.isRoosting()) {
            this.setRoostingHeadAngles(i);
        }

        this.updateAnimation(entity.flyingAnimationState, BatAnimations.FLYING, h, 1.0F);
        this.updateAnimation(entity.roostingAnimationState, BatAnimations.ROOSTING, h, 1.0F);
    }

    private void setRoostingHeadAngles(float yaw) {
        this.head.yaw = yaw * (float) (Math.PI / 180.0);
    }
}
