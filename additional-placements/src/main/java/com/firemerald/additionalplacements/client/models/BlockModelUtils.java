package com.firemerald.additionalplacements.client.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.firemerald.additionalplacements.util.PlatformUtils;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.data.AtlasIds;

import com.firemerald.additionalplacements.block.AdditionalPlacementBlock;
import com.firemerald.additionalplacements.util.BlockRotation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class BlockModelUtils {
	static {
		PlatformUtils.checkIsClient();
	}

	public static BlockState getModeledState(BlockState state) {
		if (state != null && state.getBlock() instanceof AdditionalPlacementBlock<?> block) return block.getModelState(state);
		else return state;
	}

	public static BakedQuad retexture(BakedQuad jsonBakedQuad, BakedQuad.MaterialInfo newSprite) {
		return transformed(
				jsonBakedQuad,
				(oldPos, oldTex, oldNorms, oldColors, newPos, newTex, newNorms, newColors) -> {
					System.arraycopy(oldPos, 0, newPos, 0, oldPos.length);
					updateUVs(oldTex, newTex, jsonBakedQuad.materialInfo().sprite(), newSprite.sprite());
					if (oldNorms != null) System.arraycopy(oldNorms, 0, newNorms, 0, oldNorms.length);
					if (oldColors != null) System.arraycopy(oldColors, 0, newColors, 0, oldColors.length);
				},
				jsonBakedQuad.direction(),
				newSprite
		);
	}

	@FunctionalInterface
	public interface QuadTransform {
		void transform(Vector3fc[] sourcePos, long[] sourceTex, Vector3fc @Nullable [] sourceNorms, int @Nullable [] sourceCols, Vector3fc[] desPos, long[] desTex, Vector3fc @Nullable [] desNorms, int @Nullable [] desCols);
	}

	public static BakedQuad transformed(BakedQuad originalQuad, QuadTransform quadTransform, Direction direction, BakedQuad.MaterialInfo sprite) {
		Vector3fc[] oldPos = getVertices(originalQuad);
		long[] oldTex = getUVs(originalQuad);
		Vector3fc @Nullable [] oldNorms = getNorms(originalQuad);
		int @Nullable [] oldColors = getColors(originalQuad);
		Vector3fc[] newPos = oldPos.clone();
		long[] newTex = oldTex.clone();
		Vector3fc[] newNorms = oldNorms == null ? null : oldNorms.clone();
		int[] newColors = oldColors == null ? null : oldColors.clone();
		quadTransform.transform(oldPos, oldTex, oldNorms, oldColors, newPos, newTex, newNorms, newColors);
		return transformed(originalQuad, newPos, newTex, newNorms, newColors, direction, sprite);
	}

		public static BakedQuad transformed(BakedQuad originalQuad, Vector3fc[] newPos, long[] newTex, Vector3fc @Nullable [] newNorm, int @Nullable [] newColor, Direction direction, BakedQuad.MaterialInfo sprite) {
		return com.firemerald.additionalplacements.client.models.fabric.BlockModelUtilsImpl.transformed(originalQuad, newPos, newTex, newNorm, newColor, direction, sprite);
	}

	public static final Vector3fc ZERO_POINT = new Vector3f();

	public static float getFaceSize(Vector3fc[] vertices) {
		Vector3f first = newVertex(vertices, 0);
		Vector3f prev = new Vector3f();
		Vector3f cur = newVertex(vertices, 1, first);
		float size = 0;
		for (int vertexIndex = 2; vertexIndex < vertices.length; vertexIndex++) {
			Vector3f tmp = prev;
			prev = cur;
			cur = getVertex(vertices, vertexIndex, first, tmp);
			size += getArea(prev, cur);
		}
		return size;
	}

	public static Vector3f newVertex(Vector3fc[] vertices, int vertexIndex) {
		return newVertex(vertices, vertexIndex, ZERO_POINT);
	}

	public static Vector3f newVertex(Vector3fc[] vertices, int vertexIndex, Vector3fc origin) {
		return new Vector3f(vertices[vertexIndex]).sub(origin);
	}

	public static Vector3f getVertex(Vector3fc[] vertices, int vertexIndex, Vector3f des) {
		return getVertex(vertices, vertexIndex, ZERO_POINT, des);
	}

	public static Vector3f getVertex(Vector3fc[] vertices, int vertexIndex, Vector3fc origin, Vector3f des) {
		return vertices[vertexIndex].get(des).sub(origin);
	}

	public static float getArea(Vector3f ab, Vector3f ac) {
		return .5f * Mth.sqrt(
				Mth.square(ab.x * ac.y - ab.y * ac.x) +
				Mth.square(ab.y * ac.z - ab.z * ac.y) +
				Mth.square(ab.z * ac.x - ab.x * ac.z)
				);
	}

	public static void updateUVs(long[] source, long[] des, TextureAtlasSprite oldSprite, TextureAtlasSprite newSprite) {
		for (int uvIndex = 0; uvIndex < source.length; uvIndex++) {
			des[uvIndex] = changeUVSprite(oldSprite, newSprite, source[uvIndex]);
	    }
	}

	private static long changeUVSprite(TextureAtlasSprite oldSprite, TextureAtlasSprite newSprite, long packedUV) {
		float u = getU(packedUV);
		float v = getV(packedUV);
		return packUV(
				newSprite.getU(getUOffset(oldSprite, getU(packedUV))),
				newSprite.getV(getVOffset(oldSprite, getV(packedUV)))
		);
	}

	private static int changeUVertexElementSprite(TextureAtlasSprite oldSprite, TextureAtlasSprite newSprite, int vertex) {
		return Float.floatToRawIntBits(newSprite.getU(BlockModelUtils.getUOffset(oldSprite, Float.intBitsToFloat(vertex))));
	}

	private static int changeVVertexElementSprite(TextureAtlasSprite oldSprite, TextureAtlasSprite newSprite, int vertex) {
		return Float.floatToRawIntBits(newSprite.getV(BlockModelUtils.getVOffset(oldSprite, Float.intBitsToFloat(vertex))));
	}

	@SuppressWarnings("SuspiciousSystemArraycopy")
    public static <T> void shiftData(T source, T des, int size, int shiftLeft) {
		//shiftLeft %= originalData.length;
		if (shiftLeft == 0) {
			System.arraycopy(source, 0, des, 0, size); //direct copy
		} else {
			int lengthRight = size - shiftLeft;
			System.arraycopy(source, shiftLeft, des, 0, lengthRight); //copy [middle to end] to [start to middle]
			System.arraycopy(source, 0, des, lengthRight, shiftLeft); //copy [start to middle] to [middle to end]
		}
	}

	public static BakedQuad.MaterialInfo getSidedTexture(List<BlockStateModelPart> fromModel, Direction fromSide) {
		Map<BakedQuad.MaterialInfo, Double> weights = new HashMap<>();
		List<BakedQuad> referenceQuads = fromModel.stream().flatMap(part -> part.getQuads(fromSide).stream()).toList();
		if (fromSide != null && (referenceQuads.isEmpty() || referenceQuads.stream().noneMatch(quad -> quad.direction() == fromSide))) //no valid culled sides
			referenceQuads = fromModel.stream().flatMap(part -> part.getQuads(null).stream()).toList();
		if (!referenceQuads.isEmpty()) {
			referenceQuads.forEach(referredBakedQuad -> {
				if (fromSide == null || referredBakedQuad.direction() == fromSide) { //only for quads facing the correct side
					BakedQuad.MaterialInfo tex = referredBakedQuad.materialInfo();
					weights.merge(tex, (double) BlockModelUtils.getFaceSize(getVertices(referredBakedQuad)), Double::sum);
				}
			});
			return weights.entrySet().stream().max((e1, e2) -> (int) Math.signum(e2.getValue() - e1.getValue())).map(Map.Entry::getKey).orElse(
					missingTex()
			);
		}
		else return missingTex();
	}

	public static BakedQuad.MaterialInfo missingTex() {
		return new BakedQuad.MaterialInfo(Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).missingSprite(), ChunkSectionLayer.SOLID, Sheets.cutoutBlockItemSheet(), Sheets.cutoutBlockItemGlintSheet(), Sheets.cutoutBlockItemGlintSpecialSheet(), -1, null, 0);
	}

	public static List<BakedQuad> retexturedQuads(List<BlockStateModelPart> originalModel, BlockStateModelPart ourModel, Direction side) {
		BakedQuad.MaterialInfo[] textures = new BakedQuad.MaterialInfo[6];
		List<BakedQuad> originalQuads = ourModel.getQuads(side);
		List<BakedQuad> bakedQuads = new ArrayList<>(originalQuads.size());
		for (BakedQuad originalQuad : originalQuads) {
			Direction modelSide = originalQuad.direction();
			int dirIndex = modelSide.get3DDataValue();
			BakedQuad.MaterialInfo texture = textures[dirIndex];
			if (texture == null) texture = textures[dirIndex] = getSidedTexture(originalModel, modelSide);
    		bakedQuads.add(retexture(originalQuad, texture));
		}
		return bakedQuads;
	}

	public static List<BakedQuad> rotatedQuads(BlockStateModelPart model, BlockRotation rotation, boolean rotateTex, Direction side) {
		List<BakedQuad> originalQuads = model.getQuads(rotation.unapply(side));
		List<BakedQuad> bakedQuads = new ArrayList<>(originalQuads.size());
		for (BakedQuad originalQuad : originalQuads) {
    		bakedQuads.add(transformed(
					originalQuad,
					(oldPos, oldTex, oldNorms, oldColors, newPos, newTex, newNorms, newColors) ->
							rotation.rotateVertices(originalQuad.direction(), oldPos, oldTex, oldNorms, oldColors, newPos, newTex, newNorms, newColors, rotateTex, originalQuad.materialInfo().sprite()),
					rotation.apply(originalQuad.direction()),
    				originalQuad.materialInfo()));
		}
		return bakedQuads;
	}

	public static float getUOffset(TextureAtlasSprite sprite, float offset) {
		float f = sprite.getU1() - sprite.getU0();
		return (offset - sprite.getU0()) / f;
	}

	public static float getVOffset(TextureAtlasSprite sprite, float offset) {
		float f = sprite.getV1() - sprite.getV0();
		return (offset - sprite.getV0()) / f;
	}

	public static Vector3fc[] getVertices(BakedQuad quad) {
		return new Vector3fc[] {
				quad.position0(),
				quad.position1(),
				quad.position2(),
				quad.position3()
		};
	}

	public static long[] getUVs(BakedQuad quad) {
		return new long[] {
				quad.packedUV0(),
				quad.packedUV1(),
				quad.packedUV2(),
				quad.packedUV3()
		};
	}

		@Nullable
	public static Vector3fc[] getNorms(BakedQuad quad) {
		return com.firemerald.additionalplacements.client.models.fabric.BlockModelUtilsImpl.getNorms(quad);
	}

	    public static int @Nullable [] getColors(BakedQuad quad) {
		return com.firemerald.additionalplacements.client.models.fabric.BlockModelUtilsImpl.getColors(quad);
	}

	public static float getU(long packedUV) {
		return Float.intBitsToFloat((int) ((packedUV & 0xFFFFFFFF00000000L) >> 32));
	}

	public static float getV(long packedUV) {
		return Float.intBitsToFloat((int) (packedUV & 0x00000000FFFFFFFFL));
	}

	public static long packUV(float u, float v) {
		return ((long) Float.floatToRawIntBits(v)) | (((long) Float.floatToRawIntBits(u)) << 32);
	}
}