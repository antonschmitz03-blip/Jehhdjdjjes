package io.github.antonschmitz.oreshrines;

import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

/** What a shrine does. The offering is the item you right-click the core with to switch to that effect. */
public enum ShrineEffect implements StringRepresentable {
    NONE(null, 0xA0A0A0, ChatFormatting.GRAY),
    HARVEST(Items.GOLDEN_CARROT, 0x86D93A, ChatFormatting.GREEN),
    WARD(Items.PHANTOM_MEMBRANE, 0x7C8CF0, ChatFormatting.BLUE),
    MENDING(Items.GOLDEN_APPLE, 0xF0628F, ChatFormatting.LIGHT_PURPLE),
    HEARTH(Items.BLAZE_ROD, 0xFF8C1F, ChatFormatting.GOLD),
    PASTURE(Items.WHEAT, 0xE8CF5E, ChatFormatting.YELLOW),
    WISDOM(Items.EXPERIENCE_BOTTLE, 0xA6FF4D, ChatFormatting.AQUA);

    public static final ShrineEffect[] SELECTABLE = {HARVEST, WARD, MENDING, HEARTH, PASTURE, WISDOM};

    private final @Nullable Item offering;
    private final int color;
    private final ChatFormatting formatting;

    ShrineEffect(@Nullable Item offering, int color, ChatFormatting formatting) {
        this.offering = offering;
        this.color = color;
        this.formatting = formatting;
    }

    public @Nullable Item offering() {
        return offering;
    }

    public int color() {
        return color;
    }

    public ChatFormatting formatting() {
        return formatting;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    @Override
    public String getSerializedName() {
        return id();
    }

    public String nameKey() {
        return "oreshrines.effect." + id();
    }

    public String descriptionKey() {
        return "oreshrines.effect." + id() + ".description";
    }

    public static ShrineEffect byId(String id) {
        for (ShrineEffect effect : values()) {
            if (effect.id().equals(id)) {
                return effect;
            }
        }
        return NONE;
    }

    public static @Nullable ShrineEffect byOffering(ItemStack stack) {
        for (ShrineEffect effect : SELECTABLE) {
            if (stack.is(effect.offering)) {
                return effect;
            }
        }
        return null;
    }
}
