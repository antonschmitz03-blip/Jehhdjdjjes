package com.firemerald.additionalplacements.client.models.fabric;

import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3fc;

public class BlockModelUtilsImpl {
    public static BakedQuad transformed(BakedQuad originalQuad, Vector3fc[] newPos, long[] newTex, Vector3fc @Nullable [] newNorm, int @Nullable [] newColor, Direction direction, BakedQuad.MaterialInfo sprite) {
        return new BakedQuad(
                newPos[0], newPos[1], newPos[2], newPos[3],
                newTex[0], newTex[1], newTex[2], newTex[3],
                direction,
                sprite
        );
    }

    @Nullable
    public static Vector3fc[] getNorms(BakedQuad quad) {
        return null;
    }

    public static int @Nullable [] getColors(BakedQuad quad) {
        return null;
    }
}
