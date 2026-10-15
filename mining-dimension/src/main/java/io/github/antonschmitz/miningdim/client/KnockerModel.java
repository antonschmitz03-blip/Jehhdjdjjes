package io.github.antonschmitz.miningdim.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.antonschmitz.miningdim.MiningDimension;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

/**
 * A hunched little mine goblin: big head with long ears and a snaggle-toothed grin, miner's cap with a brass lamp,
 * a stash sack on its back and long dangling arms. Generated from the model spec used for the preview renders,
 * so keep texture offsets in sync with the texture.
 */
public class KnockerModel extends EntityModel<KnockerRenderState> implements ArmedModel<KnockerRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(MiningDimension.id("knocker"), "main");
    private final ModelPart head;
    private final ModelPart rightEar;
    private final ModelPart leftEar;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final ModelPart[] accessories = new ModelPart[6];

    public KnockerModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.rightEar = this.head.getChild("right_ear");
        this.leftEar = this.head.getChild("left_ear");
        this.body = root.getChild("body");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
        this.accessories[0] = this.body.getChild("scarf");
        this.accessories[1] = this.leftEar.getChild("earring");
        this.accessories[2] = this.head.getChild("hat").getChild("feather");
        this.accessories[3] = this.body.getChild("pickaxe_handle");
        this.accessories[4] = this.body.getChild("pickaxe_head");
        this.accessories[5] = this.body.getChild("pouch");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4F, -6F, -4F, 8F, 6F, 6F)
                .texOffs(20, 22).addBox(-1F, -4F, -5.5F, 2F, 2F, 2F)
                .texOffs(0, 22).addBox(-3F, 0F, -3.5F, 6F, 1F, 4F),
                PartPose.offset(0F, 12F, -1F));
        PartDefinition hat = head.addOrReplaceChild("hat", CubeListBuilder.create()
                .texOffs(0, 12).addBox(-4.5F, -8F, -4.5F, 9F, 3F, 7F)
                .texOffs(36, 22).addBox(-4.5F, -6F, -6F, 9F, 1F, 2F)
                .texOffs(58, 22).addBox(-1F, -8.5F, -5.5F, 2F, 2F, 1F),
                PartPose.offset(0F, 0F, 0F));
        hat.addOrReplaceChild("feather", CubeListBuilder.create()
                .texOffs(8, 37).addBox(-0.5F, -4F, -0.5F, 1F, 4F, 1F),
                PartPose.offsetAndRotation(3F, -7.5F, 0F, 0F, 0F, 0.3F));
        head.addOrReplaceChild("right_ear", CubeListBuilder.create()
                .texOffs(28, 22).addBox(-3F, -1.5F, 0F, 3F, 3F, 1F)
                .texOffs(28, 22).addBox(-5F, -1F, 0F, 2F, 1F, 1F),
                PartPose.offsetAndRotation(-4F, -4F, -1F, 0F, 0.45F, -0.25F));
        PartDefinition leftEar = head.addOrReplaceChild("left_ear", CubeListBuilder.create()
                .texOffs(28, 22).addBox(0F, -1.5F, 0F, 3F, 3F, 1F)
                .texOffs(28, 22).addBox(3F, -1F, 0F, 2F, 1F, 1F),
                PartPose.offsetAndRotation(4F, -4F, -1F, 0F, -0.45F, 0.25F));
        leftEar.addOrReplaceChild("earring", CubeListBuilder.create()
                .texOffs(0, 37).addBox(4F, 0F, 0F, 1F, 2F, 1F),
                PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(28, 0).addBox(-3F, 0F, -2F, 6F, 7F, 4F)
                .texOffs(32, 12).addBox(-2.5F, 0.5F, 2F, 5F, 5F, 3F),
                PartPose.offset(0F, 12F, 0F));
        body.addOrReplaceChild("scarf", CubeListBuilder.create().texOffs(0, 29).addBox(-3.5F, -0.5F, -2.5F, 7F, 2F, 5F), PartPose.ZERO);
        body.addOrReplaceChild("pickaxe_handle", CubeListBuilder.create().texOffs(14, 37).addBox(1.5F, -1F, 5F, 1F, 7F, 1F), PartPose.ZERO);
        body.addOrReplaceChild("pickaxe_head", CubeListBuilder.create().texOffs(0, 46).addBox(-0.5F, -1F, 5F, 5F, 1F, 1F), PartPose.ZERO);
        body.addOrReplaceChild("pouch", CubeListBuilder.create().texOffs(20, 37).addBox(3F, 5F, -1F, 1F, 2F, 2F), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(48, 0).addBox(-0.5F, -1F, -1F, 1.5F, 9F, 2F),
                PartPose.offset(-4F, 13F, 0F));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(48, 0).addBox(-1F, -1F, -1F, 1.5F, 9F, 2F),
                PartPose.offset(4F, 13F, 0F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(48, 12).addBox(-1F, 0F, -1F, 2F, 5F, 2F),
                PartPose.offset(-1.5F, 19F, 0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(48, 12).addBox(-1F, 0F, -1F, 2F, 5F, 2F),
                PartPose.offset(1.5F, 19F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(KnockerRenderState state) {
        super.setupAnim(state);
        for (int i = 0; i < this.accessories.length; i++) {
            this.accessories[i].visible = (state.accessories >> i & 1) != 0;
        }
        float deg = Mth.DEG_TO_RAD;
        this.head.yRot = state.yRot * deg;
        this.head.xRot = state.xRot * deg;
        this.body.xRot = 0.12F;

        float pos = state.walkAnimationPos * 0.8F;
        float speed = Math.min(state.walkAnimationSpeed, 1.0F);
        this.rightLeg.xRot = Mth.cos(pos) * 1.3F * speed;
        this.leftLeg.xRot = Mth.cos(pos + Mth.PI) * 1.3F * speed;
        this.rightArm.xRot = Mth.cos(pos + Mth.PI) * 0.9F * speed;
        this.leftArm.xRot = Mth.cos(pos) * 0.9F * speed;

        float idle = Mth.sin(state.ageInTicks * 0.09F);
        this.rightArm.zRot = 0.12F + idle * 0.05F;
        this.leftArm.zRot = -0.12F - idle * 0.05F;
        this.rightEar.zRot += idle * 0.08F;
        this.leftEar.zRot -= idle * 0.08F;

        if (!state.rightHandItemState.isEmpty()) {
            // Clutches the loot to its chest and cackles.
            this.rightArm.xRot = -0.95F + Mth.cos(state.ageInTicks * 0.5F) * 0.08F * speed;
            this.rightArm.yRot = -0.35F;
            this.head.zRot = Mth.sin(state.ageInTicks * 0.35F) * 0.06F;
        }
        if (state.swingAnimation > 0.0F) {
            float swing = Mth.sin(state.swingAnimation * Mth.PI);
            this.rightArm.xRot -= swing * 1.2F;
            this.leftArm.xRot -= swing * 1.2F;
        }
    }

    @Override
    public void translateToHand(KnockerRenderState state, HumanoidArm arm, PoseStack poseStack) {
        (arm == HumanoidArm.RIGHT ? this.rightArm : this.leftArm).translateAndRotate(poseStack);
        poseStack.scale(0.75F, 0.75F, 0.75F);
    }
}
