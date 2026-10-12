package com.firemerald.additionalplacements.generation.fabric;

import com.firemerald.additionalplacements.block.AdditionalPlacementBlock;
import com.firemerald.additionalplacements.generation.GenerationType;
import com.firemerald.additionalplacements.generation.Registration;
import com.firemerald.additionalplacements.generation.RegistrationInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.level.block.Block;

public class RegistrationImpl {
    public static void gatherTypes() {
        FabricLoader.getInstance().invokeEntrypoints("additional-placements-generators", RegistrationInitializer.class, Registration::register);
    }

    public static <T extends Block, U extends AdditionalPlacementBlock<T>, V extends GenerationType<T, U>> void addBlacklisters(Class<T> clazz, V type) {
        FabricLoader.getInstance().invokeEntrypoints("additional-placements-generators", RegistrationInitializer.class, instance -> instance.addBlacklisters(clazz, type, blacklister -> type.addBlacklister(blacklister)));
    }
}
