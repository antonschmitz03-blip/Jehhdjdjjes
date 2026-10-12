package com.firemerald.additionalplacements.util;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.stream.Collectors;

public class GeneralUtils {
    public static String makeStateString(BlockState blockState) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(BuiltInRegistries.BLOCK.getKey(blockState.getBlock()));
        if (!blockState.isSingletonState()) {
            stringBuilder.append('[');
            stringBuilder.append(blockState.getValues().map(Property.Value::toString).collect(Collectors.joining(",")));
            stringBuilder.append(']');
        }
        return stringBuilder.toString();
    }

    public static BlockState parseStateString(String blockStateString) {
        try {
            return BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK, blockStateString, false).blockState();
        } catch (CommandSyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
