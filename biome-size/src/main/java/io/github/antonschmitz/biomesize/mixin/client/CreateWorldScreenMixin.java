package io.github.antonschmitz.biomesize.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.antonschmitz.biomesize.BiomeSize;
import io.github.antonschmitz.biomesize.client.BiomeSizeClient;
import java.nio.file.Path;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreateWorldScreen.class)
public abstract class CreateWorldScreenMixin {
    /** Every new Create World screen starts at vanilla sizes. */
    @Inject(method = "<init>", at = @At("TAIL"))
    private void biomesize$reset(CallbackInfo ci) {
        BiomeSizeClient.reset();
    }

    /** Saves the sizes into the new world folder before its server starts and reads them back. */
    @WrapOperation(method = "createNewWorld", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/worldselection/CreateWorldScreen;createNewWorldDirectory(Lnet/minecraft/client/Minecraft;Ljava/lang/String;Ljava/nio/file/Path;)Ljava/util/Optional;"))
    private Optional<LevelStorageSource.LevelStorageAccess> biomesize$saveSizes(Minecraft minecraft, String worldFolder, Path tempDataPackDir,
                                                                                Operation<Optional<LevelStorageSource.LevelStorageAccess>> original) {
        Optional<LevelStorageSource.LevelStorageAccess> access = original.call(minecraft, worldFolder, tempDataPackDir);
        access.ifPresent(a -> BiomeSize.write(a.getLevelPath(LevelResource.ROOT), BiomeSizeClient.settings()));
        return access;
    }
}
