package io.github.antonschmitz.waystones;

import io.github.antonschmitz.waystones.network.WaystoneWaypointsPayload;
import java.util.function.Function;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleWaystones implements ModInitializer {
    public static final String MOD_ID = "simplewaystones";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Block WAYSTONE;
    public static Item WAYSTONE_ITEM;
    public static Item WARP_SCROLL;

    @Override
    public void onInitialize() {
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id("waystone"));
        WAYSTONE = Registry.register(BuiltInRegistries.BLOCK, blockKey, new WaystoneBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .sound(SoundType.STONE)
                .requiresCorrectToolForDrops()
                // As hard as stone bricks to mine, but explosions can't destroy it.
                .strength(1.5F, 1200.0F)
                .lightLevel(state -> 7)
                .noOcclusion()
                .setId(blockKey)));
        WAYSTONE_ITEM = registerItem("waystone", properties -> new BlockItem(WAYSTONE, properties.useBlockDescriptionPrefix()));
        WARP_SCROLL = registerItem("warp_scroll", properties -> new WarpScrollItem(properties.stacksTo(16).rarity(Rarity.UNCOMMON)));

        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output -> output.accept(WAYSTONE_ITEM));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> output.accept(WARP_SCROLL));

        // Only the person who placed a waystone (or someone in creative mode) can break it.
        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
            if (!state.is(WAYSTONE) || !(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
                return true;
            }
            WaystoneData.Waystone waystone = WaystoneData.get().at(serverLevel, WaystoneBlock.lowerPos(state, pos));
            if (waystone == null || serverPlayer.isCreative() || serverPlayer.getUUID().equals(waystone.owner())) {
                return true;
            }
            serverPlayer.sendSystemMessage(Component.translatable("simplewaystones.message.not_yours", waystone.ownerName())
                    .withStyle(ChatFormatting.RED), true);
            return false;
        });
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
            if (state.is(WAYSTONE) && level instanceof ServerLevel serverLevel) {
                WaystoneData.get().remove(serverLevel, WaystoneBlock.lowerPos(state, pos));
            }
        });

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            WaystoneConfig.load();
            WaystoneData.load(server);
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            WaystoneData.unload();
            WaypointSync.clear();
        });
        PayloadTypeRegistry.clientboundPlay().register(WaystoneWaypointsPayload.TYPE, WaystoneWaypointsPayload.STREAM_CODEC);
        ServerTickEvents.END_SERVER_TICK.register(WaypointSync::tick);

        if (Boolean.getBoolean("simplewaystones.selfTest")) {
            ServerLifecycleEvents.SERVER_STARTED.register(SelfTest::run);
        }
    }

    private static Item registerItem(String name, Function<Item.Properties, Item> factory) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id(name));
        return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(new Item.Properties().setId(key)));
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
