package io.github.antonschmitz.newgameplus;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.TagValueOutput;

/**
 * CI smoke test: saves a fake player with items and XP as the "old world" player, wipes and moves it, then
 * imports it back through a fake source world folder and the pending file, the way a new world does.
 */
final class SelfTest {
    private SelfTest() {
    }

    static void run(MinecraftServer server) {
        try {
            Class.forName("net.minecraft.server.players.PlayerList");
            ServerLevel level = server.overworld();
            FakePlayer player = FakePlayer.get(level);

            CompoundTag source = sourcePlayer(player);
            NewGamePlus.LOGGER.info("newgameplus self test: player keys {}", source.keySet());
            CompoundTag read = readThroughWorldFolder(source);
            check(read.get("Inventory").equals(source.get("Inventory")), "inventory changed while reading the world folder");

            wipe(player);
            player.setPos(100.5, 80, -40.5);
            NbtIo.writeCompressed(new ImportData("Old World", read, true, true, false).toTag(), NewGamePlus.pendingFile(server));
            Importer.onFirstJoin(server, player);

            check(player.getInventory().getItem(0).is(Items.DIAMOND) && player.getInventory().getItem(0).getCount() == 5, "diamonds not imported");
            check(player.getInventory().getItem(20).is(Items.COBBLESTONE), "cobblestone not imported");
            check(player.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET), "helmet not imported");
            check(player.getItemBySlot(EquipmentSlot.OFFHAND).is(Items.TORCH), "offhand not imported");
            check(player.experienceLevel == 5, "expected level 5, got " + player.experienceLevel);
            check(player.getEnderChestInventory().isEmpty(), "ender chest imported although it was off");
            check(player.getX() == 100.5 && player.getZ() == -40.5, "position changed: " + player.position());
            check(!NewGamePlus.hasPending(server), "pending import not used up");
            Files.deleteIfExists(NewGamePlus.pendingFile(server).resolveSibling("newgameplus_applied.dat"));

            // Other mods' self tests reuse the same fake player.
            wipe(player);
            NewGamePlus.LOGGER.info("newgameplus self test: import ok");
        } catch (Exception | AssertionError e) {
            NewGamePlus.LOGGER.error("newgameplus self test FAILED", e);
            return;
        }
        NewGamePlus.LOGGER.info("newgameplus self test passed");
    }

    private static CompoundTag sourcePlayer(FakePlayer player) {
        // Other mods' self tests may have run on the same fake player first.
        wipe(player);
        player.setPos(0.5, 70, 0.5);
        player.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 5));
        player.getInventory().setItem(20, new ItemStack(Items.COBBLESTONE, 64));
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.TORCH, 3));
        player.getEnderChestInventory().setItem(0, new ItemStack(Items.EMERALD));
        player.giveExperienceLevels(5);
        try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(NewGamePlus.LOGGER)) {
            TagValueOutput output = TagValueOutput.createWithContext(reporter, player.registryAccess());
            player.saveWithoutId(output);
            return NbtUtils.addCurrentDataVersion(output.buildResult());
        }
    }

    private static void wipe(FakePlayer player) {
        player.getInventory().clearContent();
        player.getEnderChestInventory().clearContent();
        player.experienceLevel = 0;
        player.experienceProgress = 0;
        player.totalExperience = 0;
    }

    /** Lays out a world folder like a 26.3 singleplayer save (players/data) and reads it back with {@link SourceReader}. */
    private static CompoundTag readThroughWorldFolder(CompoundTag source) throws Exception {
        Path world = Files.createTempDirectory("newgameplus-selftest");
        try {
            UUID owner = UUID.randomUUID();
            CompoundTag data = NbtUtils.addCurrentDataVersion(new CompoundTag());
            data.store("singleplayer_uuid", UUIDUtil.CODEC, owner);
            CompoundTag root = new CompoundTag();
            root.put("Data", data);
            NbtIo.writeCompressed(root, world.resolve("level.dat"));
            Files.createDirectories(world.resolve("players").resolve("data"));
            NbtIo.writeCompressed(source, world.resolve("players").resolve("data").resolve(owner + ".dat"));

            Optional<CompoundTag> read = SourceReader.readPlayer(world, UUID.randomUUID());
            check(read.isPresent(), "no player found in the test world folder");
            return read.get();
        } finally {
            try (Stream<Path> files = Files.walk(world)) {
                files.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
            }
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
