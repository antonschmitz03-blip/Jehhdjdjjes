package io.github.antonschmitz.waystones;

import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * CI smoke test: places three waystones, discovers them with a fake player, checks the XP cost (free nearby,
 * refused without enough levels, paid when there are), the warp scroll, a removed waystone and that the data
 * survives a reload.
 */
final class SelfTest {
    private SelfTest() {
    }

    static void run(MinecraftServer server) {
        try {
            ServerLevel level = server.overworld();
            FakePlayer player = FakePlayer.get(level);
            BlockPos alpha = place(level, 0, 0);
            BlockPos beta = place(level, 1200, 0);
            BlockPos near = place(level, 200, 0);
            WaystoneBlock.register(level, alpha, player, "Alpha");
            WaystoneBlock.register(level, beta, player, "Beta");
            WaystoneBlock.register(level, near, player, null);

            for (BlockPos pos : new BlockPos[]{alpha, beta, near}) {
                WaystoneBlock.activate(level, pos, player);
            }
            check(WaystoneData.get().discovered(player.getUUID()).size() == 3, "expected 3 discovered waystones");

            standAt(player, alpha);
            WaystoneData.Waystone betaStone = WaystoneData.get().at(level, beta);
            WaystoneData.Waystone nearStone = WaystoneData.get().at(level, near);
            check(WaystoneTeleport.cost(player, betaStone) == 2, "1200 blocks should cost 2 levels, got " + WaystoneTeleport.cost(player, betaStone));
            check(WaystoneTeleport.cost(player, nearStone) == 0, "200 blocks should be free");

            // Not enough levels: stays put.
            player.giveExperienceLevels(-player.experienceLevel);
            WaystoneMenu menu = WaystoneMenu.create(1, player.getInventory(), player, WaystoneData.get().at(level, alpha), null);
            int betaSlot = slotOf(menu, "Beta");
            menu.clicked(betaSlot, 0, ContainerInput.PICKUP, player);
            check(player.blockPosition().distManhattan(alpha) <= 4, "teleported without paying");

            player.giveExperienceLevels(5);
            menu = WaystoneMenu.create(2, player.getInventory(), player, WaystoneData.get().at(level, alpha), null);
            menu.clicked(betaSlot, 0, ContainerInput.PICKUP, player);
            check(player.blockPosition().distManhattan(beta) <= 4, "not at Beta after teleporting: " + player.blockPosition());
            check(player.experienceLevel == 3, "expected 3 levels left, got " + player.experienceLevel);
            SimpleWaystones.LOGGER.info("simplewaystones self test: teleport ok");

            // Warp scroll: free, used up.
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(SimpleWaystones.WARP_SCROLL, 2));
            menu = WaystoneMenu.create(3, player.getInventory(), player, null, InteractionHand.MAIN_HAND);
            menu.clicked(slotOf(menu, "Alpha"), 0, ContainerInput.PICKUP, player);
            check(player.blockPosition().distManhattan(alpha) <= 4, "warp scroll did not teleport to Alpha");
            check(player.experienceLevel == 3, "warp scroll cost XP");
            check(player.getMainHandItem().getCount() == 1, "warp scroll not used up");
            SimpleWaystones.LOGGER.info("simplewaystones self test: warp scroll ok");

            // A waystone removed without breaking it disappears from the list on use.
            level.setBlockAndUpdate(near, Blocks.AIR.defaultBlockState());
            check(!WaystoneTeleport.teleport(player, nearStone, false), "teleported to a removed waystone");
            check(WaystoneData.get().discovered(player.getUUID()).size() == 2, "removed waystone still listed");

            WaystoneData.load(server);
            check(WaystoneData.get().discovered(player.getUUID()).size() == 2, "discovered waystones lost on reload");
            check("Beta".equals(WaystoneData.get().at(level, beta).displayName().getString()), "name lost on reload");

            for (BlockPos pos : new BlockPos[]{alpha, beta}) {
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                WaystoneData.get().remove(level, pos);
            }
            player.getInventory().clearContent();
        } catch (Exception | AssertionError e) {
            SimpleWaystones.LOGGER.error("simplewaystones self test FAILED", e);
            return;
        }
        SimpleWaystones.LOGGER.info("simplewaystones self test passed");
    }

    private static BlockPos place(ServerLevel level, int x, int z) {
        level.getChunk(x >> 4, z >> 4);
        BlockPos pos = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z), z);
        level.setBlockAndUpdate(pos, SimpleWaystones.WAYSTONE.defaultBlockState());
        return pos;
    }

    private static void standAt(FakePlayer player, BlockPos pos) {
        player.setPos(pos.getX() + 1.5, pos.getY(), pos.getZ() + 0.5);
    }

    private static int slotOf(WaystoneMenu menu, String name) {
        for (int i = 0; i < 54 && i < menu.slots.size(); i++) {
            if (menu.getSlot(i).getItem().getHoverName().getString().equals(name)) {
                return i;
            }
        }
        throw new AssertionError("no " + name + " in the menu");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
