package com.firemerald.additionalplacements.client;

import com.mojang.blaze3d.vertex.PoseStack;

import com.mojang.blaze3d.vertex.VertexConsumer;

public class BlockHighlightHelper {
	public static void line(VertexConsumer vertexConsumer, PoseStack.Pose pose, float z, float r, float g, float b, float a, float x1, float y1, float x2, float y2, float width) {
		vertexConsumer.addVertex(pose, x1, y1, z).setColor(r, g, b, a).setNormal(pose, 0, 0, 1).setLineWidth(width);
		vertexConsumer.addVertex(pose, x2, y2, z).setColor(r, g, b, a).setNormal(pose, 0, 0, 1).setLineWidth(width);
	}

	public static void lineAxis(VertexConsumer vertexConsumer, PoseStack.Pose pose, float z, float r, float g, float b, float a, float offset1, float offset2, float width) {
		line(vertexConsumer, pose, z, r, g, b, a,  offset1,  0      ,  offset2,  0      , width);
		line(vertexConsumer, pose, z, r, g, b, a,  0      ,  offset1,  0      ,  offset2, width);
		line(vertexConsumer, pose, z, r, g, b, a, -offset1,  0      , -offset2,  0      , width);
		line(vertexConsumer, pose, z, r, g, b, a,  0      , -offset1,  0      , -offset2, width);
	}

	public static void lineAxis(VertexConsumer vertexConsumer, PoseStack.Pose pose, float z, float r, float g, float b, float a, float offset, float width) {
		lineAxis(vertexConsumer, pose, z, r, g, b, a, 0, offset, width);
	}

	public static void lineAxisDiagonal(VertexConsumer vertexConsumer, PoseStack.Pose pose, float z, float r, float g, float b, float a, float offset1, float offset2, float width) {
		line(vertexConsumer, pose, z, r, g, b, a,  offset1,  offset1,  offset2,  offset2, width);
		line(vertexConsumer, pose, z, r, g, b, a, -offset1,  offset1, -offset2,  offset2, width);
		line(vertexConsumer, pose, z, r, g, b, a, -offset1, -offset1, -offset2, -offset2, width);
		line(vertexConsumer, pose, z, r, g, b, a,  offset1, -offset1,  offset2, -offset2, width);
	}

	public static void lineAxisDiagonal(VertexConsumer vertexConsumer, PoseStack.Pose pose, float z, float r, float g, float b, float a, float offset, float width) {
		lineAxisDiagonal(vertexConsumer, pose, z, r, g, b, a, 0, offset, width);
	}

	public static void lineOctal(VertexConsumer vertexConsumer, PoseStack.Pose pose, float z, float r, float g, float b, float a, float offset1A, float offset1B, float offset2A, float offset2B, float width) {
		line(vertexConsumer, pose, z, r, g, b, a,
				 offset1A,  offset1B,  offset2A,  offset2B,
				width);
		line(vertexConsumer, pose, z, r, g, b, a,
				 offset1B,  offset1A,  offset2B,  offset2A,
				width);
		line(vertexConsumer, pose, z, r, g, b, a,
				-offset1A,  offset1B, -offset2A,  offset2B,
				width);
		line(vertexConsumer, pose, z, r, g, b, a,
				-offset1B,  offset1A, -offset2B,  offset2A,
				width);
		line(vertexConsumer, pose, z, r, g, b, a,
				-offset1A, -offset1B, -offset2A, -offset2B,
				width);
		line(vertexConsumer, pose, z, r, g, b, a,
				-offset1B, -offset1A, -offset2B, -offset2A,
				width);
		line(vertexConsumer, pose, z, r, g, b, a,
				 offset1A, -offset1B,  offset2A, -offset2B,
				width);
		line(vertexConsumer, pose, z, r, g, b, a,
				 offset1B, -offset1A,  offset2B, -offset2A,
				width);
	}

	public static void lineCenteredGrid(VertexConsumer vertexConsumer, PoseStack.Pose pose, float z, float r, float g, float b, float a, float offset1, float offset2, float width) {
		line(vertexConsumer, pose, z, r, g, b, a,
				-offset1, -offset2,
				-offset1,  offset2,
				width);
		line(vertexConsumer, pose, z, r, g, b, a,
				-offset2, -offset1,
				 offset2, -offset1,
				width);
		line(vertexConsumer, pose, z, r, g, b, a,
				 offset1, -offset2,
				 offset1,  offset2,
				width);
		line(vertexConsumer, pose, z, r, g, b, a,
				-offset2,  offset1,
				 offset2,  offset1,
				width);
	}

	public static void lineCenteredSquare(VertexConsumer vertexConsumer, PoseStack.Pose pose, float z, float r, float g, float b, float a, float offset, float width) {
		lineRectangle(vertexConsumer, pose, z, r, g, b, a, -offset, -offset, offset, offset, width);
	}

	public static void lineRectangle(VertexConsumer vertexConsumer, PoseStack.Pose pose, float z, float r, float g, float b, float a, float x1, float y1, float x2, float y2, float width) {
		line(vertexConsumer, pose, z, r, g, b, a,
				x1, y1,
				x2, y1,
				width);
		line(vertexConsumer, pose, z, r, g, b, a,
				x2, y1,
				x2, y2,
				width);
		line(vertexConsumer, pose, z, r, g, b, a,
				x2, y2,
				x1, y2,
				width);
		line(vertexConsumer, pose, z, r, g, b, a,
				x1, y2,
				x1, y1,
				width);
	}

	public static void lineCenteredPlus(VertexConsumer vertexConsumer, PoseStack.Pose pose, float z, float r, float g, float b, float a, float offset, float width) {
		linePlus(vertexConsumer, pose, z, r, g, b, a,
				-offset, -offset,
				 offset,  offset,
				width);
	}

	public static void linePlus(VertexConsumer vertexConsumer, PoseStack.Pose pose, float z, float r, float g, float b, float a, float x1, float y1, float x2, float y2, float width) {
		float xC = (x1 + x2) / 2;
		float yC = (y1 + y2) / 2;
		line(vertexConsumer, pose, z, r, g, b, a,
				x1, yC,
				x2, yC,
				width);
		line(vertexConsumer, pose, z, r, g, b, a,
				xC, y1,
				xC, y2,
				width);
	}

	public static void lineCenteredCross(VertexConsumer vertexConsumer, PoseStack.Pose pose, float z, float r, float g, float b, float a, float offset, float width) {
		lineCross(vertexConsumer, pose, z, r, g, b, a,
				-offset, -offset,
				 offset,  offset,
				width);
	}

	public static void lineCross(VertexConsumer vertexConsumer, PoseStack.Pose pose, float z, float r, float g, float b, float a, float x1, float y1, float x2, float y2, float width) {
		line(vertexConsumer, pose, z, r, g, b, a,
				x1, y1,
				x2, y2,
				width);
		line(vertexConsumer, pose, z, r, g, b, a,
				x2, y1,
				x1, y2,
				width);
	}

	public static void lineList(VertexConsumer vertexConsumer, PoseStack.Pose pose, float z, float r, float g, float b, float a, float width, float... points) {
		for (int i = 0; i < points.length; i += 4) {
			line(vertexConsumer, pose, z, r, g, b, a,
					points[i    ], points[i + 1],
					points[i + 2], points[i + 3],
					width);
		}
	}

	public static void lineStrip(VertexConsumer vertexConsumer, PoseStack.Pose pose, float z, float r, float g, float b, float a, float width, float... points) {
		for (int i = 2; i < points.length; i += 2) {
			line(vertexConsumer, pose, z, r, g, b, a,
					points[i - 2], points[i - 1],
					points[i    ], points[i + 1],
					width);
		}
	}

	public static void lineLoop(VertexConsumer vertexConsumer, PoseStack.Pose pose, float z, float r, float g, float b, float a, float width, float... points) {
		int i1 = 0, i2 = 0;
		do {
			if ((i2 += 2) >= points.length) i2 = 0;
			line(vertexConsumer, pose, z, r, g, b, a,
					points[i1], points[i1 + 1],
					points[i2], points[i2 + 1],
					width);
			i1 = i2;
		} while (i2 > 0);
	}
}
