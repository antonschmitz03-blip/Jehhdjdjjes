package io.github.antonschmitz.tbaddons.mixin.client;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
    @Accessor("imageWidth")
    int tbaddons$getImageWidth();

    @Mutable
    @Accessor("imageWidth")
    void tbaddons$setImageWidth(int imageWidth);

    @Accessor("leftPos")
    int tbaddons$getLeftPos();

    @Accessor("topPos")
    int tbaddons$getTopPos();

    @Accessor("menu")
    AbstractContainerMenu tbaddons$getMenu();
}
