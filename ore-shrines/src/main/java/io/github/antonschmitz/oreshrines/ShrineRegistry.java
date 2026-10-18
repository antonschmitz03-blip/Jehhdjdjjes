package io.github.antonschmitz.oreshrines;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Loaded shrines per level, so the spawn check can ask "is this spot warded?" without searching the world. */
public final class ShrineRegistry {
    private static final Map<Level, Set<ShrineBlockEntity>> SHRINES = new IdentityHashMap<>();

    private ShrineRegistry() {
    }

    static synchronized void add(Level level, ShrineBlockEntity shrine) {
        SHRINES.computeIfAbsent(level, l -> Collections.newSetFromMap(new IdentityHashMap<>())).add(shrine);
    }

    static synchronized void remove(Level level, ShrineBlockEntity shrine) {
        Set<ShrineBlockEntity> shrines = SHRINES.get(level);
        if (shrines != null) {
            shrines.remove(shrine);
        }
    }

    static synchronized void clear() {
        SHRINES.clear();
    }

    public static synchronized boolean isWarded(Level level, BlockPos pos) {
        Set<ShrineBlockEntity> shrines = SHRINES.get(level);
        if (shrines == null) {
            return false;
        }
        for (ShrineBlockEntity shrine : shrines) {
            if (shrine.effect() == ShrineEffect.WARD && shrine.inRange(pos)) {
                return true;
            }
        }
        return false;
    }
}
