package io.github.antonschmitz.vplustweaks;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Happy ghast extras: speed upgrade, protection from players, and the whistle that calls it. */
public final class GhastFeatures {
    public static final int WHISTLE_RANGE = 128;
    private static final double SPEED_BONUS = 1.0; // +100% = double speed
    private static final int RECALL_TICKS = 20 * 45;
    private static final double ARRIVE_DISTANCE = 6.0;
    /** A ghast this close (sideways) to the whistling player lets its rope ladder down instead of flying over. */
    private static final double LADDER_DISTANCE = 8.0;

    private static final Identifier SPEED_ID = Identifier.fromNamespaceAndPath(VanillaPlusTweaks.MOD_ID, "ghast_speed_upgrade");

    /** The last player who steered this ghast; the whistle only calls ghasts you have ridden. */
    private static final AttachmentType<UUID> OWNER = AttachmentRegistry.createPersistent(
            Identifier.fromNamespaceAndPath(VanillaPlusTweaks.MOD_ID, "ghast_owner"), UUIDUtil.CODEC);

    /** Synced to clients so they can draw the rocket engines. */
    public static final AttachmentType<Boolean> SPEED_UPGRADED = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(VanillaPlusTweaks.MOD_ID, "ghast_speed_upgraded"),
            builder -> builder.persistent(Codec.BOOL).syncWith(ByteBufCodecs.BOOL, AttachmentSyncPredicate.all()));

    private static final Map<UUID, Recall> RECALLS = new HashMap<>();

    private record Recall(ServerLevel level, UUID player, int ticksLeft) {}

    private GhastFeatures() {}

    public static void register() {
        UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            ItemStack held = player.getItemInHand(hand);
            if (!held.is(VanillaPlusTweaks.GHAST_SPEED_UPGRADE) || !(entity instanceof HappyGhast ghast) || ghast.isBaby()) {
                return InteractionResult.PASS;
            }
            if (hasSpeedUpgrade(ghast)) {
                return InteractionResult.FAIL;
            }
            if (!level.isClientSide()) {
                addSpeed(ghast.getAttribute(Attributes.FLYING_SPEED));
                addSpeed(ghast.getAttribute(Attributes.MOVEMENT_SPEED));
                ghast.setAttached(SPEED_UPGRADED, true);
                if (!player.hasInfiniteMaterials()) {
                    held.shrink(1);
                }
            }
            return InteractionResult.SUCCESS;
        });

        // Players can't hurt a harnessed happy ghast, or the one they are riding.
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof HappyGhast ghast && source.getEntity() instanceof Player attacker) {
                return ghast.getItemBySlot(EquipmentSlot.BODY).isEmpty() && attacker.getVehicle() != ghast;
            }
            return true;
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof HappyGhast ghast && hasSpeedUpgrade(ghast)) {
                GhastChest.drop(ghast, new ItemStack(VanillaPlusTweaks.GHAST_SPEED_UPGRADE));
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(GhastFeatures::tick);
    }

    private static boolean hasSpeedUpgrade(HappyGhast ghast) {
        AttributeInstance speed = ghast.getAttribute(Attributes.FLYING_SPEED);
        return speed != null && speed.hasModifier(SPEED_ID);
    }

    private static void addSpeed(AttributeInstance attribute) {
        if (attribute != null && !attribute.hasModifier(SPEED_ID)) {
            attribute.addPermanentModifier(new AttributeModifier(SPEED_ID, SPEED_BONUS, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
    }

    /** Called by the whistle. Returns how many ghasts are on their way. */
    public static int callGhasts(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        int count = 0;
        for (HappyGhast ghast : level.getEntitiesOfClass(HappyGhast.class, player.getBoundingBox().inflate(WHISTLE_RANGE),
                g -> !g.isBaby() && !g.isVehicle() && player.getUUID().equals(g.getAttached(OWNER))
                        && g.distanceTo(player) <= WHISTLE_RANGE)) {
            double dx = ghast.getX() - player.getX();
            double dz = ghast.getZ() - player.getZ();
            if (dx * dx + dz * dz <= LADDER_DISTANCE * LADDER_DISTANCE) {
                RECALLS.remove(ghast.getUUID());
                RopeLadders.lower(ghast, player.position());
            } else {
                RECALLS.put(ghast.getUUID(), new Recall(level, player.getUUID(), RECALL_TICKS));
            }
            count++;
        }
        return count;
    }

    private static void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.getVehicle() instanceof HappyGhast ghast && ghast.getControllingPassenger() == player
                    && !player.getUUID().equals(ghast.getAttached(OWNER))) {
                ghast.setAttached(OWNER, player.getUUID());
            }
        }

        Iterator<Map.Entry<UUID, Recall>> it = RECALLS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Recall> entry = it.next();
            Recall recall = entry.getValue();
            Entity entity = recall.level().getEntity(entry.getKey());
            ServerPlayer player = server.getPlayerList().getPlayer(recall.player());
            if (!(entity instanceof HappyGhast ghast) || !ghast.isAlive() || ghast.isVehicle() || player == null
                    || player.level() != ghast.level() || recall.ticksLeft() <= 0) {
                it.remove();
                continue;
            }
            Vec3 toPlayer = player.position().add(0, 2.0, 0).subtract(ghast.position());
            double distance = toPlayer.length();
            if (distance < ARRIVE_DISTANCE) {
                ghast.setDeltaMovement(Vec3.ZERO);
                it.remove();
                RopeLadders.lower(ghast, player.position());
                continue;
            }
            ghast.getNavigation().stop();
            double speed = Math.min(0.6, distance * 0.08);
            ghast.setDeltaMovement(toPlayer.normalize().scale(speed));
            float yaw = (float) (Mth.atan2(toPlayer.z, toPlayer.x) * Mth.RAD_TO_DEG) - 90.0F;
            ghast.setYRot(yaw);
            ghast.setYHeadRot(yaw);
            ghast.yBodyRot = yaw;
            entry.setValue(new Recall(recall.level(), recall.player(), recall.ticksLeft() - 1));
        }
    }
}
