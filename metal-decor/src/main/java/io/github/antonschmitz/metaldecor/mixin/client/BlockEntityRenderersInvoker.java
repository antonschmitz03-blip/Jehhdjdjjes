package io.github.antonschmitz.metaldecor.mixin.client;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Vanilla's renderer registration is private; the pedestal renderer goes in through it like the vanilla ones. */
@Mixin(BlockEntityRenderers.class)
public interface BlockEntityRenderersInvoker {
    @Invoker("register")
    static <T extends BlockEntity, S extends BlockEntityRenderState> void metaldecor$register(
            BlockEntityType<? extends T> type, BlockEntityRendererProvider<T, S> provider) {
        throw new AssertionError();
    }
}
