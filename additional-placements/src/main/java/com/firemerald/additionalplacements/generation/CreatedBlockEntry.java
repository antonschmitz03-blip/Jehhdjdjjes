package com.firemerald.additionalplacements.generation;

import com.firemerald.additionalplacements.block.AdditionalPlacementBlock;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

public record CreatedBlockEntry<T extends Block, U extends AdditionalPlacementBlock<T>>(Identifier originalId, T originalBlock, Identifier newId, U newBlock) {}
