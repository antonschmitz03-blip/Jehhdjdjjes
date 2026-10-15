package io.github.antonschmitz.happyghastplus.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.antonschmitz.happyghastplus.GhastSearchlight;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The parts of the searchlight that are only drawn on screen: a faint cone from the lens to where it points, and a soft
 * round glow on the ground there. The glow glides smoothly while the real light blocks follow in small steps.
 */
public final class SearchlightBeamRenderer {
    private static final double RANGE = 160.0;
    private static final int SEGMENTS = 12;
    private static final float LENS_RADIUS = 0.12F;
    private static final double GLOW_RADIUS = 7.0;
    private static final int BEAM_ALPHA = 70;
    private static final int GLOW_ALPHA = 55;
    private static final int RED = 255;
    private static final int GREEN = 232;
    private static final int BLUE = 176;

    private SearchlightBeamRenderer() {}

    public static void register() {
        LevelRenderEvents.COLLECT_SUBMITS.register(SearchlightBeamRenderer::render);
    }

    private static void render(LevelRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        ClientLevel level = client.level;
        if (level == null || client.player == null) {
            return;
        }
        float partialTick = context.levelState().worldPartialTicks;
        Vec3 camera = context.levelState().cameraRenderState.pos;
        for (HappyGhast ghast : level.getEntitiesOfClass(HappyGhast.class, client.player.getBoundingBox().inflate(RANGE),
                g -> g.getAttachedOrElse(GhastSearchlight.ON, false))) {
            SearchlightAim aim = SearchlightAim.of(ghast, partialTick);
            if (!aim.on() || aim.target() == null) {
                continue;
            }
            PoseStack poseStack = context.poseStack();
            poseStack.pushPose();
            poseStack.translate(-camera.x, -camera.y, -camera.z);
            Vec3 lens = aim.lens();
            Vec3 target = aim.target();
            context.submitNodeCollector().submitCustomGeometry(poseStack, RenderTypes.lightning(), (pose, consumer) -> {
                beam(pose, consumer, lens, target);
                if (aim.normal() != null && aim.normal().y > 0.5) {
                    glow(pose, consumer, level, target);
                }
            });
            poseStack.popPose();
        }
    }

    /** A cone of quads, bright at the lens and fading to nothing at the far end. */
    private static void beam(PoseStack.Pose pose, VertexConsumer consumer, Vec3 from, Vec3 to) {
        Vec3 axis = to.subtract(from);
        double length = axis.length();
        if (length < 0.5) {
            return;
        }
        Vec3 dir = axis.scale(1.0 / length);
        Vec3 side = Math.abs(dir.y) < 0.9 ? dir.cross(new Vec3(0, 1, 0)).normalize() : dir.cross(new Vec3(1, 0, 0)).normalize();
        Vec3 up = side.cross(dir).normalize();
        float farRadius = (float) Math.min(2.5, 0.25 + length * 0.05);
        for (int i = 0; i < SEGMENTS; i++) {
            double a0 = Math.PI * 2 * i / SEGMENTS;
            double a1 = Math.PI * 2 * (i + 1) / SEGMENTS;
            Vec3 n0 = side.scale(Math.cos(a0)).add(up.scale(Math.sin(a0)));
            Vec3 n1 = side.scale(Math.cos(a1)).add(up.scale(Math.sin(a1)));
            Vec3 p0 = from.add(n0.scale(LENS_RADIUS));
            Vec3 p1 = from.add(n1.scale(LENS_RADIUS));
            Vec3 q1 = to.add(n1.scale(farRadius));
            Vec3 q0 = to.add(n0.scale(farRadius));
            // both windings, so the cone shows from inside and outside
            quad(pose, consumer, p0, p1, q1, q0, BEAM_ALPHA, BEAM_ALPHA, 0, 0);
            quad(pose, consumer, p0, q0, q1, p1, BEAM_ALPHA, 0, 0, BEAM_ALPHA);
        }
    }

    /** Soft round light on the tops of the blocks around the target, fading out towards the edge. */
    private static void glow(PoseStack.Pose pose, VertexConsumer consumer, ClientLevel level, Vec3 target) {
        int radius = (int) Math.ceil(GLOW_RADIUS);
        BlockPos centre = BlockPos.containing(target);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double cx = centre.getX() + dx + 0.5 - target.x;
                double cz = centre.getZ() + dz + 0.5 - target.z;
                if (cx * cx + cz * cz > (GLOW_RADIUS + 0.75) * (GLOW_RADIUS + 0.75)) {
                    continue;
                }
                double top = surface(level, centre.offset(dx, 0, dz));
                if (Double.isNaN(top)) {
                    continue;
                }
                double x0 = centre.getX() + dx;
                double z0 = centre.getZ() + dz;
                Vec3 a = new Vec3(x0, top, z0);
                Vec3 b = new Vec3(x0, top, z0 + 1);
                Vec3 c = new Vec3(x0 + 1, top, z0 + 1);
                Vec3 d = new Vec3(x0 + 1, top, z0);
                quad(pose, consumer, a, b, c, d, glowAlpha(a, target), glowAlpha(b, target), glowAlpha(c, target), glowAlpha(d, target));
            }
        }
    }

    private static int glowAlpha(Vec3 corner, Vec3 target) {
        double dx = corner.x - target.x;
        double dz = corner.z - target.z;
        double f = Math.max(0.0, 1.0 - (dx * dx + dz * dz) / (GLOW_RADIUS * GLOW_RADIUS));
        return (int) (GLOW_ALPHA * f * f);
    }

    /** Height just above the top surface of the column near {@code pos}, or NaN if there is none close by. */
    private static double surface(ClientLevel level, BlockPos pos) {
        for (int dy = 2; dy >= -4; dy--) {
            BlockPos at = pos.offset(0, dy, 0);
            BlockState state = level.getBlockState(at);
            VoxelShape shape = state.getCollisionShape(level, at);
            if (!shape.isEmpty() && level.getBlockState(at.above()).getCollisionShape(level, at.above()).isEmpty()) {
                return at.getY() + shape.max(Direction.Axis.Y) + 0.02;
            }
        }
        return Double.NaN;
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer consumer, Vec3 a, Vec3 b, Vec3 c, Vec3 d,
                             int alphaA, int alphaB, int alphaC, int alphaD) {
        vertex(pose, consumer, a, alphaA);
        vertex(pose, consumer, b, alphaB);
        vertex(pose, consumer, c, alphaC);
        vertex(pose, consumer, d, alphaD);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer consumer, Vec3 p, int alpha) {
        consumer.addVertex(pose, (float) p.x, (float) p.y, (float) p.z).setColor(RED, GREEN, BLUE, alpha);
    }
}
