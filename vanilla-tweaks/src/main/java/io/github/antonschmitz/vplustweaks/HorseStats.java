package io.github.antonschmitz.vplustweaks;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.Horse;

/** Every horse has the best possible health, speed and jump strength (the vanilla maximums). */
public final class HorseStats {
    private static final double MAX_HEALTH = 30.0;
    private static final double MAX_SPEED = 0.3375;
    private static final double MAX_JUMP = 1.0;

    private HorseStats() {}

    public static void register() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (entity instanceof Horse horse) {
                boolean healthChanged = set(horse.getAttribute(Attributes.MAX_HEALTH), MAX_HEALTH);
                set(horse.getAttribute(Attributes.MOVEMENT_SPEED), MAX_SPEED);
                set(horse.getAttribute(Attributes.JUMP_STRENGTH), MAX_JUMP);
                if (healthChanged) {
                    horse.setHealth((float) MAX_HEALTH);
                }
            }
        });
    }

    private static boolean set(AttributeInstance attribute, double value) {
        if (attribute == null || attribute.getBaseValue() == value) {
            return false;
        }
        attribute.setBaseValue(value);
        return true;
    }
}
