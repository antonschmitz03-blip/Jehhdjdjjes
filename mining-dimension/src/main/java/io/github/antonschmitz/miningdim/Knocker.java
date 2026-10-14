package io.github.antonschmitz.miningdim;

import com.mojang.serialization.Codec;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A mischievous mine spirit from Cornish folklore. Knockers live around a den (a stash sack in the caves),
 * pickpocket the most valuable stack from a player's hotbar and only hand it back for a piece of food.
 * Three food offerings to a den's knockers keep that den off your pockets for one in-game day,
 * and while they like you they knock on the rock to warn you about lurking monsters.
 */
public class Knocker extends PathfinderMob implements RangedAttackMob {
    static final int OFFERINGS_NEEDED = 3;
    static final long APPEASED_TICKS = 24000L;
    static final int DEN_RADIUS = 20;
    static final int STEAL_COOLDOWN = 2400;
    private static final Codec<Map<String, Long>> LONG_MAP = Codec.unboundedMap(Codec.STRING, Codec.LONG);
    private static final Codec<Map<String, Integer>> INT_MAP = Codec.unboundedMap(Codec.STRING, Codec.INT);

    /** Hotbar loot, most tempting first. Anything not listed is left alone, so tools, weapons and armour are safe. */
    private static final List<List<Item>> LOOT_TIERS = List.of(
            List.<Item>of(Items.NETHERITE_BLOCK, Items.NETHERITE_INGOT, Items.NETHERITE_SCRAP, Items.ANCIENT_DEBRIS),
            List.<Item>of(Items.DIAMOND_BLOCK, Items.EMERALD_BLOCK, Items.DIAMOND, Items.EMERALD, Items.DIAMOND_ORE, Items.DEEPSLATE_DIAMOND_ORE,
                    Items.EMERALD_ORE, Items.DEEPSLATE_EMERALD_ORE, Items.ENCHANTED_BOOK),
            List.<Item>of(Items.GOLD_BLOCK, Items.RAW_GOLD_BLOCK, Items.GOLD_INGOT, Items.RAW_GOLD, Items.GOLD_ORE, Items.DEEPSLATE_GOLD_ORE),
            List.<Item>of(Items.IRON_BLOCK, Items.RAW_IRON_BLOCK, Items.LAPIS_BLOCK, Items.IRON_INGOT, Items.RAW_IRON, Items.LAPIS_LAZULI,
                    Items.AMETHYST_SHARD, Items.IRON_ORE, Items.DEEPSLATE_IRON_ORE, Items.LAPIS_ORE, Items.DEEPSLATE_LAPIS_ORE),
            List.<Item>of(Items.COPPER_BLOCK, Items.RAW_COPPER_BLOCK, Items.REDSTONE_BLOCK, Items.COPPER_INGOT, Items.RAW_COPPER, Items.REDSTONE,
                    Items.QUARTZ, Items.GOLD_NUGGET, Items.IRON_NUGGET, Items.REDSTONE_ORE, Items.DEEPSLATE_REDSTONE_ORE),
            List.<Item>of(Items.COAL, Items.COAL_BLOCK, Items.COAL_ORE, Items.DEEPSLATE_COAL_ORE, Items.COPPER_ORE, Items.DEEPSLATE_COPPER_ORE));

    private BlockPos den;
    private UUID victim;
    private int stealCooldown = 600;
    private int angerTicks;
    private int knockCooldown;
    private int knocksLeft;
    private final Map<String, Long> appeasedUntil = new HashMap<>();
    private final Map<String, Integer> offerings = new HashMap<>();

    public Knocker(EntityType<? extends Knocker> type, Level level) {
        super(type, level);
        this.setDropChance(EquipmentSlot.MAINHAND, 2.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 12.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3F)
                .add(Attributes.FOLLOW_RANGE, 20.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new RangedAttackGoal(this, 1.0, 30, 12.0F));
        this.goalSelector.addGoal(2, new RunWithLootGoal(this));
        this.goalSelector.addGoal(3, new GrabFoodGoal(this));
        this.goalSelector.addGoal(4, new PickpocketGoal(this));
        this.goalSelector.addGoal(5, new PinchTorchGoal(this));
        this.goalSelector.addGoal(6, new MoveTowardsRestrictionGoal(this, 1.0));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
    }

    // ---------- den ----------

    public void setDen(BlockPos den) {
        this.den = den;
        this.setHomeTo(den, DEN_RADIUS);
        this.setPersistenceRequired();
    }

    public BlockPos getDen() {
        return this.den;
    }

    /** The den's other knockers (including this one), or just this one for a den-less knocker. */
    List<Knocker> denMates() {
        if (this.den == null) return List.of(this);
        return this.level().getEntitiesOfClass(Knocker.class, new AABB(this.den).inflate(DEN_RADIUS + 12), k -> this.den.equals(k.den));
    }

    /** New den members learn who their den-mates already like. */
    void copyMoodFrom(Knocker other) {
        this.appeasedUntil.putAll(other.appeasedUntil);
        this.offerings.putAll(other.offerings);
    }

    public boolean isAppeasedBy(Player player) {
        Long until = this.appeasedUntil.get(player.getStringUUID());
        return until != null && until > this.level().getGameTime();
    }

    // ---------- loot ----------

    public boolean hasLoot() {
        return !this.getMainHandItem().isEmpty();
    }

    /** Hotbar slot holding the most tempting stack, or -1. */
    static int pickValuableSlot(Inventory inventory) {
        int best = -1;
        int bestScore = 0;
        for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty()) continue;
            for (int tier = 0; tier < LOOT_TIERS.size(); tier++) {
                if (LOOT_TIERS.get(tier).contains(stack.getItem())) {
                    int score = (LOOT_TIERS.size() - tier) * 1000 + stack.getCount();
                    if (score > bestScore) {
                        bestScore = score;
                        best = slot;
                    }
                    break;
                }
            }
        }
        return best;
    }

    boolean canRob(Player player) {
        return player.isAlive() && !player.isSpectator() && !player.isCreative() && !isAppeasedBy(player)
                && pickValuableSlot(player.getInventory()) >= 0;
    }

    void steal(ServerLevel level, Player player) {
        int slot = pickValuableSlot(player.getInventory());
        if (slot < 0) return;
        ItemStack loot = player.getInventory().removeItemNoUpdate(slot);
        player.getInventory().setChanged();
        this.setItemInHand(InteractionHand.MAIN_HAND, loot);
        this.setGuaranteedDrop(EquipmentSlot.MAINHAND);
        this.victim = player.getUUID();
        this.setPersistenceRequired();
        this.swingForAttack(InteractionHand.MAIN_HAND);
        this.playSound(SoundEvents.ITEM_PICKUP, 0.8F, 1.6F);
        this.playSound(SoundEvents.WITCH_CELEBRATE, 1.0F, 1.9F);
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, ItemStackTemplate.fromNonEmptyStack(loot)), this.getX(), this.getY() + 1.0, this.getZ(),
                6, 0.2, 0.2, 0.2, 0.05);
        player.sendOverlayMessage(Component.translatable("entity.miningdim.knocker.stole", loot.getHoverName()));
    }

    /** Hands the loot back by tossing it toward whoever paid the ransom. */
    void giveBackLoot(ServerLevel level, Player toward) {
        ItemStack loot = this.getMainHandItem().copy();
        this.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        this.victim = null;
        this.stealCooldown = STEAL_COOLDOWN;
        ItemEntity item = new ItemEntity(level, this.getX(), this.getEyeY() - 0.3, this.getZ(), loot);
        if (toward != null) {
            Vec3 dir = toward.position().subtract(this.position()).normalize().scale(0.25);
            item.setDeltaMovement(dir.x, 0.25, dir.z);
            this.getLookControl().setLookAt(toward);
        }
        item.setDefaultPickUpDelay();
        level.addFreshEntity(item);
        this.swingForAttack(InteractionHand.MAIN_HAND);
        this.playSound(SoundEvents.ALLAY_ITEM_GIVEN, 1.0F, 0.8F);
    }

    /** Eats a food item thrown by a player: pays back stolen loot, or counts as an offering to the den. */
    void eat(ServerLevel level, ItemEntity food) {
        Player thrower = food.getOwner() instanceof Player p ? p : null;
        ItemStack stack = food.getItem();
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, ItemStackTemplate.fromNonEmptyStack(stack)), this.getX(), this.getEyeY(), this.getZ(),
                8, 0.15, 0.1, 0.15, 0.05);
        stack.shrink(1);
        if (stack.isEmpty()) food.discard();
        this.playSound(SoundEvents.GENERIC_EAT.value(), 0.8F, 1.4F);
        this.angerTicks = 0;
        this.setTarget(null);
        if (hasLoot()) {
            giveBackLoot(level, thrower);
        } else if (thrower != null) {
            makeOffering(level, thrower);
        }
    }

    void makeOffering(ServerLevel level, Player player) {
        String id = player.getStringUUID();
        int count = this.offerings.getOrDefault(id, 0) + 1;
        List<Knocker> mates = denMates();
        if (count >= OFFERINGS_NEEDED) {
            long until = level.getGameTime() + APPEASED_TICKS;
            for (Knocker mate : mates) {
                mate.offerings.remove(id);
                mate.appeasedUntil.put(id, until);
            }
            level.sendParticles(ParticleTypes.HEART, this.getX(), this.getY() + 1.4, this.getZ(), 5, 0.3, 0.2, 0.3, 0.0);
            this.playSound(SoundEvents.ALLAY_AMBIENT_WITH_ITEM, 1.0F, 0.7F);
            player.sendOverlayMessage(Component.translatable("entity.miningdim.knocker.appeased"));
        } else {
            for (Knocker mate : mates) mate.offerings.put(id, count);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getY() + 1.2, this.getZ(), 4, 0.25, 0.2, 0.25, 0.0);
        }
    }

    // ---------- ticking ----------

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (this.stealCooldown > 0) this.stealCooldown--;
        if (this.getTarget() != null && ++this.angerTicks > 400) {
            this.setTarget(null);
            this.angerTicks = 0;
        }
        if (this.knocksLeft > 0 && this.tickCount % 6 == 0) {
            this.knocksLeft--;
            this.swingForAttack(InteractionHand.OFF_HAND);
            this.playSound(SoundEvents.NOTE_BLOCK_BASEDRUM.value(), 1.4F, 0.55F + this.random.nextFloat() * 0.1F);
        }
        if (--this.knockCooldown <= 0 && this.tickCount % 20 == 0) {
            this.knockCooldown = 0;
            for (Player player : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(16), this::isAppeasedBy)) {
                if (!level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(12), m -> m instanceof Enemy && m.isAlive()).isEmpty()) {
                    this.knocksLeft = 3;
                    this.knockCooldown = 300 + this.random.nextInt(200);
                    break;
                }
            }
        }
    }

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        if (!(this.level() instanceof ServerLevel level)) return;
        KnockerPebble pebble = new KnockerPebble(level, this);
        double dx = target.getX() - this.getX();
        double dy = target.getY(0.33) - pebble.getY();
        double dz = target.getZ() - this.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        pebble.shoot(dx, dy + dist * 0.2, dz, 1.2F, 8.0F);
        level.addFreshEntity(pebble);
        this.swingForAttack(InteractionHand.OFF_HAND);
        this.playSound(SoundEvents.SNOWBALL_THROW, 0.8F, 0.6F);
    }

    @Override
    public boolean removeWhenFarAway(double distanceSqr) {
        return this.den == null && !hasLoot();
    }

    @Override
    public boolean canPickUpLoot() {
        return false;
    }

    // ---------- save data ----------

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.storeNullable("Den", BlockPos.CODEC, this.den);
        if (this.victim != null) output.putString("Victim", this.victim.toString());
        output.putInt("StealCooldown", this.stealCooldown);
        output.store("AppeasedUntil", LONG_MAP, this.appeasedUntil);
        output.store("Offerings", INT_MAP, this.offerings);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        input.read("Den", BlockPos.CODEC).ifPresent(this::setDen);
        this.victim = input.getString("Victim").map(UUID::fromString).orElse(null);
        this.stealCooldown = input.getIntOr("StealCooldown", 600);
        input.read("AppeasedUntil", LONG_MAP).ifPresent(this.appeasedUntil::putAll);
        input.read("Offerings", INT_MAP).ifPresent(this.offerings::putAll);
    }

    // ---------- sounds ----------

    @Override
    protected SoundEvent getAmbientSound() {
        return hasLoot() ? SoundEvents.WITCH_CELEBRATE : SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ALLAY_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ALLAY_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.55F + this.random.nextFloat() * 0.1F;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 240;
    }

    // ---------- goals ----------

    /** With loot in hand: keep away from whoever is closest, giggling. */
    static class RunWithLootGoal extends Goal {
        private final Knocker knocker;
        private Player chaser;

        RunWithLootGoal(Knocker knocker) {
            this.knocker = knocker;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!this.knocker.hasLoot()) return false;
            this.chaser = this.knocker.level().getNearestPlayer(this.knocker, 7.0);
            return this.chaser != null && !this.chaser.isSpectator() && !this.chaser.isCreative();
        }

        @Override
        public boolean canContinueToUse() {
            return this.knocker.hasLoot() && !this.knocker.getNavigation().isDone();
        }

        @Override
        public void start() {
            Vec3 away = DefaultRandomPos.getPosAway(this.knocker, 12, 5, this.chaser.position());
            if (away != null) this.knocker.getNavigation().moveTo(away.x, away.y, away.z, 1.35);
        }
    }

    /** Walks to food a player threw nearby and eats it. */
    static class GrabFoodGoal extends Goal {
        private final Knocker knocker;
        private ItemEntity food;

        GrabFoodGoal(Knocker knocker) {
            this.knocker = knocker;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (this.knocker.tickCount % 5 != 0) return false;
            List<ItemEntity> items = this.knocker.level().getEntitiesOfClass(ItemEntity.class, this.knocker.getBoundingBox().inflate(10, 4, 10),
                    item -> item.isAlive() && item.getOwner() instanceof Player && item.getItem().has(DataComponents.FOOD));
            this.food = null;
            double best = Double.MAX_VALUE;
            for (ItemEntity item : items) {
                double d = item.distanceToSqr(this.knocker);
                if (d < best) {
                    best = d;
                    this.food = item;
                }
            }
            return this.food != null;
        }

        @Override
        public boolean canContinueToUse() {
            return this.food.isAlive() && this.food.distanceToSqr(this.knocker) < 196;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            this.knocker.getLookControl().setLookAt(this.food);
            if (this.knocker.distanceToSqr(this.food) < 2.5) {
                if (this.knocker.level() instanceof ServerLevel level) this.knocker.eat(level, this.food);
                this.food = null;
            } else if (this.knocker.tickCount % 10 == 0) {
                this.knocker.getNavigation().moveTo(this.food, 1.2);
            }
        }

        @Override
        public boolean isInterruptable() {
            return false;
        }

        @Override
        public void stop() {
            this.food = null;
            this.knocker.getNavigation().stop();
        }
    }

    /** Sneaks up on a player carrying something shiny and lifts it from their hotbar. */
    static class PickpocketGoal extends Goal {
        private final Knocker knocker;
        private Player mark;
        private int timeout;

        PickpocketGoal(Knocker knocker) {
            this.knocker = knocker;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (this.knocker.hasLoot() || this.knocker.stealCooldown > 0 || this.knocker.getTarget() != null || this.knocker.tickCount % 10 != 0) {
                return false;
            }
            this.mark = null;
            double best = 144;
            for (Player player : this.knocker.level().players()) {
                double d = player.distanceToSqr(this.knocker);
                if (d < best && this.knocker.canRob(player)) {
                    best = d;
                    this.mark = player;
                }
            }
            return this.mark != null;
        }

        @Override
        public void start() {
            this.timeout = 300;
        }

        @Override
        public boolean canContinueToUse() {
            return this.mark != null && --this.timeout > 0 && !this.knocker.hasLoot() && this.knocker.canRob(this.mark)
                    && this.mark.distanceToSqr(this.knocker) < 400;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            this.knocker.getLookControl().setLookAt(this.mark);
            if (this.knocker.distanceToSqr(this.mark) < 3.0) {
                if (this.knocker.level() instanceof ServerLevel level) this.knocker.steal(level, this.mark);
            } else if (this.knocker.tickCount % 10 == 0) {
                this.knocker.getNavigation().moveTo(this.mark, 1.25);
            }
        }

        @Override
        public void stop() {
            if (!this.knocker.hasLoot()) this.knocker.stealCooldown = 200;
            this.mark = null;
            this.knocker.getNavigation().stop();
        }
    }

    /** Now and then walks up to a torch and pockets it. */
    static class PinchTorchGoal extends Goal {
        private final Knocker knocker;
        private BlockPos torch;

        PinchTorchGoal(Knocker knocker) {
            this.knocker = knocker;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        static boolean isTorch(BlockState state) {
            return state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH);
        }

        @Override
        public boolean canUse() {
            if (this.knocker.hasLoot() || this.knocker.getTarget() != null || this.knocker.random.nextInt(600) != 0
                    || !(this.knocker.level() instanceof ServerLevel level) || !level.getGameRules().get(GameRules.MOB_GRIEFING)) {
                return false;
            }
            BlockPos origin = this.knocker.blockPosition();
            for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-8, -4, -8), origin.offset(8, 4, 8))) {
                if (isTorch(level.getBlockState(pos))) {
                    this.torch = pos.immutable();
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            return this.torch != null && !this.knocker.hasLoot() && isTorch(this.knocker.level().getBlockState(this.torch))
                    && !this.knocker.getNavigation().isDone() || this.torch != null && closeEnough();
        }

        private boolean closeEnough() {
            return this.knocker.position().distanceToSqr(Vec3.atCenterOf(this.torch)) < 6.0;
        }

        @Override
        public void start() {
            this.knocker.getNavigation().moveTo(this.torch.getX() + 0.5, this.torch.getY(), this.torch.getZ() + 0.5, 1.0);
        }

        @Override
        public void tick() {
            if (this.torch != null && closeEnough() && this.knocker.level() instanceof ServerLevel level) {
                if (isTorch(level.getBlockState(this.torch))) {
                    level.removeBlock(this.torch, false);
                    this.knocker.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.TORCH));
                    this.knocker.setGuaranteedDrop(EquipmentSlot.MAINHAND);
                    this.knocker.swingForAttack(InteractionHand.MAIN_HAND);
                    this.knocker.playSound(SoundEvents.WITCH_CELEBRATE, 1.0F, 1.9F);
                }
                this.torch = null;
            }
        }

        @Override
        public void stop() {
            this.torch = null;
        }
    }
}
