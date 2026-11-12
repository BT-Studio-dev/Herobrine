package com.herobrinemod.herobrine.client.entities.models;

import com.herobrinemod.herobrine.entities.InfectedArmadilloEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.animation.ArmadilloAnimations;
import net.minecraft.client.render.entity.model.EntityModelPartNames;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;

@Environment(EnvType.CLIENT)
public class InfectedArmadilloEntityModel extends SinglePartEntityModel<InfectedArmadilloEntity> {
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart rightHindLeg;
    private final ModelPart leftHindLeg;
    private final ModelPart head;
    private final ModelPart tail;

    public InfectedArmadilloEntityModel(@NotNull ModelPart root) {
        super();
        this.root = root;
        this.body = root.getChild(EntityModelPartNames.BODY);
        this.rightHindLeg = root.getChild(EntityModelPartNames.RIGHT_HIND_LEG);
        this.leftHindLeg = root.getChild(EntityModelPartNames.LEFT_HIND_LEG);
        this.head = this.body.getChild(EntityModelPartNames.HEAD);
        this.tail = this.body.getChild(EntityModelPartNames.TAIL);
    }

    public static @NotNull TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData modelPartData = modelData.getRoot();
        ModelPartData modelPartData2 = modelPartData.addChild(
                EntityModelPartNames.BODY,
                ModelPartBuilder.create()
                        .uv(0, 20)
                        .cuboid(-4.0F, -7.0F, -10.0F, 8.0F, 8.0F, 12.0F, new Dilation(0.3F))
                        .uv(0, 40)
                        .cuboid(-4.0F, -7.0F, -10.0F, 8.0F, 8.0F, 12.0F, new Dilation(0.0F)),
                ModelTransform.pivot(0.0F, 21.0F, 4.0F)
        );
        modelPartData2.addChild(
                EntityModelPartNames.TAIL,
                ModelPartBuilder.create().uv(44, 53).cuboid(-0.5F, -0.0865F, 0.0933F, 1.0F, 6.0F, 1.0F, new Dilation(0.0F)),
                ModelTransform.of(0.0F, -3.0F, 1.0F, 0.5061F, 0.0F, 0.0F)
        );
        ModelPartData modelPartData3 = modelPartData2.addChild(EntityModelPartNames.HEAD, ModelPartBuilder.create(), ModelTransform.pivot(0.0F, -2.0F, -11.0F));
        modelPartData3.addChild(
                "head_cube",
                ModelPartBuilder.create().uv(43, 15).cuboid(-1.5F, -1.0F, -1.0F, 3.0F, 5.0F, 2.0F, new Dilation(0.0F)),
                ModelTransform.of(0.0F, 0.0F, 0.0F, -0.3927F, 0.0F, 0.0F)
        );
        ModelPartData modelPartData4 = modelPartData3.addChild(EntityModelPartNames.RIGHT_EAR, ModelPartBuilder.create(), ModelTransform.pivot(-1.0F, -1.0F, 0.0F));
        modelPartData4.addChild(
                "right_ear_cube",
                ModelPartBuilder.create().uv(43, 10).cuboid(-2.0F, -3.0F, 0.0F, 2.0F, 5.0F, 0.0F, new Dilation(0.0F)),
                ModelTransform.of(-0.5F, 0.0F, -0.6F, 0.1886F, -0.3864F, -0.0718F)
        );
        ModelPartData modelPartData5 = modelPartData3.addChild(EntityModelPartNames.LEFT_EAR, ModelPartBuilder.create(), ModelTransform.pivot(1.0F, -2.0F, 0.0F));
        modelPartData5.addChild(
                "left_ear_cube",
                ModelPartBuilder.create().uv(47, 10).cuboid(0.0F, -3.0F, 0.0F, 2.0F, 5.0F, 0.0F, new Dilation(0.0F)),
                ModelTransform.of(0.5F, 1.0F, -0.6F, 0.1886F, 0.3864F, 0.0718F)
        );
        modelPartData.addChild(
                EntityModelPartNames.RIGHT_HIND_LEG,
                ModelPartBuilder.create().uv(51, 31).cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F, new Dilation(0.0F)),
                ModelTransform.pivot(-2.0F, 21.0F, 4.0F)
        );
        modelPartData.addChild(
                EntityModelPartNames.LEFT_HIND_LEG,
                ModelPartBuilder.create().uv(42, 31).cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F, new Dilation(0.0F)),
                ModelTransform.pivot(2.0F, 21.0F, 4.0F)
        );
        modelPartData.addChild(
                EntityModelPartNames.RIGHT_FRONT_LEG,
                ModelPartBuilder.create().uv(51, 43).cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F, new Dilation(0.0F)),
                ModelTransform.pivot(-2.0F, 21.0F, -4.0F)
        );
        modelPartData.addChild(
                EntityModelPartNames.LEFT_FRONT_LEG,
                ModelPartBuilder.create().uv(42, 43).cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F, new Dilation(0.0F)),
                ModelTransform.pivot(2.0F, 21.0F, -4.0F)
        );
        return TexturedModelData.of(modelData, 64, 64);
    }

    @Override
    public ModelPart getPart() {
        return this.root;
    }

    @Override
    public void setAngles(@NotNull InfectedArmadilloEntity entity, float f, float g, float h, float i, float j) {
        this.getPart().traverse().forEach(ModelPart::resetTransform);
        this.body.hidden = false;
        this.leftHindLeg.visible = true;
        this.rightHindLeg.visible = true;
        this.tail.visible = true;
        this.head.pitch = MathHelper.clamp(j, -22.5F, 25.0F) * (float) (Math.PI / 180.0);
        this.head.yaw = MathHelper.clamp(i, -32.5F, 32.5F) * (float) (Math.PI / 180.0);
        this.animateMovement(ArmadilloAnimations.IDLE, f, g, 16.5F, 2.5F);
    }
}
