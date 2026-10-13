package io.github.antonschmitz.tbaddons;

import com.tiviacz.travelersbackpack.attachment.AttachmentUtils;
import com.tiviacz.travelersbackpack.init.ModDataComponents;
import com.tiviacz.travelersbackpack.item.TravelersBackpackItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.ServerLevelAccessor;

/** Natural hostile mob spawns are cancelled near a player wearing a backpack with an enabled Mob Ward Upgrade. */
public final class MobWard {
    /** Vanilla only keeps the inner 24 blocks around a player free of spawns. */
    public static final int RADIUS = 64;
    private static final double RADIUS_SQR = (double) RADIUS * RADIUS;

    private MobWard() {
    }

    public static boolean blocksSpawn(EntityType<?> type, ServerLevelAccessor accessor, EntitySpawnReason reason, BlockPos pos) {
        if (reason != EntitySpawnReason.NATURAL || type.getCategory() != MobCategory.MONSTER) {
            return false;
        }
        ServerLevel level = accessor.getLevel();
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || player.distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5) > RADIUS_SQR) {
                continue;
            }
            if (hasWard(player)) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasWard(Player player) {
        ItemStack backpack = AttachmentUtils.getWearingBackpack(player);
        if (backpack.isEmpty() || !(backpack.getItem() instanceof TravelersBackpackItem)) {
            return false;
        }
        ItemContainerContents upgrades = backpack.get(ModDataComponents.UPGRADES);
        return upgrades != null && upgrades.nonEmptyItemCopyStream().anyMatch(upgrade ->
                upgrade.is(TBAddons.MOB_WARD_UPGRADE) && upgrade.getOrDefault(ModDataComponents.UPGRADE_ENABLED, true));
    }
}
