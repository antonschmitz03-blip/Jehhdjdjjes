package com.firemerald.additionalplacements.client;

import com.firemerald.additionalplacements.block.AdditionalPlacementBlock;
import com.firemerald.additionalplacements.util.PlatformUtils;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Set;
import java.util.stream.Collectors;

public record BlockTintSourceWrapper(AdditionalPlacementBlock<?> block, BlockTintSource tintSource) implements BlockTintSource {
	static {
		PlatformUtils.checkIsClient();
	}

    @Override
	public int color(BlockState state) {
		return tintSource.color(block.getModelState(state));
	}

	@Override
	public int colorInWorld(final BlockState state, final BlockAndTintGetter level, final BlockPos pos) {
		return tintSource.colorInWorld(block.getModelState(state), level, pos);
	}

	@Override
	public int colorAsTerrainParticle(final BlockState state, final BlockAndTintGetter level, final BlockPos pos) {
		return tintSource.colorAsTerrainParticle(block.getModelState(state), level, pos);
	}

	@Override
	public Set<Property<?>> relevantProperties() {
		return tintSource.relevantProperties().stream().filter(block::isValidProperty).collect(Collectors.toSet());
	}
}