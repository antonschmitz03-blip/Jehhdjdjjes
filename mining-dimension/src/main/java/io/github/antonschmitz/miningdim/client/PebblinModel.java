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

/** A round little rock on stubby legs with a crystal cluster on its back. */
public class PebblinModel extends QuadrupedModel<PebblinRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(MiningDimension.id("pebblin"), "main");

    public PebblinModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -3.0F, -4.0F, 6.0F, 5.0F, 4.0F),
                PartPose.offset(0.0F, 18.0F, -5.0F));
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 10).addBox(-4.0F, -6.0F, -5.0F, 8.0F, 6.0F, 10.0F),
                PartPose.offset(0.0F, 21.0F, 0.0F));
        body.addOrReplaceChild("crystal_big",
                CubeListBuilder.create().texOffs(40, 8).addBox(-1.0F, -4.0F, -1.0F, 2.0F, 4.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, -6.0F, 0.5F, -0.15F, 0.0F, 0.1F));
        body.addOrReplaceChild("crystal_mid",
                CubeListBuilder.create().texOffs(40, 16).addBox(-1.0F, -3.0F, -1.0F, 2.0F, 3.0F, 2.0F),
                PartPose.offsetAndRotation(2.0F, -6.0F, 2.5F, 0.35F, 0.0F, 0.4F));
        body.addOrReplaceChild("crystal_small",
                CubeListBuilder.create().texOffs(50, 16).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(-2.0F, -6.0F, -1.5F, -0.3F, 0.0F, -0.45F));
        CubeListBuilder leg = CubeListBuilder.create().texOffs(40, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F);
        root.addOrReplaceChild("right_hind_leg", leg, PartPose.offset(-2.5F, 21.0F, 3.0F));
        root.addOrReplaceChild("left_hind_leg", leg, PartPose.offset(2.5F, 21.0F, 3.0F));
        root.addOrReplaceChild("right_front_leg", leg, PartPose.offset(-2.5F, 21.0F, -3.0F));
        root.addOrReplaceChild("left_front_leg", leg, PartPose.offset(2.5F, 21.0F, -3.0F));
        return LayerDefinition.create(mesh, 64, 32);
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
