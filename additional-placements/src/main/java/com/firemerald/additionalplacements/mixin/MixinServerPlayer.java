package com.firemerald.additionalplacements.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.firemerald.additionalplacements.common.IAPServerPlayer;
import com.firemerald.additionalplacements.config.APConfigs;

import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ServerPlayer.class)
public class MixinServerPlayer implements IAPServerPlayer {
	@Unique
    private boolean additionalplacements$placementEnabled = APConfigs.server().fakePlayerPlacement.get();

	@Override
	public boolean additionalplacements$isPlacementEnabled() {
		return additionalplacements$placementEnabled;
	}

	@Override
	public void additionalplacements$setPlacementEnabled(boolean state) {
		this.additionalplacements$placementEnabled = state;
	}
}