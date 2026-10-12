package com.firemerald.additionalplacements;

import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AdditionalPlacementsMod {
    public static final String MOD_ID = "additionalplacements";
    public static final Logger LOGGER = LoggerFactory.getLogger("Additional Placements");

    public static Identifier rl(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
