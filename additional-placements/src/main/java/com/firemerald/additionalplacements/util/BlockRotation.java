package com.firemerald.additionalplacements.util;

import java.util.Map;
import java.util.WeakHashMap;

import com.firemerald.additionalplacements.client.models.BlockModelUtils;

import com.mojang.serialization.Codec;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public enum BlockRotation implements StringRepresentable {
	IDENTITY(Direction.values(), new int[6]) {
		@Override
		public Direction apply(Direction original) {
			return original;
		}

		@Override
		public Direction unapply(Direction original) {
			return original;
		}

		@Override
		public int getVertexShiftLeft(Direction original) {
			return 0;
		}

		@Override
		public void shiftVertices(Direction original, Vector3fc[] sourcePos, long[] sourceTex, @Nullable Vector3fc[] sourceNorms, int @Nullable [] sourceColors, Vector3fc[] desPos, long[] desTex, @Nullable Vector3fc[] desNorms, int @Nullable [] desColors) {
			System.arraycopy(sourcePos, 0, desPos, 0, sourcePos.length);
			System.arraycopy(sourceTex, 0, desTex, 0, sourceTex.length);
			if (sourceNorms != null) System.arraycopy(sourceNorms, 0, desNorms, 0, sourceNorms.length);
			if (sourceColors != null) System.arraycopy(sourceColors, 0, desColors, 0, sourceColors.length);
		}

		@Override
		public void applyBlockSpace(float[] vertex) {}

		@Override
		public Vector3fc rotatePos(Vector3fc pos) {
			return pos;
		}

		@Override
		public Vector3fc rotateNorm(Vector3fc norm) {
			return norm;
		}

		@Override
		public VoxelShape applyBlockSpace(VoxelShape shape) {
			return shape;
		}

		@Override
		public VoxelShape createRotatedBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
			return Shapes.create(minX, minY, minZ, maxX, maxY, maxZ);
		}
	},
	X_90(new Direction[] {
			Direction.SOUTH, Direction.NORTH,
			Direction.DOWN, Direction.UP,
			Direction.WEST, Direction.EAST,
	}, new int[] {
			0, 2,
			2, 0,
			3, 1
	}) {
		//x = x
		//y = z
		//z = -y
		@Override
		public void applyBlockSpace(float[] vertex) {
			float temp = vertex[1];
			vertex[1] = vertex[2];
			vertex[2] = 1 - temp;
		}

		@Override
		public Vector3fc rotatePos(Vector3fc pos) {
			return new Vector3f(
					pos.x(),
					pos.z(),
					1 - pos.y()
			);
		}

		@Override
		public Vector3fc rotateNorm(Vector3fc norm) {
			return new Vector3f(
					norm.x(),
					norm.z(),
					-norm.y()
			);
		}

		@Override
		public VoxelShape createRotatedBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
			return Shapes.create(
	        		minX,
	        		minZ,
	        		1 - maxY,
	        		maxX,
	        		maxZ,
	        		1 - minY);
		}
	},
	X_270(new Direction[] {
			Direction.NORTH, Direction.SOUTH,
			Direction.UP, Direction.DOWN,
			Direction.WEST, Direction.EAST,
	}, new int[] {
			2, 0,
			2, 0,
			1, 3
	}) {
		//x = x
		//y = -z
		//z = y
		@Override
		public void applyBlockSpace(float[] vertex) {
			float temp = vertex[1];
			vertex[1] = 1 - vertex[2];
			vertex[2] = temp;
		}

		@Override
		public Vector3fc rotatePos(Vector3fc pos) {
			return new Vector3f(
					pos.x(),
					1 - pos.z(),
					pos.y()
			);
		}

		@Override
		public Vector3fc rotateNorm(Vector3fc norm) {
			return new Vector3f(
					norm.x(),
					-norm.z(),
					norm.y()
			);
		}

		@Override
		public VoxelShape createRotatedBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
			return Shapes.create(
	        		minX,
	        		1 - maxZ,
	        		minY,
	        		maxX,
	        		1 - minZ,
	        		maxY);
		}
	},
	X_270_Y_90(new Direction[] {
			Direction.EAST, Direction.WEST,
			Direction.UP, Direction.DOWN,
			Direction.NORTH, Direction.SOUTH,
	}, new int[] {
			2, 0,
			3, 3,
			1, 3
	}) {
		//x = -y
		//y = -z
		//z = x
		@Override
		public void applyBlockSpace(float[] vertex) {
			float temp = vertex[0];
			vertex[0] = 1 - vertex[1];
			vertex[1] = 1 - vertex[2];
			vertex[2] = temp;
		}

		@Override
		public Vector3fc rotatePos(Vector3fc pos) {
			return new Vector3f(
					1 - pos.y(),
					1 - pos.z(),
					pos.x()
			);
		}

		@Override
		public Vector3fc rotateNorm(Vector3fc norm) {
			return new Vector3f(
					-norm.y(),
					-norm.z(),
					norm.x()
			);
		}

		@Override
		public VoxelShape createRotatedBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
			return Shapes.create(
	        		1 - maxY,
	        		1 - maxZ,
	        		minX,
	        		1 - minY,
	        		1 - minZ,
	        		maxX);
		}
	},
	X_270_Y_180(new Direction[] {
			Direction.SOUTH, Direction.NORTH,
			Direction.UP, Direction.DOWN,
			Direction.EAST, Direction.WEST,
	}, new int[] {
			2, 0,
			0, 2,
			1, 3
	}) {
		//x = -x
		//y = -z
		//z = -y
		@Override
		public void applyBlockSpace(float[] vertex) {
			vertex[0] = 1 - vertex[0];
			float temp = vertex[1];
			vertex[1] = 1 - vertex[2];
			vertex[2] = 1 - temp;
		}

		@Override
		public Vector3fc rotatePos(Vector3fc pos) {
			return new Vector3f(
					1 - pos.x(),
					1 - pos.z(),
					1 - pos.y()
			);
		}

		@Override
		public Vector3fc rotateNorm(Vector3fc norm) {
			return new Vector3f(
					-norm.x(),
					-norm.z(),
					-norm.y()
			);
		}

		@Override
		public VoxelShape createRotatedBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
			return Shapes.create(
	        		1 - maxX,
	        		1 - maxZ,
	        		1 - maxY,
	        		1 - minX,
	        		1 - minZ,
	        		1 - minY);
		}
	},
	X_270_Y_270(new Direction[] {
			Direction.WEST, Direction.EAST,
			Direction.UP, Direction.DOWN,
			Direction.SOUTH, Direction.NORTH,
	}, new int[] {
			2, 0,
			1, 1,
			1, 3
	}) {
		//x = y
		//y = -z
		//z = -x
		@Override
		public void applyBlockSpace(float[] vertex) {
			float temp = vertex[0];
			vertex[0] = vertex[1];
			vertex[1] = 1 - vertex[2];
			vertex[2] = 1 - temp;
		}

		@Override
		public Vector3fc rotatePos(Vector3fc pos) {
			return new Vector3f(
					pos.y(),
					1 - pos.z(),
					1 - pos.x()
			);
		}

		@Override
		public Vector3fc rotateNorm(Vector3fc norm) {
			return new Vector3f(
					norm.y(),
					1-norm.z(),
					1-norm.x()
			);
		}

		@Override
		public VoxelShape createRotatedBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
			return Shapes.create(
	        		minY,
	        		1 - maxZ,
	        		1 - maxX,
	        		maxY,
	        		1 - minZ,
	        		1 - minX);
		}
	},
	X_180(new Direction[] {
			Direction.UP, Direction.DOWN,
			Direction.SOUTH, Direction.NORTH,
			Direction.WEST, Direction.EAST,
	}, new int[] {
			0, 0,
			2, 2,
			2, 2
	}) {
		//x = x
		//y = -y
		//z = -z
		@Override
		public void applyBlockSpace(float[] vertex) {
			vertex[1] = 1 - vertex[1];
			vertex[2] = 1 - vertex[2];
		}

		@Override
		public Vector3fc rotatePos(Vector3fc pos) {
			return new Vector3f(
					pos.x(),
					1 - pos.y(),
					1 - pos.z()
			);
		}

		@Override
		public Vector3fc rotateNorm(Vector3fc norm) {
			return new Vector3f(
					norm.x(),
					-norm.y(),
					-norm.z()
			);
		}

		@Override
		public VoxelShape createRotatedBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
			return Shapes.create(
	        		minX,
	        		1 - maxY,
	        		1 - maxZ,
	        		maxX,
	        		1 - minY,
	        		1 - minZ);
		}
	};

	public static final Codec<BlockRotation> CODEC = new StringRepresentable.EnumCodec<>(values(), BlockRotation::valueOf);

	private final Map<VoxelShape, VoxelShape> shapeCache = new WeakHashMap<>(); //we cache these values, to avoid overhead, but weakly in case of dynamically computed shapes
	private final Direction[] applyDirection, unapplyDirection;
	private final int[] vertexShiftLeft;

	BlockRotation(Direction[] appliedDirection, int[] vertexShiftLeft) {
		applyDirection = appliedDirection;
		unapplyDirection = new Direction[6];
		for (int i = 0; i < 6; ++i) unapplyDirection[applyDirection[i].get3DDataValue()] = Direction.from3DDataValue(i);
		this.vertexShiftLeft = vertexShiftLeft;
	}

	@Override
	@NotNull
	public String getSerializedName() {
		return this.name();
	}

	public Direction apply(Direction original) {
		return original == null ? null : applyDirection[original.get3DDataValue()];
	}

	public Direction unapply(Direction original) {
		return original == null ? null : unapplyDirection[original.get3DDataValue()];
	}

	public int getVertexShiftLeft(Direction original) {
		return original == null ? 0 : vertexShiftLeft[original.get3DDataValue()];
	}

	public void shiftVertices(Direction original, Vector3fc[] sourcePos, long[] sourceTex, @Nullable Vector3fc[] sourceNorms, int @Nullable [] sourceColors, Vector3fc[] desPos, long[] desTex, @Nullable Vector3fc[] desNorms, int @Nullable [] desColors) {
		int shiftLeft = getVertexShiftLeft(original);
		BlockModelUtils.shiftData(sourcePos, desPos, sourcePos.length, shiftLeft);
		BlockModelUtils.shiftData(sourceTex, desTex, sourceTex.length, shiftLeft);
		if (sourceNorms != null) BlockModelUtils.shiftData(sourceNorms, desNorms, sourceNorms.length, shiftLeft);
		if (sourceColors != null) BlockModelUtils.shiftData(sourceColors, desColors, sourceColors.length, shiftLeft);
	}

	public void rotateVertices(Direction original,
							   Vector3fc[] sourcePos, long[] sourceTex, @Nullable Vector3fc[] sourceNorms, int @Nullable [] sourceColors,
							   Vector3fc[] desPos, long[] desTex, @Nullable Vector3fc[] desNorms, int @Nullable [] desColors,
							   boolean rotateUV, TextureAtlasSprite tex) {
		int shiftLeft = getVertexShiftLeft(original);
		rotateUV &= shiftLeft != 0;
		if (!rotateUV) BlockModelUtils.shiftData(sourceTex, desTex, sourceTex.length, shiftLeft);
		if (sourceColors != null) BlockModelUtils.shiftData(sourceColors, desColors, sourceColors.length, shiftLeft);
		int sourceIndex = shiftLeft;
		for (int desIndex = 0; desIndex < sourcePos.length; desIndex++) {
			desPos[desIndex] = rotatePos(sourcePos[sourceIndex]);
			if (rotateUV) desTex[desIndex] = rotateUV(sourceTex[sourceIndex], shiftLeft, tex);
			if (sourceNorms != null) desNorms[desIndex] = rotateNorm(sourceNorms[sourceIndex]);
			sourceIndex++;
			if (sourceIndex >= sourcePos.length) sourceIndex = 0;
		}
	}

	public abstract void applyBlockSpace(float[] vertex);

	public abstract Vector3fc rotatePos(Vector3fc pos);

	public abstract Vector3fc rotateNorm(Vector3fc norm);

	public long rotateUV(long packedUV, int rotateUV, TextureAtlasSprite tex) {
		if (rotateUV == 0) return packedUV;
		float sourceU = BlockModelUtils.getU(packedUV);
		float sourceV = BlockModelUtils.getV(packedUV);
		float desU, desV;
		switch (rotateUV) {
			case 1:
				desU = tex.getU(1 - BlockModelUtils.getVOffset(tex, sourceV)); //1-V
				desV = tex.getV(BlockModelUtils.getUOffset(tex, sourceU)); //U
				break;
			case 2:
				desU = tex.getU0() + tex.getU1() - sourceU; //quick 1-U
				desV = tex.getV0() + tex.getV1() - sourceV; //quick 1-V
				break;
			case 3:
				desU = tex.getU(BlockModelUtils.getVOffset(tex, sourceV)); //V
				desV = tex.getV(1 - BlockModelUtils.getUOffset(tex, sourceU)); //1 - U
				break;
			default:
				throw new IllegalStateException("Invalid rotateUV value - must be in range [0,3], got " + rotateUV);
		}
		return BlockModelUtils.packUV(desU, desV);
	}

	public VoxelShape applyBlockSpace(VoxelShape shape) {
		return shapeCache.computeIfAbsent(shape, nil -> {
	        VoxelShape[] buffer = new VoxelShape[] { Shapes.empty() };
	        shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> buffer[0] = Shapes.or(buffer[0], createRotatedBox(minX, minY, minZ, maxX, maxY, maxZ)));
	        return buffer[0];
		});
	}

	protected abstract VoxelShape createRotatedBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ);
}
