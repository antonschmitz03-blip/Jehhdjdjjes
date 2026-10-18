package io.github.antonschmitz.oreshrines;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The shrine's brain. Every few seconds it measures the pyramid under the core (beacon layout, 1 to 4 layers of
 * iron, gold or diamond blocks) and works out its range; every tick it applies the chosen effect inside that range.
 * It is also a one-slot container that only takes ore, so hoppers can feed a Wisdom shrine.
 */
public class ShrineBlockEntity extends BlockEntity implements Container {
    public static final int MAX_XP = 2000;
    public static final int MAX_LAYERS = 4;
    /** Vanilla's default random tick speed: 3 random ticks per 16x16x16 section per tick. */
    private static final int RANDOM_TICKS_PER_SECTION = 3;
    private static final Map<Item, Integer> XP_VALUES = Map.of(
            Items.IRON_INGOT, 1, Items.RAW_IRON, 1, Items.IRON_BLOCK, 9, Items.RAW_IRON_BLOCK, 9,
            Items.GOLD_INGOT, 2, Items.RAW_GOLD, 2, Items.GOLD_BLOCK, 18, Items.RAW_GOLD_BLOCK, 18,
            Items.DIAMOND, 10, Items.DIAMOND_BLOCK, 90);

    private ShrineEffect effect = ShrineEffect.NONE;
    private int storedXp;

    // Measured from the pyramid; not saved, the first tick after loading measures again.
    private int layers;
    private int ironBlocks;
    private int goldBlocks;
    private int diamondBlocks;
    private int radius;
    private int scanCooldown;
    private boolean registered;
    private final List<AbstractFurnaceBlockEntity> furnaces = new ArrayList<>();

    private @Nullable UUID ringViewer;
    private int ringTicks;

    public ShrineBlockEntity(BlockPos pos, BlockState state) {
        super(OreShrines.SHRINE_BLOCK_ENTITY, pos, state);
    }

    public ShrineEffect effect() {
        return effect;
    }

    public int storedXp() {
        return storedXp;
    }

    public int layers() {
        return layers;
    }

    public int radius() {
        return radius;
    }

    public int ironBlocks() {
        return ironBlocks;
    }

    public int goldBlocks() {
        return goldBlocks;
    }

    public int diamondBlocks() {
        return diamondBlocks;
    }

    /** Wisdom pays out 1 XP per second per pyramid layer. */
    public int xpPerSecond() {
        return layers;
    }

    public static int xpValue(ItemStack stack) {
        return XP_VALUES.getOrDefault(stack.getItem(), 0);
    }

    public static int xpValue(Item item) {
        return XP_VALUES.getOrDefault(item, 0);
    }

    private int verticalRadius() {
        return Math.max(8, radius / 2);
    }

    /** The range is a box: {@link #radius} blocks out to each side, half that up and down (at least 8). */
    public boolean inRange(BlockPos pos) {
        if (radius <= 0 || isRemoved()) {
            return false;
        }
        BlockPos core = getBlockPos();
        return Math.abs(pos.getX() - core.getX()) <= radius
                && Math.abs(pos.getZ() - core.getZ()) <= radius
                && Math.abs(pos.getY() - core.getY()) <= verticalRadius();
    }

    private AABB rangeBox() {
        BlockPos core = getBlockPos();
        int v = verticalRadius();
        return new AABB(core.getX() - radius, core.getY() - v, core.getZ() - radius,
                core.getX() + radius + 1, core.getY() + v + 1, core.getZ() + radius + 1);
    }

    public void setEffect(ShrineEffect effect) {
        this.effect = effect;
        setChanged();
        updateComparators();
    }

    /** Adds up to {@code count} items worth {@code value} XP each; returns how many were taken. */
    public int feed(int value, int count) {
        if (value <= 0 || effect != ShrineEffect.WISDOM) {
            return 0;
        }
        int taken = Math.min(count, (MAX_XP - storedXp) / value);
        if (taken > 0) {
            storedXp += taken * value;
            setChanged();
            updateComparators();
        }
        return taken;
    }

    public void showRange(ServerPlayer player) {
        ringViewer = player.getUUID();
        ringTicks = 60;
    }

    public int comparatorSignal() {
        if (effect != ShrineEffect.WISDOM || storedXp <= 0) {
            return 0;
        }
        return Math.max(1, Math.round(storedXp * 15.0F / MAX_XP));
    }

    private void updateComparators() {
        if (level != null) {
            level.updateNeighbourForOutputSignal(getBlockPos(), getBlockState().getBlock());
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ShrineBlockEntity shrine) {
        if (level instanceof ServerLevel serverLevel) {
            shrine.tick(serverLevel);
        }
    }

    private void tick(ServerLevel level) {
        if (!registered) {
            ShrineRegistry.add(level, this);
            registered = true;
        }
        if (--scanCooldown <= 0) {
            scanCooldown = 80;
            scan(level);
        }
        long time = level.getGameTime();
        if (ringTicks > 0) {
            if (ringTicks % 10 == 0) {
                drawRing(level);
            }
            ringTicks--;
        }
        if (radius <= 0 || effect == ShrineEffect.NONE) {
            return;
        }
        if (time % 20 == 0) {
            BlockPos core = getBlockPos();
            level.sendParticles(new DustParticleOptions(effect.color(), 1.0F),
                    core.getX() + 0.5, core.getY() + 1.0, core.getZ() + 0.5, 3, 0.2, 0.15, 0.2, 0.0);
        }
        switch (effect) {
            case HARVEST -> growPlants(level);
            case WARD -> {
                if (time % 40 == 0) {
                    // Sleeping is what keeps phantoms away; standing near a Ward shrine counts as having slept.
                    for (ServerPlayer player : playersInRange(level)) {
                        player.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));
                    }
                }
            }
            case MENDING -> {
                if (time % 40 == 0) {
                    for (ServerPlayer player : playersInRange(level)) {
                        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0, true, true));
                    }
                }
            }
            case HEARTH -> {
                // One extra furnace tick every other tick: 50% faster smelting.
                if (time % 2 == 0) {
                    for (AbstractFurnaceBlockEntity furnace : furnaces) {
                        if (!furnace.isRemoved() && furnace.getLevel() == level) {
                            AbstractFurnaceBlockEntity.serverTick(level, furnace.getBlockPos(), furnace.getBlockState(), furnace);
                        }
                    }
                }
            }
            case PASTURE -> {
                if (time % 20 == 0) {
                    growAnimals(level);
                }
            }
            case WISDOM -> {
                if (time % 20 == 0) {
                    payXp(level);
                }
            }
            default -> {
            }
        }
    }

    /** Measures the pyramid and, for Hearth, collects the furnaces in range. Package-private for the menu. */
    void scan(ServerLevel level) {
        BlockPos core = getBlockPos();
        int iron = 0;
        int gold = 0;
        int diamond = 0;
        int found = 0;
        for (int layer = 1; layer <= MAX_LAYERS; layer++) {
            int y = core.getY() - layer;
            if (y < level.getMinY()) {
                break;
            }
            int layerIron = 0;
            int layerGold = 0;
            int layerDiamond = 0;
            boolean complete = true;
            for (int x = -layer; x <= layer && complete; x++) {
                for (int z = -layer; z <= layer; z++) {
                    BlockState state = level.getBlockState(new BlockPos(core.getX() + x, y, core.getZ() + z));
                    if (state.is(Blocks.IRON_BLOCK)) {
                        layerIron++;
                    } else if (state.is(Blocks.GOLD_BLOCK)) {
                        layerGold++;
                    } else if (state.is(Blocks.DIAMOND_BLOCK)) {
                        layerDiamond++;
                    } else {
                        complete = false;
                        break;
                    }
                }
            }
            if (!complete) {
                break;
            }
            found = layer;
            iron += layerIron;
            gold += layerGold;
            diamond += layerDiamond;
        }
        layers = found;
        ironBlocks = iron;
        goldBlocks = gold;
        diamondBlocks = diamond;
        int total = iron + gold + diamond;
        if (found == 0 || total == 0) {
            radius = 0;
        } else {
            // 16 blocks per layer for plain iron; gold and diamond reach further (all diamond = 1.5x).
            double points = (iron + gold * 2.0 + diamond * 3.0) / total;
            radius = (int) Math.round(16 * found * (0.75 + 0.25 * points));
        }

        furnaces.clear();
        if (radius > 0 && effect == ShrineEffect.HEARTH) {
            int minChunkX = (core.getX() - radius) >> 4;
            int maxChunkX = (core.getX() + radius) >> 4;
            int minChunkZ = (core.getZ() - radius) >> 4;
            int maxChunkZ = (core.getZ() + radius) >> 4;
            for (int cx = minChunkX; cx <= maxChunkX; cx++) {
                for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                    if (!level.getChunkSource().hasChunk(cx, cz)) {
                        continue;
                    }
                    for (BlockEntity blockEntity : level.getChunk(cx, cz).getBlockEntities().values()) {
                        if (blockEntity instanceof AbstractFurnaceBlockEntity furnace && inRange(furnace.getBlockPos())) {
                            furnaces.add(furnace);
                        }
                    }
                }
            }
        }
    }

    private List<ServerPlayer> playersInRange(ServerLevel level) {
        List<ServerPlayer> players = new ArrayList<>();
        for (ServerPlayer player : level.players()) {
            if (!player.isSpectator() && player.isAlive() && inRange(player.blockPosition())) {
                players.add(player);
            }
        }
        return players;
    }

    /** As many extra random ticks as the game itself gives the range, but only for plants: twice the growth speed. */
    private void growPlants(ServerLevel level) {
        BlockPos core = getBlockPos();
        int v = verticalRadius();
        int width = radius * 2 + 1;
        int height = v * 2 + 1;
        double perTick = (double) width * width * height / 4096.0 * RANDOM_TICKS_PER_SECTION;
        RandomSource random = level.getRandom();
        int count = (int) perTick + (random.nextDouble() < perTick - (int) perTick ? 1 : 0);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < count; i++) {
            pos.set(core.getX() - radius + random.nextInt(width), core.getY() - v + random.nextInt(height),
                    core.getZ() - radius + random.nextInt(width));
            if (!level.isLoaded(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (state.isRandomlyTicking() && isPlant(state.getBlock())) {
                state.randomTick(level, pos.immutable(), random);
            }
        }
    }

    private static boolean isPlant(Block block) {
        return block instanceof BonemealableBlock || block instanceof SugarCaneBlock || block instanceof CactusBlock
                || block instanceof NetherWartBlock;
    }

    /** Babies grow up and breeding cooldowns run out twice as fast. */
    private void growAnimals(ServerLevel level) {
        for (AgeableMob mob : level.getEntitiesOfClass(AgeableMob.class, rangeBox())) {
            if (mob.isAgeLocked()) {
                continue;
            }
            int age = mob.getAge();
            if (age < 0) {
                mob.setAge(Math.min(0, age + 20));
            } else if (age > 0) {
                mob.setAge(Math.max(0, age - 20));
            }
        }
    }

    private void payXp(ServerLevel level) {
        if (storedXp <= 0) {
            return;
        }
        BlockPos core = getBlockPos();
        Vec3 center = Vec3.atCenterOf(core);
        ServerPlayer nearest = null;
        double best = Double.MAX_VALUE;
        for (ServerPlayer player : playersInRange(level)) {
            double distance = player.position().distanceToSqr(center);
            if (distance < best) {
                best = distance;
                nearest = player;
            }
        }
        if (nearest == null) {
            return;
        }
        int amount = Math.min(storedXp, xpPerSecond());
        storedXp -= amount;
        ExperienceOrb.award(level, nearest.position().add(0, 0.5, 0), amount);
        level.sendParticles(new DustParticleOptions(effect.color(), 1.2F),
                center.x, center.y + 0.6, center.z, 6, 0.3, 0.3, 0.3, 0.0);
        setChanged();
        updateComparators();
    }

    /** A square of particles at the edge of the range, shown only to the player who opened the shrine. */
    private void drawRing(ServerLevel level) {
        if (ringViewer == null || radius <= 0 || !(level.getPlayerByUUID(ringViewer) instanceof ServerPlayer player)) {
            return;
        }
        BlockPos core = getBlockPos();
        DustParticleOptions dust = new DustParticleOptions(effect.color(), 1.5F);
        double y = player.getY() + 0.3;
        int step = radius > 48 ? 3 : 2;
        for (int d = -radius; d <= radius; d += step) {
            double[][] points = {
                    {core.getX() + d + 0.5, core.getZ() - radius + 0.5},
                    {core.getX() + d + 0.5, core.getZ() + radius + 0.5},
                    {core.getX() - radius + 0.5, core.getZ() + d + 0.5},
                    {core.getX() + radius + 0.5, core.getZ() + d + 0.5},
            };
            for (double[] p : points) {
                level.sendParticles(player, dust, true, true, p[0], y, p[1], 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (registered && level != null) {
            ShrineRegistry.remove(level, this);
        }
        registered = false;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        // Breaking the core gives back whatever XP was still stored.
        if (level instanceof ServerLevel serverLevel && storedXp > 0) {
            ExperienceOrb.award(serverLevel, Vec3.atCenterOf(pos), storedXp);
            storedXp = 0;
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString("effect", effect.id());
        output.putInt("stored_xp", storedXp);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        effect = ShrineEffect.byId(input.getStringOr("effect", "none"));
        storedXp = Math.max(0, Math.min(MAX_XP, input.getIntOr("stored_xp", 0)));
    }

    // Container: one write-only slot. Hoppers can push ore into a Wisdom shrine; nothing can be taken out.

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (!stack.isEmpty()) {
            feed(xpValue(stack), stack.getCount());
        }
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        int value = xpValue(stack);
        return effect == ShrineEffect.WISDOM && value > 0 && storedXp + value <= MAX_XP;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
    }
}
