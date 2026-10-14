package io.github.antonschmitz.miningdim.client;

import io.github.antonschmitz.miningdim.MiningDimension;
import net.minecraft.client.model.QuadrupedModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * A round little rock on stubby legs: pebble ears, a snout, a pebble tail and a geode crust with five crystals on its back.
 * Generated from the model spec used for the preview renders, so keep texture offsets in sync with the textures.
 */
public class PebblinModel extends QuadrupedModel<PebblinRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(MiningDimension.id("pebblin"), "main");

    public PebblinModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 24).addBox(-3.5F, -3F, -4F, 7F, 5F, 4F)
                .texOffs(20, 33).addBox(-2.5F, -4F, -4F, 5F, 1F, 3F)
                .texOffs(56, 33).addBox(-1.5F, 0F, -5F, 3F, 2F, 1F),
                PartPose.offset(0F, 18F, -5F));
        head.addOrReplaceChild("right_ear", CubeListBuilder.create()
                .texOffs(36, 33).addBox(-1F, -2F, -1F, 2F, 2F, 2F),
                PartPose.offsetAndRotation(-2.5F, -3F, -2F, 0F, 0F, -0.35F));
        head.addOrReplaceChild("left_ear", CubeListBuilder.create()
                .texOffs(36, 33).addBox(-1F, -2F, -1F, 2F, 2F, 2F),
                PartPose.offsetAndRotation(2.5F, -3F, -2F, 0F, 0F, 0.35F));
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.5F, -6F, -5F, 9F, 5F, 10F)
                .texOffs(0, 15).addBox(-3.5F, -7F, -4F, 7F, 1F, 8F)
                .texOffs(30, 15).addBox(-3.5F, -1F, -4F, 7F, 1F, 8F)
                .texOffs(30, 24).addBox(-2.5F, -8F, -2F, 5F, 1F, 5F)
                .texOffs(44, 33).addBox(-1F, -4F, 5F, 2F, 2F, 2F),
                PartPose.offset(0F, 22F, 0F));
        body.addOrReplaceChild("crystal_big", CubeListBuilder.create()
                .texOffs(22, 24).addBox(-1F, -6F, -1F, 2F, 6F, 2F),
                PartPose.offsetAndRotation(0F, -8F, 0.5F, -0.1F, 0.3F, 0.08F));
        body.addOrReplaceChild("crystal_right", CubeListBuilder.create()
                .texOffs(50, 24).addBox(-1F, -4F, -1F, 2F, 4F, 2F),
                PartPose.offsetAndRotation(-1.5F, -8F, -1F, -0.3F, 0.2F, -0.5F));
        body.addOrReplaceChild("crystal_left", CubeListBuilder.create()
                .texOffs(0, 33).addBox(-1F, -4F, -1F, 2F, 4F, 2F),
                PartPose.offsetAndRotation(1.5F, -8F, 1.5F, 0.35F, -0.3F, 0.5F));
        body.addOrReplaceChild("crystal_tiny_front", CubeListBuilder.create()
                .texOffs(0, 39).addBox(-0.5F, -2F, -0.5F, 1F, 2F, 1F),
                PartPose.offsetAndRotation(1F, -8F, -1.5F, -0.5F, 0F, 0.3F));
        body.addOrReplaceChild("crystal_tiny_back", CubeListBuilder.create()
                .texOffs(52, 33).addBox(-0.5F, -3F, -0.5F, 1F, 3F, 1F),
                PartPose.offsetAndRotation(-1F, -8F, 2.5F, 0.5F, 0F, -0.25F));
        root.addOrReplaceChild("right_hind_leg", CubeListBuilder.create()
                .texOffs(8, 33).addBox(-1.5F, 0F, -1.5F, 3F, 2F, 3F),
                PartPose.offset(-2.5F, 22F, 3F));
        root.addOrReplaceChild("left_hind_leg", CubeListBuilder.create()
                .texOffs(8, 33).addBox(-1.5F, 0F, -1.5F, 3F, 2F, 3F),
                PartPose.offset(2.5F, 22F, 3F));
        root.addOrReplaceChild("right_front_leg", CubeListBuilder.create()
                .texOffs(8, 33).addBox(-1.5F, 0F, -1.5F, 3F, 2F, 3F),
                PartPose.offset(-2.5F, 22F, -3F));
        root.addOrReplaceChild("left_front_leg", CubeListBuilder.create()
                .texOffs(8, 33).addBox(-1.5F, 0F, -1.5F, 3F, 2F, 3F),
                PartPose.offset(2.5F, 22F, -3F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(PebblinRenderState state) {
        super.setupAnim(state);
        // A waddle: the round body rocks side to side while walking.
        float waddle = Mth.cos(state.walkAnimationPos * 0.6662F) * 0.18F * state.walkAnimationSpeed;
        this.body.zRot = waddle;
        this.head.zRot = waddle * 0.5F;
        if (state.ore != io.github.antonschmitz.miningdim.Pebblin.Ore.NONE) {
            // Excited little hop while prospecting.
            float hop = Math.abs(Mth.sin(state.ageInTicks * 0.35F)) * 0.6F;
            this.body.y -= hop;
            this.head.y -= hop;
        }
    }
}
