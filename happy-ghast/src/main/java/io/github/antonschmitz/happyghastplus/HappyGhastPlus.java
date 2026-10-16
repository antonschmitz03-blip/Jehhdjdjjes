package io.github.antonschmitz.happyghastplus;

import io.github.antonschmitz.happyghastplus.item.GhastNestItem;
import io.github.antonschmitz.happyghastplus.item.GhastWhistleItem;
import java.util.Set;
import java.util.function.Function;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HappyGhastPlus implements ModInitializer {
    public static final String MOD_ID = "happyghastplus";
    /**
     * These features started out in Vanilla+ Tweaks. The saved data they leave on ghasts (chest contents, owner, upgrade)
     * keeps its old "vplustweaks" keys, and the old item and block ids are aliased, so existing worlds carry over.
     */
    public static final String LEGACY_ID = "vplustweaks";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Item GHAST_SPEED_UPGRADE;
    public static Item GHAST_WHISTLE;
    public static Block ROPE_LADDER;
    public static Block GHAST_NEST;
    public static Item GHAST_NEST_ITEM;
    /** Only drawn over a sleeping ghast's face; not obtainable. */
    public static Item GHAST_EYELIDS;
    public static Item GHAST_SEARCHLIGHT;
    public static Block SEARCHLIGHT_LIGHT;
    /** Pieces of the rope ladder the ghast draws; not obtainable. */
    public static Item GHAST_ROPE_SEGMENT;
    public static Item GHAST_ROPE_TOP;

    @Override
    public void onInitialize() {
        ResourceKey<Block> ropeLadderKey = ResourceKey.create(Registries.BLOCK, id("rope_ladder"));
        ROPE_LADDER = Registry.register(BuiltInRegistries.BLOCK, ropeLadderKey, new RopeLadderBlock(BlockBehaviour.Properties.of()
                .setId(ropeLadderKey).strength(0.4F).sound(SoundType.LADDER).noOcclusion().noLootTable()));
        ResourceKey<Block> nestKey = ResourceKey.create(Registries.BLOCK, id("ghast_nest"));
        GHAST_NEST = Registry.register(BuiltInRegistries.BLOCK, nestKey, new GhastNestBlock(BlockBehaviour.Properties.of()
                .setId(nestKey).strength(0.5F, 1200.0F).sound(SoundType.GRASS).noOcclusion().noLootTable()
                .pushReaction(PushReaction.IMMOVEABLE)));
        ResourceKey<Block> lightKey = ResourceKey.create(Registries.BLOCK, id("searchlight_light"));
        SEARCHLIGHT_LIGHT = Registry.register(BuiltInRegistries.BLOCK, lightKey, new SearchlightLightBlock(BlockBehaviour.Properties.of()
                .setId(lightKey).replaceable().noCollision().noLootTable().noOcclusion().strength(-1.0F, 3600000.0F)
                .pushReaction(PushReaction.POPPED).lightLevel(state -> state.getValue(SearchlightLightBlock.LEVEL))));
        GHAST_SPEED_UPGRADE = register("ghast_speed_upgrade", Item::new);
        GHAST_WHISTLE = register("ghast_whistle", properties -> new GhastWhistleItem(properties.stacksTo(1)));
        GHAST_NEST_ITEM = register("ghast_nest", GhastNestItem::new);
        GHAST_EYELIDS = register("ghast_eyelids", Item::new);
        GHAST_SEARCHLIGHT = register("ghast_searchlight", properties -> new Item(properties.stacksTo(16)));
        GHAST_ROPE_SEGMENT = register("ghast_rope_segment", Item::new);
        GHAST_ROPE_TOP = register("ghast_rope_top", Item::new);
        ArmoredHarnesses.register(HappyGhastPlus::register);

        for (String name : new String[] {"ghast_speed_upgrade", "ghast_whistle"}) {
            ((MappedRegistry<Item>) BuiltInRegistries.ITEM).addAlias(Identifier.fromNamespaceAndPath(LEGACY_ID, name), id(name));
        }
        ((MappedRegistry<Block>) BuiltInRegistries.BLOCK).addAlias(Identifier.fromNamespaceAndPath(LEGACY_ID, "rope_ladder"), id("rope_ladder"));

        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> {
            output.accept(GHAST_SPEED_UPGRADE);
            output.accept(GHAST_WHISTLE);
            output.accept(GHAST_NEST_ITEM);
            output.accept(GHAST_SEARCHLIGHT);
            ArmoredHarnesses.all().forEach(output::accept);
        });

        GhastChest.register();
        GhastFeatures.register();
        RopeLadders.register();
        GhastNests.register();
        GhastSearchlight.register();

        if (Boolean.getBoolean("happyghastplus.selfTest")) {
            ServerLifecycleEvents.SERVER_STARTED.register(HappyGhastPlus::selfTest);
        }
    }

    /**
     * CI smoke test: force-load the mixin target, check that the old Vanilla+ Tweaks ids still resolve, and place and
     * break a ghast nest.
     */
    private static void selfTest(MinecraftServer server) {
        try {
            Class.forName("net.minecraft.world.entity.animal.happyghast.HappyGhast");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
        check(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(LEGACY_ID, "ghast_whistle")) == GHAST_WHISTLE, "ghast whistle alias");
        check(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(LEGACY_ID, "ghast_speed_upgrade")) == GHAST_SPEED_UPGRADE, "speed upgrade alias");
        check(BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(LEGACY_ID, "rope_ladder")) == ROPE_LADDER, "rope ladder alias");
        check(BuiltInRegistries.ITEM.getValue(id("ghast_nest")) == GHAST_NEST_ITEM, "ghast nest item");
        check(BuiltInRegistries.ITEM.getValue(id("ghast_eyelids")) == GHAST_EYELIDS, "ghast eyelids item");
        check(BuiltInRegistries.ITEM.getValue(id("ghast_rope_top")) == GHAST_ROPE_TOP, "ghast rope item");
        check(BuiltInRegistries.BLOCK.getValue(id("ghast_nest")) == GHAST_NEST, "ghast nest block");
        check(BuiltInRegistries.ITEM.getValue(id("ghast_searchlight")) == GHAST_SEARCHLIGHT, "searchlight item");
        check(BuiltInRegistries.ITEM.getValue(id("red_netherite_harness")) == ArmoredHarnesses.get(ArmoredHarnesses.Tier.NETHERITE, DyeColor.RED),
                "netherite harness item");
        check(new ItemStack(ArmoredHarnesses.get(ArmoredHarnesses.Tier.IRON, DyeColor.WHITE)).is(ItemTags.HARNESSES), "armored harness tag");
        nestSelfTest(server.overworld());
        searchlightSelfTest(server.overworld());
        LOGGER.info("happyghastplus self test passed");
    }

    private static void nestSelfTest(ServerLevel level) {
        BlockPos center = new BlockPos(0, level.getMaxY() - 8, 0);
        check(GhastNestBlock.placeNest(level, center), "ghast nest placed");
        check(countNestCells(level, center) == GhastNestBlock.CELLS, "ghast nest has " + GhastNestBlock.CELLS + " cells");
        check(!GhastNestBlock.placeNest(level, center.offset(2, 0, 0)), "overlapping ghast nest refused");

        BlockPos broken = center.offset(-2, 0, 1);
        BlockState state = level.getBlockState(broken);
        GhastNestBlock.breakNest(level, broken, state, false);
        level.setBlock(broken, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        check(countNestCells(level, center) == 0, "broken ghast nest fully removed");
    }

    private static void searchlightSelfTest(ServerLevel level) {
        BlockPos center = new BlockPos(32, level.getMaxY() - 8, 32);
        Set<BlockPos> lights = GhastSearchlight.place(level, center, Direction.UP, Set.of());
        check(lights.size() == GhastSearchlight.ringSize(), "searchlight ring placed " + lights.size() + " of " + GhastSearchlight.ringSize());
        check(level.getBlockState(center).is(SEARCHLIGHT_LIGHT) && level.getBlockState(center).getValue(SearchlightLightBlock.LEVEL) == 15,
                "searchlight centre light");
        check(!GhastSearchlight.isClaimed(level, center), "unclaimed searchlight light");
        GhastSearchlight.removeBlocks(level, lights, Set.of());
        for (BlockPos pos : lights) {
            check(!level.getBlockState(pos).is(SEARCHLIGHT_LIGHT), "searchlight light removed");
        }
    }

    private static int countNestCells(ServerLevel level, BlockPos center) {
        int count = 0;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-4, 0, -4), center.offset(4, 0, 4))) {
            if (level.getBlockState(pos).is(GHAST_NEST)) {
                count++;
            }
        }
        return count;
    }

    private static void check(boolean ok, String what) {
        if (!ok) {
            throw new IllegalStateException("happyghastplus self test failed: " + what);
        }
    }

    static Identifier id(String name) {
        return Identifier.fromNamespaceAndPath(MOD_ID, name);
    }

    private static Item register(String name, Function<Item.Properties, Item> factory) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id(name));
        return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(new Item.Properties().setId(key)));
    }
}
