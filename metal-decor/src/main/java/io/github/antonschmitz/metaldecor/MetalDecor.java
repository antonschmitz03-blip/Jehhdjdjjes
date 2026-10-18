package io.github.antonschmitz.metaldecor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WaterloggedTransparentBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

public class MetalDecor implements ModInitializer {
    public static final String MOD_ID = "metaldecor";

    /** Strength and look of each metal, copied from its vanilla block. */
    public enum Metal {
        IRON("iron", MapColor.METAL, 5.0F),
        GOLD("gold", MapColor.GOLD, 3.0F),
        DIAMOND("diamond", MapColor.DIAMOND, 5.0F);

        final String name;
        final MapColor color;
        final float hardness;

        Metal(String name, MapColor color, float hardness) {
            this.name = name;
            this.color = color;
            this.hardness = hardness;
        }
    }

    /** Every decor block in creative-tab order. */
    public static final List<Block> BLOCKS = new ArrayList<>();
    public static final List<Block> LIGHTS = new ArrayList<>();
    public static final List<Block> PEDESTALS = new ArrayList<>();
    public static BlockEntityType<PedestalBlockEntity> PEDESTAL_ENTITY;

    @Override
    public void onInitialize() {
        for (Metal metal : Metal.values()) {
            String m = metal.name;
            for (String[] style : new String[][] {
                    {m + "_plating", m + "_plating_stairs", m + "_plating_slab"},
                    {m + "_tiles", m + "_tile_stairs", m + "_tile_slab"},
                    {"cut_" + m, "cut_" + m + "_stairs", "cut_" + m + "_slab"}}) {
                Block full = register(style[0], Block::new, metal(metal), BLOCKS);
                register(style[1], p -> new StairBlock(full.defaultBlockState(), p), metal(metal), BLOCKS);
                register(style[2], SlabBlock::new, metal(metal), BLOCKS);
            }
            register(m + "_pillar", RotatedPillarBlock::new, metal(metal), BLOCKS);
            register(m + "_grate", WaterloggedTransparentBlock::new, metal(metal).noOcclusion()
                    .isValidSpawn((state, level, pos, type) -> false)
                    .isRedstoneConductor((state, level, pos) -> false)
                    .isSuffocating((state, level, pos) -> false)
                    .isViewBlocking((state, level, pos) -> false), BLOCKS);
            Block pedestal = register(m + "_pedestal", PedestalBlock::new, metal(metal).noOcclusion(), BLOCKS);
            PEDESTALS.add(pedestal);
        }

        register("gold_lantern", LanternBlock::new, lantern(15), LIGHTS);
        register("gold_soul_lantern", LanternBlock::new, lantern(10), LIGHTS);
        register("diamond_lantern", LanternBlock::new, lantern(15).mapColor(MapColor.DIAMOND), LIGHTS);
        register("gold_chain", ChainBlock::new, BlockBehaviour.Properties.of().forceSolidOn().requiresCorrectToolForDrops()
                .strength(5.0F, 6.0F).sound(SoundType.CHAIN).noOcclusion(), LIGHTS);
        register("gold_bars", IronBarsBlock::new, BlockBehaviour.Properties.of().requiresCorrectToolForDrops()
                .strength(5.0F, 6.0F).sound(SoundType.METAL).noOcclusion(), LIGHTS);

        PEDESTAL_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id("pedestal"),
                FabricBlockEntityTypeBuilder.create(PedestalBlockEntity::new, PEDESTALS.toArray(Block[]::new)).build());

        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register(output -> BLOCKS.forEach(output::accept));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output -> LIGHTS.forEach(output::accept));
    }

    private static BlockBehaviour.Properties metal(Metal metal) {
        return BlockBehaviour.Properties.of().mapColor(metal.color).instrument(NoteBlockInstrument.IRON_XYLOPHONE)
                .requiresCorrectToolForDrops().strength(metal.hardness, 6.0F).sound(SoundType.METAL);
    }

    private static BlockBehaviour.Properties lantern(int light) {
        return BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).forceSolidOn().requiresCorrectToolForDrops()
                .strength(3.5F).sound(SoundType.LANTERN).lightLevel(state -> light).noOcclusion()
                .pushReaction(PushReaction.DESTROY);
    }

    private static Block register(String name, Function<BlockBehaviour.Properties, Block> factory,
                                  BlockBehaviour.Properties properties, List<Block> group) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id(name));
        Block block = Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key)));
        Items.registerBlock(block);
        group.add(block);
        return block;
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
