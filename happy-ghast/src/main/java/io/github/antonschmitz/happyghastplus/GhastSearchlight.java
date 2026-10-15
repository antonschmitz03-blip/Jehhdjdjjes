package io.github.antonschmitz.happyghastplus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A searchlight on a harnessed happy ghast. The rider switches it with a key; it lights up where the rider looks
 * (up to {@link #RANGE} blocks) by placing a ring of invisible {@link SearchlightLightBlock}s there, which follow the
 * aim a few times a second. The ring makes the lit pool round instead of the diamond a single light gives.
 */
public final class GhastSearchlight {
    public static final double RANGE = 48.0;
    private static final int UPDATE_INTERVAL = 5;

    /** Whether the ghast carries a searchlight; synced so clients can draw it. */
    public static final AttachmentType<Boolean> MOUNTED = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(HappyGhastPlus.MOD_ID, "searchlight"),
            builder -> builder.persistent(Codec.BOOL).syncWith(ByteBufCodecs.BOOL, AttachmentSyncPredicate.all()));
    /** Whether it is switched on; synced for the lit lens and the beam. Not saved: it starts off. */
    public static final AttachmentType<Boolean> ON = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(HappyGhastPlus.MOD_ID, "searchlight_on"),
            builder -> builder.syncWith(ByteBufCodecs.BOOL, AttachmentSyncPredicate.all()));

    /** Light blocks around the block the beam hits: {dx, dy, level} in the plane of the face that was hit. */
    private static final int[][] RING = ring();

    private static final Map<UUID, Lit> LIT = new HashMap<>();

    /** A switched-on searchlight; {@code center} is the air block the beam last hit, null if it hit nothing. */
    private record Lit(ServerLevel level, BlockPos center, Set<BlockPos> blocks) {}

    private GhastSearchlight() {}

    private static int[][] ring() {
        Set<String> seen = new HashSet<>();
        List<int[]> out = new ArrayList<>();
        out.add(new int[] {0, 0, 15});
        seen.add("0,0");
        int[][] base = {{3, 0, 14}, {2, 2, 15}, {3, 1, 14}};
        for (int[] b : base) {
            for (int[] p : new int[][] {{b[0], b[1]}, {b[1], b[0]}}) {
                for (int sx : new int[] {1, -1}) {
                    for (int sy : new int[] {1, -1}) {
                        int x = p[0] * sx;
                        int y = p[1] * sy;
                        if (seen.add(x + "," + y)) {
                            out.add(new int[] {x, y, b[2]});
                        }
                    }
                }
            }
        }
        return out.toArray(new int[0][]);
    }

    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(SearchlightTogglePayload.TYPE, SearchlightTogglePayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(SearchlightTogglePayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            if (player.getVehicle() instanceof HappyGhast ghast && ghast.getControllingPassenger() == player && isMounted(ghast)) {
                boolean on = !ghast.getAttachedOrElse(ON, false);
                ghast.setAttached(ON, on);
                if (on) {
                    LIT.put(ghast.getUUID(), new Lit((ServerLevel) ghast.level(), null, Set.of()));
                }
                ghast.level().playSound(null, ghast.getX(), ghast.getY() + 4.5, ghast.getZ(),
                        on ? SoundEvents.COPPER_BULB_TURN_ON : SoundEvents.COPPER_BULB_TURN_OFF, SoundSource.NEUTRAL, 1.0F, 1.2F);
                if (!on) {
                    clear(ghast.getUUID());
                }
            }
        });

        UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (!(entity instanceof HappyGhast ghast) || ghast.isBaby()) {
                return InteractionResult.PASS;
            }
            ItemStack held = player.getItemInHand(hand);
            if (held.is(HappyGhastPlus.GHAST_SEARCHLIGHT) && !isMounted(ghast)) {
                if (ghast.getItemBySlot(EquipmentSlot.BODY).isEmpty()) {
                    return InteractionResult.PASS;
                }
                if (!level.isClientSide()) {
                    ghast.setAttached(MOUNTED, true);
                    level.playSound(null, ghast.getX(), ghast.getY() + 4.0, ghast.getZ(), SoundEvents.CHAIN_PLACE, SoundSource.NEUTRAL, 1.0F, 1.0F);
                    if (!player.hasInfiniteMaterials()) {
                        held.shrink(1);
                    }
                }
                return InteractionResult.SUCCESS;
            }
            // Sneak + shears takes the searchlight off (before vanilla shears the harness).
            if (held.is(Items.SHEARS) && player.isSecondaryUseActive() && isMounted(ghast)) {
                if (!level.isClientSide()) {
                    unmount(ghast, !player.hasInfiniteMaterials());
                    level.playSound(null, ghast.getX(), ghast.getY() + 4.0, ghast.getZ(), SoundEvents.SHEEP_SHEAR, SoundSource.NEUTRAL, 1.0F, 1.0F);
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof HappyGhast ghast && isMounted(ghast)) {
                unmount(ghast, true);
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(GhastSearchlight::tick);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            for (UUID id : new HashSet<>(LIT.keySet())) {
                clear(id);
            }
        });
    }

    public static boolean isMounted(HappyGhast ghast) {
        return ghast.getAttachedOrElse(MOUNTED, false);
    }

    private static void unmount(HappyGhast ghast, boolean drop) {
        ghast.removeAttached(MOUNTED);
        ghast.removeAttached(ON);
        clear(ghast.getUUID());
        if (drop) {
            GhastChest.drop(ghast, new ItemStack(HappyGhastPlus.GHAST_SEARCHLIGHT));
        }
    }

    /** Whether a lit searchlight still wants a light block at {@code pos}. */
    static boolean isClaimed(ServerLevel level, BlockPos pos) {
        for (Lit lit : LIT.values()) {
            if (lit.level() == level && lit.blocks().contains(pos)) {
                return true;
            }
        }
        return false;
    }

    private static void tick(MinecraftServer server) {
        boolean update = server.getTickCount() % UPDATE_INTERVAL == 0;
        Iterator<Map.Entry<UUID, Lit>> it = LIT.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Lit> entry = it.next();
            Lit lit = entry.getValue();
            HappyGhast ghast = lit.level().getEntity(entry.getKey()) instanceof HappyGhast g ? g : null;
            boolean harnessed = ghast != null && !ghast.getItemBySlot(EquipmentSlot.BODY).isEmpty();
            ServerPlayer rider = ghast != null && ghast.getControllingPassenger() instanceof ServerPlayer p ? p : null;
            if (ghast == null || !ghast.isAlive() || !ghast.getAttachedOrElse(ON, false) || !isMounted(ghast) || !harnessed || rider == null) {
                // Gone, switched off, harness taken off or nobody steering: the light goes out.
                removeBlocks(lit.level(), lit.blocks(), Set.of());
                it.remove();
                if (ghast != null && ghast.isAlive()) {
                    ghast.removeAttached(ON);
                    if (isMounted(ghast) && !harnessed) {
                        ghast.removeAttached(MOUNTED);
                        GhastChest.drop(ghast, new ItemStack(HappyGhastPlus.GHAST_SEARCHLIGHT));
                    }
                }
                continue;
            }
            if (update) {
                entry.setValue(aim(lit, rider));
            }
        }
    }

    /** Where the rider looks, up to {@link #RANGE} blocks; null if the beam reaches nothing. */
    public static BlockHitResult target(Level level, LivingEntity rider, float partialTick) {
        Vec3 eye = rider.getEyePosition(partialTick);
        Vec3 end = eye.add(rider.getViewVector(partialTick).scale(RANGE));
        BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, rider));
        return hit.getType() == HitResult.Type.MISS ? null : hit;
    }

    private static Lit aim(Lit lit, ServerPlayer rider) {
        ServerLevel level = lit.level();
        BlockHitResult hit = target(level, rider, 1.0F);
        if (hit == null) {
            removeBlocks(level, lit.blocks(), Set.of());
            return new Lit(level, null, Set.of());
        }
        BlockPos center = hit.getBlockPos().relative(hit.getDirection());
        if (center.equals(lit.center())) {
            return lit;
        }
        Set<BlockPos> wanted = place(level, center, hit.getDirection(), lit.blocks());
        removeBlocks(level, lit.blocks(), wanted);
        return new Lit(level, center, wanted);
    }

    /**
     * Lights the ring around {@code center}, in the plane of the face the beam hit. Only air (or light blocks of
     * {@code own}) is replaced; a spot that is blocked tries one block further out of the surface.
     */
    static Set<BlockPos> place(ServerLevel level, BlockPos center, Direction face, Set<BlockPos> own) {
        Direction.Axis axis = face.getAxis();
        Set<BlockPos> placed = new HashSet<>();
        for (int[] spot : RING) {
            BlockPos pos = switch (axis) {
                case Y -> center.offset(spot[0], 0, spot[1]);
                case X -> center.offset(0, spot[1], spot[0]);
                case Z -> center.offset(spot[0], spot[1], 0);
            };
            BlockPos target = free(level, pos, own) ? pos : free(level, pos.relative(face), own) ? pos.relative(face) : null;
            if (target == null) {
                continue;
            }
            BlockState light = HappyGhastPlus.SEARCHLIGHT_LIGHT.defaultBlockState().setValue(SearchlightLightBlock.LEVEL, spot[2]);
            if (!level.getBlockState(target).equals(light)) {
                level.setBlock(target, light, Block.UPDATE_CLIENTS);
            }
            placed.add(target);
        }
        return placed;
    }

    private static boolean free(ServerLevel level, BlockPos pos, Set<BlockPos> own) {
        BlockState state = level.getBlockState(pos);
        return state.isAir() || (state.is(HappyGhastPlus.SEARCHLIGHT_LIGHT) && (own.contains(pos) || !isClaimed(level, pos)));
    }

    static void removeBlocks(ServerLevel level, Set<BlockPos> blocks, Set<BlockPos> keep) {
        for (BlockPos pos : blocks) {
            if (!keep.contains(pos) && level.getBlockState(pos).is(HappyGhastPlus.SEARCHLIGHT_LIGHT)) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }

    private static void clear(UUID ghast) {
        Lit lit = LIT.remove(ghast);
        if (lit != null) {
            removeBlocks(lit.level(), lit.blocks(), Set.of());
        }
    }

    /** Self test helper: number of light blocks the ring places on open ground. */
    static int ringSize() {
        return RING.length;
    }

}
