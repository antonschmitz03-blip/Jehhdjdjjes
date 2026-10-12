package com.firemerald.additionalplacements.mixin;

public class BlockMethods {
    public static final String
            GET_STATE_FOR_PLACEMENT_DESC = "(Lnet/minecraft/world/item/context/BlockPlaceContext;)Lnet/minecraft/world/level/block/state/BlockState;",
            GET_STATE_FOR_PLACEMENT_NAME = "getStateForPlacement" + GET_STATE_FOR_PLACEMENT_DESC,
            GET_STATE_FOR_PLACEMENT_OBF_NAME = "m_5573_" + GET_STATE_FOR_PLACEMENT_DESC,

            ROTATE_DESC = "(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/Rotation;)Lnet/minecraft/world/level/block/state/BlockState;",
            ROTATE_NAME = "rotate" + ROTATE_DESC,
            ROTATE_OBF_NAME = "m_6843_" + ROTATE_DESC,

            MIRROR_DESC = "(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/Mirror;)Lnet/minecraft/world/level/block/state/BlockState;",
            MIRROR_NAME = "mirror" + MIRROR_DESC,
            MIRROR_OBF_NAME = "m_6943_" + MIRROR_DESC,

            UPDATE_SHAPE_DESC = "(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/world/level/ScheduledTickAccess;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/util/RandomSource;)Lnet/minecraft/world/level/block/state/BlockState;",
            UPDATE_SHAPE_NAME = "updateShape" + UPDATE_SHAPE_DESC;


}
