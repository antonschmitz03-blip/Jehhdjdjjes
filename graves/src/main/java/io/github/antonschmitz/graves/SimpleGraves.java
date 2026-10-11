package io.github.antonschmitz.graves;

import io.github.antonschmitz.graves.network.GraveWaypointPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleGraves implements ModInitializer {
    public static final String MOD_ID = "simplegraves";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /** Runs after the default phase, so drops other mods spawn in their own death listeners are caught too. */
    private static final Identifier LATE_PHASE = id("late");

    public static Block GRAVE;
    public static BlockEntityType<GraveBlockEntity> GRAVE_BLOCK_ENTITY;

    @Override
    public void onInitialize() {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id("grave"));
        GRAVE = Registry.register(BuiltInRegistries.BLOCK, key, new GraveBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .sound(SoundType.STONE)
                // Unbreakable in survival, explosion-proof and immovable; only the owner can empty it.
                .strength(-1.0F, 3600000.0F)
                .pushReaction(PushReaction.BLOCK)
                .noLootTable()
                .noOcclusion()
                .setId(key)));
        GRAVE_BLOCK_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id("grave"),
                FabricBlockEntityTypeBuilder.create(GraveBlockEntity::new, GRAVE).build());

        PayloadTypeRegistry.clientboundPlay().register(GraveWaypointPayload.TYPE, GraveWaypointPayload.STREAM_CODEC);

        ServerLivingEntityEvents.AFTER_DEATH.addPhaseOrdering(net.fabricmc.fabric.api.event.Event.DEFAULT_PHASE, LATE_PHASE);
        ServerLivingEntityEvents.AFTER_DEATH.register(LATE_PHASE, (entity, source) -> {
            if (entity instanceof ServerPlayer player) {
                DeathCapture.finish(player);
            }
        });
        // Safety net: never leave a capture open past the tick the player died in.
        ServerTickEvents.END_SERVER_TICK.register(server -> DeathCapture.finishAny());

        if (Boolean.getBoolean("simplegraves.selfTest")) {
            ServerLifecycleEvents.SERVER_STARTED.register(SelfTest::run);
        }
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
