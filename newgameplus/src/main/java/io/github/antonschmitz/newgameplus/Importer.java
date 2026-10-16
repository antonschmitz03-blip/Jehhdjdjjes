package io.github.antonschmitz.newgameplus;

import java.io.IOException;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

/** Applies a stored import to the first player who joins the new world without saved data. */
public final class Importer {
    private Importer() {
    }

    /** Called right before the server sends the joining player their state. */
    public static void onFirstJoin(MinecraftServer server, ServerPlayer player) {
        Path pending = NewGamePlus.pendingFile(server);
        if (!Files.isRegularFile(pending) || server.getPlayerList().loadPlayerData(player.nameAndId()).isPresent()) {
            return;
        }
        try {
            ImportData data = ImportData.fromTag(NbtIo.readCompressed(pending, NbtAccounter.unlimitedHeap()));
            apply(player, data);
            storeMobs(server, player, data);
            Files.move(pending, pending.resolveSibling("newgameplus_applied.dat"), StandardCopyOption.REPLACE_EXISTING);
            NewGamePlus.LOGGER.info("New Game Plus: imported {} from \"{}\"", player.getPlainTextName(), data.sourceName());
        } catch (IOException | RuntimeException e) {
            NewGamePlus.LOGGER.error("New Game Plus: could not import the player", e);
        }
    }

    /** Overwrites the carried parts of the player with the imported ones; position and the rest stay. */
    public static void apply(ServerPlayer player, ImportData data) {
        try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(NewGamePlus.LOGGER)) {
            TagValueOutput output = TagValueOutput.createWithContext(reporter, player.registryAccess());
            player.saveWithoutId(output);
            CompoundTag merged = PlayerTransfer.merge(output.buildResult(), data);
            player.load(TagValueInput.create(reporter, player.registryAccess(), merged));
        }
    }

    /** Keeps the mobs until the player has joined, so they appear next to them. */
    private static void storeMobs(MinecraftServer server, ServerPlayer player, ImportData data) throws IOException {
        List<CompoundTag> mobs = data.mobsToImport();
        if (mobs.isEmpty()) {
            return;
        }
        CompoundTag root = new CompoundTag();
        root.put("source_uuid", new net.minecraft.nbt.IntArrayTag(sourceId(data, player)));
        ListTag list = new ListTag();
        mobs.forEach(mob -> list.add(mob.copy()));
        root.put("mobs", list);
        NbtIo.writeCompressed(root, NewGamePlus.mobsFile(server));
    }

    /** The id the imported player had in the world they come from (it owns the mobs there). */
    private static int[] sourceId(ImportData data, ServerPlayer fallback) {
        return data.player().getIntArray("UUID").orElseGet(() -> UUIDUtil.uuidToIntArray(fallback.getUUID()));
    }

    /** Called once the player is in the world: spawns the imported mobs around them. */
    public static void spawnMobs(MinecraftServer server, ServerPlayer player) {
        Path file = NewGamePlus.mobsFile(server);
        if (!Files.isRegularFile(file)) {
            return;
        }
        try {
            CompoundTag root = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
            Files.move(file, file.resolveSibling("newgameplus_mobs_applied.dat"), StandardCopyOption.REPLACE_EXISTING);
            int[] from = root.getIntArray("source_uuid").orElseGet(() -> UUIDUtil.uuidToIntArray(player.getUUID()));
            int[] to = UUIDUtil.uuidToIntArray(player.getUUID());
            ListTag list = root.getListOrEmpty("mobs");
            int spawned = 0;
            for (int i = 0; i < list.size(); i++) {
                if (spawnMob(player, list.getCompoundOrEmpty(i).copy(), from, to, i)) {
                    spawned++;
                }
            }
            NewGamePlus.LOGGER.info("New Game Plus: spawned {} of {} imported mobs for {}", spawned, list.size(), player.getPlainTextName());
        } catch (IOException | RuntimeException e) {
            NewGamePlus.LOGGER.error("New Game Plus: could not import the mobs", e);
        }
    }

    private static boolean spawnMob(ServerPlayer player, CompoundTag tag, int[] from, int[] to, int index) {
        ServerLevel level = player.level();
        // What the mob was tied to in the old world does not exist here.
        for (String key : new String[]{"Passengers", "Leash", "leash", "Motion", "RootVehicle"}) {
            tag.remove(key);
        }
        MobScanner.retarget(tag, from, to);
        try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(NewGamePlus.LOGGER)) {
            ValueInput input = TagValueInput.create(reporter, level.registryAccess(), tag);
            Entity entity = EntityType.loadEntityRecursive(input, level, EntitySpawnReason.LOAD, loaded -> {
                // Spread them out on a ring around the player, further out for big mobs.
                double angle = index * 2.4;
                double radius = 1.5 + loaded.getBbWidth() * 0.75 + (index / 8) * 1.2;
                loaded.snapTo(player.getX() + Math.cos(angle) * radius, player.getY(), player.getZ() + Math.sin(angle) * radius,
                        loaded.getYRot(), 0.0F);
                loaded.setDeltaMovement(Vec3.ZERO);
                return level.addWithUUID(loaded) ? loaded : null;
            });
            return entity != null;
        }
    }
}
