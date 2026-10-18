package io.github.antonschmitz.diamondmachines.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.zurrtum.create.content.kinetics.crusher.CrushingWheelControllerBlockEntity;
import io.github.antonschmitz.diamondmachines.DiamondCrushingWheelBlock;
import io.github.antonschmitz.diamondmachines.DiamondMachines;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/** Between two diamond crushing wheels: crush faster and roll a bonus for every output item. */
@Mixin(CrushingWheelControllerBlockEntity.class)
public abstract class CrushingWheelControllerBlockEntityMixin {
    @ModifyExpressionValue(method = "tick", at = @At(value = "FIELD",
            target = "Lcom/zurrtum/create/content/kinetics/crusher/CrushingWheelControllerBlockEntity;crushingspeed:F"))
    private float diamondmachines$fasterBetweenDiamondWheels(float speed) {
        return speed != 0 && diamondmachines$isDiamondPair() ? speed * DiamondMachines.CRUSHING_SPEED_MULTIPLIER : speed;
    }

    @ModifyExpressionValue(method = "applyRecipe", at = @At(value = "INVOKE",
            target = "Lcom/zurrtum/create/foundation/recipe/RecipeApplier;applyRecipeOn(Lnet/minecraft/util/RandomSource;ILnet/minecraft/world/item/crafting/RecipeInput;Lcom/zurrtum/create/foundation/recipe/CreateRollableRecipe;)Ljava/util/List;"))
    private List<ItemStack> diamondmachines$bonusOutput(List<ItemStack> results) {
        BlockEntity self = (BlockEntity) (Object) this;
        if (self.getLevel() == null || !diamondmachines$isDiamondPair()) {
            return results;
        }
        RandomSource random = self.getLevel().getRandom();
        List<ItemStack> boosted = new ArrayList<>();
        for (ItemStack stack : results) {
            int extra = 0;
            for (int i = 0; i < stack.getCount(); i++) {
                if (random.nextFloat() < DiamondMachines.CRUSHING_BONUS_CHANCE) {
                    extra++;
                }
            }
            int total = stack.getCount() + extra;
            // keep stacks within their size limit so nothing is lost when they go into the output slots
            while (total > 0) {
                int count = Math.min(total, stack.getMaxStackSize());
                boosted.add(stack.copyWithCount(count));
                total -= count;
            }
        }
        return boosted;
    }

    @Unique
    private boolean diamondmachines$isDiamondPair() {
        BlockEntity self = (BlockEntity) (Object) this;
        return self.getLevel() != null && DiamondCrushingWheelBlock.isDiamondPair(self.getLevel(), self.getBlockPos());
    }
}
