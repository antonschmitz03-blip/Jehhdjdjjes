package com.firemerald.additionalplacements.generation;

import java.util.function.Consumer;

import com.firemerald.additionalplacements.block.AdditionalPlacementBlock;

import net.minecraft.world.level.block.Block;

/**
 * A mod developer can use this to register new {@link GenerationType generation types} and/or {@link IBlockBlacklister block blacklisters}</br>
 * If on Fabric, you do this by adding an instance of this to the {@code additional-placements-generators} entrypoint in your {@code fabric.mod.json}.</br>
 * If on Forge, you do this by registering an instance of this via {@link Registration#addRegistration(RegistrationInitializer)} during your mod construction.</br>
 * Please note is it incorrect to register these in any other way at any other point, and doing so may cause an exception to be thrown!
 */
public interface RegistrationInitializer {
	default void onInitializeRegistration(IRegistration register) {}

	default void addGlobalBlacklisters(Consumer<IBlockBlacklister<Block>> register) {}

	default <T extends Block, U extends AdditionalPlacementBlock<T>> void addBlacklisters(Class<T> type, GenerationType<T, U> generationType, Consumer<IBlockBlacklister<? super T>> register) {}
}
