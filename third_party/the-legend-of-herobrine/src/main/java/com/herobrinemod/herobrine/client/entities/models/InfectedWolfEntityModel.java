package com.herobrinemod.herobrine.client.entities.models;

import com.google.common.collect.ImmutableList;
import com.herobrinemod.herobrine.entities.InfectedWolfEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.EntityModelPartNames;
import net.minecraft.client.render.entity.model.TintableAnimalModel;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;

@Environment(EnvType.CLIENT)
public class InfectedWolfEntityModel extends TintableAnimalModel<InfectedWolfEntity> {
    private final ModelPart head;
    private final ModelPart torso;
    private final ModelPart rightHindLeg;
    private final ModelPart leftHindLeg;
    private final ModelPart rightFrontLeg;
    private final ModelPart leftFrontLeg;
    private final ModelPart tail;
    private final ModelPart realTail;
    private final ModelPart neck;

    public InfectedWolfEntityModel(@NotNull ModelPart root) {
        this.head = root.getChild(EntityModelPartNames.HEAD);
        this.torso = root.getChild(EntityModelPartNames.BODY);
        this.neck = root.getChild("upper_body");
        this.rightHindLeg = root.getChild(EntityModelPartNames.RIGHT_HIND_LEG);
        this.leftHindLeg = root.getChild(EntityModelPartNames.LEFT_HIND_LEG);
        this.rightFrontLeg = root.getChild(EntityModelPartNames.RIGHT_FRONT_LEG);
        this.leftFrontLeg = root.getChild(EntityModelPartNames.LEFT_FRONT_LEG);
        this.tail = root.getChild(EntityModelPartNames.TAIL);
        this.realTail = this.tail.getChild("real_tail");
    }

    public static TexturedModelData getTexturedModelData() {
        return TexturedModelData.of(getModelData(Dilation.NONE), 64, 32);
    }

    public static @NotNull ModelData getModelData(Dilation dilation) {
        ModelData modelData = new ModelData();
        ModelPartData modelPartData = modelData.getRoot();
        ModelPartData modelPartData2 = modelPartData.addChild(EntityModelPartNames.HEAD, ModelPartBuilder.create(), ModelTransform.pivot(-1.0F, 13.5F, -7.0F));
        modelPartData2.addChild(
                "real_head",
                ModelPartBuilder.create()
                        .uv(0, 0)
                        .cuboid(-2.0F, -3.0F, -2.0F, 6.0F, 6.0F, 4.0F, dilation)
                        .uv(16, 14)
                        .cuboid(-2.0F, -5.0F, 0.0F, 2.0F, 2.0F, 1.0F, dilation)
                        .uv(16, 14)
                        .cuboid(2.0F, -5.0F, 0.0F, 2.0F, 2.0F, 1.0F, dilation)
                        .uv(0, 10)
                        .cuboid(-0.5F, -0.001F, -5.0F, 3.0F, 3.0F, 4.0F, dilation),
                ModelTransform.NONE
        );
        modelPartData.addChild(
                EntityModelPartNames.BODY,
                ModelPartBuilder.create().uv(18, 14).cuboid(-3.0F, -2.0F, -3.0F, 6.0F, 9.0F, 6.0F, dilation),
                ModelTransform.of(0.0F, 14.0F, 2.0F, (float) (Math.PI / 2), 0.0F, 0.0F)
        );
        modelPartData.addChild(
                "upper_body",
                ModelPartBuilder.create().uv(21, 0).cuboid(-3.0F, -3.0F, -3.0F, 8.0F, 6.0F, 7.0F, dilation),
                ModelTransform.of(-1.0F, 14.0F, -3.0F, (float) (Math.PI / 2), 0.0F, 0.0F)
        );
        ModelPartBuilder modelPartBuilder = ModelPartBuilder.create().uv(0, 18).cuboid(0.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F, dilation);
        modelPartData.addChild(EntityModelPartNames.RIGHT_HIND_LEG, modelPartBuilder, ModelTransform.pivot(-2.5F, 16.0F, 7.0F));
        modelPartData.addChild(EntityModelPartNames.LEFT_HIND_LEG, modelPartBuilder, ModelTransform.pivot(0.5F, 16.0F, 7.0F));
        modelPartData.addChild(EntityModelPartNames.RIGHT_FRONT_LEG, modelPartBuilder, ModelTransform.pivot(-2.5F, 16.0F, -4.0F));
        modelPartData.addChild(EntityModelPartNames.LEFT_FRONT_LEG, modelPartBuilder, ModelTransform.pivot(0.5F, 16.0F, -4.0F));
        ModelPartData modelPartData3 = modelPartData.addChild(
                EntityModelPartNames.TAIL, ModelPartBuilder.create(), ModelTransform.of(-1.0F, 12.0F, 8.0F, (float) (Math.PI / 5), 0.0F, 0.0F)
        );
        modelPartData3.addChild("real_tail", ModelPartBuilder.create().uv(9, 18).cuboid(0.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F, dilation), ModelTransform.NONE);
        return modelData;
    }

    @Override
    protected Iterable<ModelPart> getHeadParts() {
        return ImmutableList.of(this.head);
    }

    @Override
    protected Iterable<ModelPart> getBodyParts() {
        return ImmutableList.of(this.torso, this.rightHindLeg, this.leftHindLeg, this.rightFrontLeg, this.leftFrontLeg, this.tail, this.neck);
    }

    @Override
    public void animateModel(@NotNull InfectedWolfEntity entity, float f, float g, float h) {
        this.tail.yaw = MathHelper.cos(f * 0.6662F) * 1.4F * g;
        this.torso.setPivot(0.0F, 14.0F, 2.0F);
        this.torso.pitch = (float) (Math.PI / 2);
        this.neck.setPivot(-1.0F, 14.0F, -3.0F);
        this.neck.pitch = this.torso.pitch;
        this.tail.setPivot(-1.0F, 12.0F, 8.0F);
        this.rightHindLeg.setPivot(-2.5F, 16.0F, 7.0F);
        this.leftHindLeg.setPivot(0.5F, 16.0F, 7.0F);
        this.rightFrontLeg.setPivot(-2.5F, 16.0F, -4.0F);
        this.leftFrontLeg.setPivot(0.5F, 16.0F, -4.0F);
        this.rightHindLeg.pitch = MathHelper.cos(f * 0.6662F) * 1.4F * g;
        this.leftHindLeg.pitch = MathHelper.cos(f * 0.6662F + (float) Math.PI) * 1.4F * g;
        this.rightFrontLeg.pitch = MathHelper.cos(f * 0.6662F + (float) Math.PI) * 1.4F * g;
        this.leftFrontLeg.pitch = MathHelper.cos(f * 0.6662F) * 1.4F * g;
        this.neck.roll = entity.getShakeAnimationProgress(h, -0.08F);
        this.torso.roll = entity.getShakeAnimationProgress(h, -0.16F);
        this.realTail.roll = entity.getShakeAnimationProgress(h, -0.2F);
    }

    @Override
    public void setAngles(InfectedWolfEntity entity, float f, float g, float h, float i, float j) {
        this.head.pitch = j * (float) (Math.PI / 180.0);
        this.head.yaw = i * (float) (Math.PI / 180.0);
        this.tail.pitch = h;
    }
}
