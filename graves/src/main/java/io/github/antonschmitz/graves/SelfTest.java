package io.github.antonschmitz.graves;

import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * CI smoke test: kills a fake player carrying items and XP, checks the grave holds exactly what was dropped
 * (with the original slots), then empties it and checks everything went back and the grave is gone.
 */
final class SelfTest {
    private SelfTest() {
    }

    static void run(MinecraftServer server) {
        try {
            for (String target : new String[]{
                    "net.minecraft.server.level.ServerPlayer",
                    "net.minecraft.server.level.ServerLevel",
                    "net.minecraft.world.entity.ExperienceOrb"}) {
                Class.forName(target);
            }
            if (FabricLoader.getInstance().isModLoaded("travelersbackpack")) {
                Class.forName("com.tiviacz.travelersbackpack.TravelersBackpack");
                SimpleGraves.LOGGER.info("simplegraves self test: travelers backpack mixin loaded");
            }
            deathAndRestore(server.overworld());
        } catch (Exception | AssertionError e) {
            SimpleGraves.LOGGER.error("simplegraves self test FAILED", e);
            return;
        }
        SimpleGraves.LOGGER.info("simplegraves self test passed");
    }

    private static void deathAndRestore(ServerLevel level) {
        level.getChunk(0, 0);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, 0, 0);
        FakePlayer player = FakePlayer.get(level);
        player.setPos(0.5, y, 0.5);
        player.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 5));
        player.getInventory().setItem(20, new ItemStack(Items.COBBLESTONE, 64));
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.TORCH, 3));
        player.giveExperienceLevels(5);

        player.die(level.damageSources().generic());

        GraveBlockEntity grave = null;
        for (BlockPos pos : BlockPos.betweenClosed(-4, y - 4, -4, 4, y + 4, 4)) {
            if (level.getBlockEntity(pos) instanceof GraveBlockEntity found) {
                grave = found;
                break;
            }
        }
        check(grave != null, "no grave placed");
        check(grave.entries().size() == 4, "expected 4 stacks in the grave, got " + grave.entries());
        check(grave.xp() == 35, "expected 35 xp in the grave, got " + grave.xp());
        check(grave.entries().stream().anyMatch(e -> e.slot().equals("inv:0") && e.stack().is(Items.DIAMOND) && e.stack().getCount() == 5),
                "diamonds not stored with their slot: " + grave.entries());
        check(player.getInventory().isEmpty(), "player still has items after death");
        BlockPos gravePos = grave.getBlockPos();

        grave.restoreTo(player);

        check(player.getInventory().getItem(0).is(Items.DIAMOND) && player.getInventory().getItem(0).getCount() == 5, "diamonds not back in slot 0");
        check(player.getInventory().getItem(20).is(Items.COBBLESTONE), "cobblestone not back in slot 20");
        check(player.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET), "helmet not back on the head");
        check(player.getItemBySlot(EquipmentSlot.OFFHAND).is(Items.TORCH), "torches not back in the offhand");
        check(!(level.getBlockEntity(gravePos) instanceof GraveBlockEntity), "grave still there after restoring");
        player.getInventory().clearContent();
        SimpleGraves.LOGGER.info("simplegraves self test: death grave ok at {}", gravePos);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
