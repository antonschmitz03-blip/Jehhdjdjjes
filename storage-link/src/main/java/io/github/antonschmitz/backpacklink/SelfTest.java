package io.github.antonschmitz.backpacklink;

import com.tiviacz.travelersbackpack.attachment.AttachmentUtils;
import com.tiviacz.travelersbackpack.init.ModItems;
import com.tiviacz.travelersbackpack.inventory.BackpackWrapper;
import com.tom.storagemod.Content;
import com.tom.storagemod.block.AbstractStorageTerminalBlock;
import com.tom.storagemod.block.entity.StorageTerminalBlockEntity;
import com.tom.storagemod.inventory.StoredItemStack;
import com.tom.storagemod.inventory.TerminalItemStack;
import com.tom.storagemod.menu.StorageTerminalMenu;
import java.util.Map;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * CI smoke test: a terminal in front of a chest with 3 diamonds, and a fake player carrying a backpack with
 * 10 diamonds. Pulling 5 for the player takes the 3 from the chest and 2 from the backpack; pulling without a
 * player never touches the backpack.
 */
final class SelfTest {
    private SelfTest() {
    }

    static void run(MinecraftServer server) {
        try {
            check(TerminalMenuDuck.class.isAssignableFrom(StorageTerminalMenu.class), "terminal menu mixin not applied");
            BackpackLink.LOGGER.info("backpacklink self test: terminal mixin loaded");
            pull(server.overworld());
        } catch (Exception | AssertionError e) {
            BackpackLink.LOGGER.error("backpacklink self test FAILED", e);
            return;
        }
        BackpackLink.LOGGER.info("backpacklink self test passed");
    }

    private static void pull(ServerLevel level) {
        level.getChunk(0, 0);
        BlockPos pos = new BlockPos(4, level.getHeight(Heightmap.Types.MOTION_BLOCKING, 4, 4), 4);
        BlockState terminalState = Content.terminal.get().defaultBlockState();
        Direction facing = (Direction) terminalState.getValue(AbstractStorageTerminalBlock.FACING);
        BlockPos chestPos = pos.relative(facing);
        level.setBlockAndUpdate(chestPos, Blocks.CHEST.defaultBlockState());
        level.setBlockAndUpdate(pos, terminalState);
        try {
            ChestBlockEntity chest = (ChestBlockEntity) level.getBlockEntity(chestPos);
            chest.setItem(0, new ItemStack(Items.DIAMOND, 3));
            StorageTerminalBlockEntity terminal = (StorageTerminalBlockEntity) level.getBlockEntity(pos);
            terminal.onLoad();

            FakePlayer player = FakePlayer.get(level);
            ItemStack backpack = new ItemStack(ModItems.STANDARD_TRAVELERS_BACKPACK);
            new BackpackWrapper(backpack, 1, player, level, AttachmentUtils.STORAGE_ONLY.get())
                    .getStorage().insertItem(0, new ItemStack(Items.DIAMOND, 10), false);
            player.getInventory().setItem(5, backpack);
            check(diamonds(BackpackContents.snapshot(player)) == 10, "backpack snapshot: " + BackpackContents.snapshot(player));
            check(diamonds(BackpackContents.merge(Map.of(), BackpackContents.snapshot(player))) == 10, "merged list has no diamonds");

            StoredItemStack pulled = PullContext.with(player, () -> terminal.pullStack(new StoredItemStack(new ItemStack(Items.DIAMOND)), 5));
            check(pulled != null && pulled.getQuantity() == 5, "pulled " + (pulled == null ? 0 : pulled.getQuantity()) + " instead of 5");
            check(chest.getItem(0).isEmpty(), "network was not used first, chest still has " + chest.getItem(0));
            check(diamonds(BackpackContents.snapshot(player)) == 8, "backpack should have 8 diamonds left: " + BackpackContents.snapshot(player));
            BackpackLink.LOGGER.info("backpacklink self test: pull from network then backpack ok");

            check(terminal.pullStack(new StoredItemStack(new ItemStack(Items.DIAMOND)), 5) == null, "pull without a player used the backpack");
            check(diamonds(BackpackContents.snapshot(player)) == 8, "backpack changed by a pull without a player");
            player.getInventory().clearContent();
        } finally {
            level.removeBlock(pos, false);
            level.removeBlock(chestPos, false);
        }
    }

    private static long diamonds(Map<? extends StoredItemStack, ?> map) {
        Object value = map.get(new TerminalItemStack(new ItemStack(Items.DIAMOND), 1));
        if (value instanceof Long count) {
            return count;
        }
        return value instanceof StoredItemStack stack ? stack.getQuantity() : 0;
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
