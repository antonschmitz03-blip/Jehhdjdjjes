package io.github.antonschmitz.vplustweaks;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OwnableEntity;

/** Players (and their arrows etc.) can't damage their own tamed pets and horses. */
public final class PetProtection {
    private PetProtection() {}

    public static void register() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof OwnableEntity pet) {
                Entity attacker = source.getEntity();
                if (attacker != null && attacker == pet.getOwner()) {
                    return false;
                }
            }
            return true;
        });
    }
}
