package io.github.antonschmitz.miningdim;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.resources.Identifier;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * A small, round stone critter. Feed it a raw ore and it "tastes" the rock around it,
 * waddles toward the nearest matching ore and points a sparkle trail at it.
 * Adults occasionally shed a nugget or flint. Breeds with glow berries.
 */
public class Pebblin extends Animal {
    /** Ore kinds a pebblin can sniff out. Index 0 means "not prospecting". */
    public enum Ore {
        NONE("none", null, null),
        COAL("coal", Items.COAL, ores("coal")),
        IRON("iron", Items.RAW_IRON, ores("iron")),
        COPPER("copper", Items.RAW_COPPER, ores("copper")),
        GOLD("gold", Items.RAW_GOLD, ores("gold")),
        REDSTONE("redstone", Items.REDSTONE, ores("redstone")),
        LAPIS("lapis", Items.LAPIS_LAZULI, ores("lapis")),
        DIAMOND("diamond", Items.DIAMOND, ores("diamond")),
        EMERALD("emerald", Items.EMERALD, ores("emerald"));

        public final String name;
        final Item snack;
        final TagKey<Block> blocks;

        Ore(String name, Item snack, TagKey<Block> blocks) {
            this.name = name;
            this.snack = snack;
            this.blocks = blocks;
        }

        private static TagKey<Block> ores(String metal) {
            return TagKey.create(Registries.BLOCK, Identifier.withDefaultNamespace(metal + "_ores"));
        }

        static Ore bySnack(ItemStack stack) {
            for (Ore ore : values()) {
                if (ore.snack != null && stack.is(ore.snack)) return ore;
            }
            return NONE;
        }

        public static Ore byId(int id) {
            return id >= 0 && id < values().length ? values()[id] : NONE;
        }
    }

    private static final EntityDataAccessor<Integer> DATA_ORE = SynchedEntityData.defineId(Pebblin.class, EntityDataSerializers.INT);
    static final int PROSPECT_TICKS = 1200;
    static final int SEARCH_RADIUS = 14;

    private int prospectTicks;
    private BlockPos target;
    private int giftTime;

    public Pebblin(EntityType<? extends Pebblin> type, Level level) {
        super(type, level);
        this.giftTime = nextGiftTime(this.random);
    }

    private static int nextGiftTime(RandomSource random) {
        return 6000 + random.nextInt(6000);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes().add(Attributes.MAX_HEALTH, 8.0).add(Attributes.MOVEMENT_SPEED, 0.2F);
    }

    /** Pebblins live in dark caves, so they don't need light or grass to spawn. */
    public static boolean checkPebblinSpawnRules(EntityType<Pebblin> type, LevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        BlockState below = level.getBlockState(pos.below());
        return below.is(BlockTags.BASE_STONE_OVERWORLD) || below.is(BlockTags.ANIMALS_SPAWNABLE_ON);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ORE, 0);
    }

    public Ore getOre() {
        return Ore.byId(this.entityData.get(DATA_ORE));
    }

    private void setOre(Ore ore) {
        this.entityData.set(DATA_ORE, ore.ordinal());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.6));
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(3, new ProspectGoal(this));
        this.goalSelector.addGoal(4, new TemptGoal(this, 1.2, stack -> isFood(stack) || Ore.bySnack(stack) != Ore.NONE, false));
        this.goalSelector.addGoal(5, new FollowParentGoal(this, 1.2));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.GLOW_BERRIES);
    }

    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader level) {
        return level.getBlockState(pos.below()).is(BlockTags.BASE_STONE_OVERWORLD) ? 5.0F : 0.0F;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Ore ore = Ore.bySnack(stack);
        if (ore != Ore.NONE && !this.isBaby()) {
            if (this.level() instanceof ServerLevel level) {
                this.usePlayerItem(player, hand, stack);
                startProspecting(level, ore);
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    void startProspecting(ServerLevel level, Ore ore) {
        setOre(ore);
        this.prospectTicks = PROSPECT_TICKS;
        this.target = findOre(level, ore);
        this.playSound(SoundEvents.ARMADILLO_EAT, 0.8F, 1.4F);
        if (this.target == null) {
            // Nothing nearby: a sad little shake and it loses interest quickly.
            level.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.6, this.getZ(), 6, 0.2, 0.1, 0.2, 0.01);
            this.playSound(SoundEvents.AMETHYST_CLUSTER_HIT, 0.8F, 0.6F);
            this.prospectTicks = 60;
        } else {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getY() + 0.6, this.getZ(), 6, 0.25, 0.15, 0.25, 0.0);
        }
    }

    BlockPos findOre(ServerLevel level, Ore ore) {
        BlockPos origin = this.blockPosition();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -SEARCH_RADIUS; dx <= SEARCH_RADIUS; dx++) {
            for (int dy = -SEARCH_RADIUS; dy <= SEARCH_RADIUS; dy++) {
                for (int dz = -SEARCH_RADIUS; dz <= SEARCH_RADIUS; dz++) {
                    double dist = dx * dx + dy * dy + dz * dz;
                    if (dist >= bestDist || dist > SEARCH_RADIUS * SEARCH_RADIUS) continue;
                    pos.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    if (level.getBlockState(pos).is(ore.blocks)) {
                        best = pos.immutable();
                        bestDist = dist;
                    }
                }
            }
        }
        return best;
    }

    BlockPos getOreTarget() {
        return this.target;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (this.prospectTicks > 0) {
            this.prospectTicks--;
            Ore ore = getOre();
            if (this.target != null && this.tickCount % 20 == 0 && !level.getBlockState(this.target).is(ore.blocks)) {
                // Someone mined it: sniff out the next one.
                this.target = findOre(level, ore);
            }
            if (this.prospectTicks == 0 || ore == Ore.NONE) {
                this.target = null;
                this.prospectTicks = 0;
                setOre(Ore.NONE);
            }
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level() instanceof ServerLevel level && this.isAlive() && !this.isBaby() && --this.giftTime <= 0) {
            ItemStack gift = switch (this.random.nextInt(6)) {
                case 0, 1, 2 -> new ItemStack(Items.FLINT);
                case 3, 4 -> new ItemStack(Items.IRON_NUGGET, 1 + this.random.nextInt(2));
                default -> new ItemStack(Items.GOLD_NUGGET);
            };
            this.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 1.6F);
            this.spawnAtLocation(level, gift);
            this.giftTime = nextGiftTime(this.random);
        }
    }

    /** Draws a short sparkle trail from the pebblin toward its ore. */
    void sparkle(ServerLevel level) {
        if (this.target == null) return;
        Vec3 from = this.position().add(0, 0.6, 0);
        Vec3 to = Vec3.atCenterOf(this.target);
        Vec3 step = to.subtract(from);
        double len = step.length();
        int points = (int) Math.min(16, Math.max(3, len * 2));
        for (int i = 1; i <= points; i++) {
            Vec3 p = from.add(step.scale(i / (double) points));
            level.sendParticles(ParticleTypes.WAX_ON, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.0);
        }
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY() + 0.8, this.getZ(), 4, 0.15, 0.1, 0.15, 0.02);
        this.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.9F, 1.2F + this.random.nextFloat() * 0.5F);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("GiftTime", this.giftTime);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.giftTime = input.getIntOr("GiftTime", nextGiftTime(this.random));
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return MiningDimension.PEBBLIN.create(level, EntitySpawnReason.BREEDING);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.AMETHYST_BLOCK_CHIME;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ARMADILLO_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.DECORATED_POT_BREAK;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.ARMADILLO_STEP, 0.15F, 1.3F);
    }

    @Override
    protected void playEatingSound() {
        this.playSound(SoundEvents.ARMADILLO_EAT, 0.8F, 1.4F);
    }

    @Override
    protected float getSoundVolume() {
        return 0.5F;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 400;
    }

    /** Walks to the ore it smelled, then stands there sparkling at it. */
    static class ProspectGoal extends Goal {
        private final Pebblin pebblin;
        private int cooldown;

        ProspectGoal(Pebblin pebblin) {
            this.pebblin = pebblin;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return this.pebblin.getOreTarget() != null && this.pebblin.prospectTicks > 0;
        }

        @Override
        public void start() {
            this.cooldown = 0;
        }

        @Override
        public void stop() {
            this.pebblin.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            BlockPos target = this.pebblin.getOreTarget();
            if (target == null) return;
            Vec3 center = Vec3.atCenterOf(target);
            this.pebblin.getLookControl().setLookAt(center.x, center.y, center.z);
            double dist = this.pebblin.position().distanceToSqr(center);
            if (dist > 6.0 && this.pebblin.tickCount % 10 == 0) {
                this.pebblin.getNavigation().moveTo(center.x, center.y, center.z, 1.1);
            }
            if (--this.cooldown <= 0 && this.pebblin.level() instanceof ServerLevel level) {
                this.cooldown = dist > 9.0 ? 40 : 25;
                this.pebblin.sparkle(level);
            }
        }
    }
}
