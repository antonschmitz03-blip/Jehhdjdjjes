package io.github.antonschmitz.biomesize.client;

import io.github.antonschmitz.biomesize.BiomeSize;
import java.util.function.IntConsumer;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

/** A 10% to 400% slider in 1% steps. */
public class PercentSlider extends AbstractSliderButton {
    private static final int RANGE = BiomeSize.MAX_PERCENT - BiomeSize.MIN_PERCENT;
    private final String labelKey;
    private final IntConsumer onChange;

    public PercentSlider(int width, String labelKey, String tooltipKey, int percent, IntConsumer onChange) {
        super(0, 0, width, 20, Component.empty(), toValue(percent));
        this.labelKey = labelKey;
        this.onChange = onChange;
        setTooltip(Tooltip.create(Component.translatable(tooltipKey)));
        updateMessage();
    }

    private static double toValue(int percent) {
        return (BiomeSize.clamp(percent) - BiomeSize.MIN_PERCENT) / (double) RANGE;
    }

    private int percent() {
        return BiomeSize.MIN_PERCENT + (int) Math.round(value * RANGE);
    }

    @Override
    protected void updateMessage() {
        setMessage(Component.translatable(labelKey, percent()));
    }

    @Override
    protected void applyValue() {
        onChange.accept(percent());
    }
}
