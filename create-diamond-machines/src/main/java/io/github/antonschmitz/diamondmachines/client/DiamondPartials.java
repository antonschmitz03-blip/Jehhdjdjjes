package io.github.antonschmitz.diamondmachines.client;

import com.zurrtum.create.client.AllPartialModels;
import com.zurrtum.create.client.flywheel.lib.model.baked.PartialModel;
import io.github.antonschmitz.diamondmachines.DiamondMachines;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The moving parts of the diamond machines. Each model reuses Create's shape by naming it as parent and only swaps
 * the texture, so none of Create's assets are copied.
 */
public final class DiamondPartials {
    // Partial models must be created before models are baked, so these are made when the client mod starts.
    public static final PartialModel DRILL_HEAD = block("diamond_drill/head");
    public static final PartialModel CRUSHING_WHEEL = block("diamond_crushing_wheel/block");
    public static final PartialModel SAW_BLADE_HORIZONTAL_ACTIVE = block("diamond_saw/blade_horizontal_active");
    public static final PartialModel SAW_BLADE_HORIZONTAL_INACTIVE = block("diamond_saw/blade_horizontal_inactive");
    public static final PartialModel SAW_BLADE_HORIZONTAL_REVERSED = block("diamond_saw/blade_horizontal_reversed");
    public static final PartialModel SAW_BLADE_VERTICAL_ACTIVE = block("diamond_saw/blade_vertical_active");
    public static final PartialModel SAW_BLADE_VERTICAL_INACTIVE = block("diamond_saw/blade_vertical_inactive");
    public static final PartialModel SAW_BLADE_VERTICAL_REVERSED = block("diamond_saw/blade_vertical_reversed");

    private static final Map<PartialModel, PartialModel> DIAMOND_VERSION = new IdentityHashMap<>();

    static {
        DIAMOND_VERSION.put(AllPartialModels.DRILL_HEAD, DRILL_HEAD);
        DIAMOND_VERSION.put(AllPartialModels.CRUSHING_WHEEL, CRUSHING_WHEEL);
        DIAMOND_VERSION.put(AllPartialModels.SAW_BLADE_HORIZONTAL_ACTIVE, SAW_BLADE_HORIZONTAL_ACTIVE);
        DIAMOND_VERSION.put(AllPartialModels.SAW_BLADE_HORIZONTAL_INACTIVE, SAW_BLADE_HORIZONTAL_INACTIVE);
        DIAMOND_VERSION.put(AllPartialModels.SAW_BLADE_HORIZONTAL_REVERSED, SAW_BLADE_HORIZONTAL_REVERSED);
        DIAMOND_VERSION.put(AllPartialModels.SAW_BLADE_VERTICAL_ACTIVE, SAW_BLADE_VERTICAL_ACTIVE);
        DIAMOND_VERSION.put(AllPartialModels.SAW_BLADE_VERTICAL_INACTIVE, SAW_BLADE_VERTICAL_INACTIVE);
        DIAMOND_VERSION.put(AllPartialModels.SAW_BLADE_VERTICAL_REVERSED, SAW_BLADE_VERTICAL_REVERSED);
    }

    private DiamondPartials() {
    }

    static void init() {
        // loads the class, which creates the partial models above
    }

    /** Create's part, or our diamond version of it when the block is one of ours. */
    public static PartialModel forState(PartialModel original, BlockState state) {
        if (state == null || !isDiamond(state)) {
            return original;
        }
        return DIAMOND_VERSION.getOrDefault(original, original);
    }

    public static boolean isDiamond(BlockState state) {
        return state != null && state.getBlock() instanceof io.github.antonschmitz.diamondmachines.DiamondVariant;
    }

    /** The animated diamond saw blade texture, for marking it active like Create does for the stonecutter blade. */
    public static TextureAtlasSprite sawSprite() {
        return Minecraft.getInstance().getAtlasManager().get(Sheets.BLOCKS_MAPPER.apply(DiamondMachines.id("diamond_saw")));
    }

    private static PartialModel block(String path) {
        return PartialModel.of(DiamondMachines.id("block/" + path));
    }
}
