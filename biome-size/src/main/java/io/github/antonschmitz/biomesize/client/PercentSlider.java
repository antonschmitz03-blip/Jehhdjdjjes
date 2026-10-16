package io.github.antonschmitz.biomesize.client;

import io.github.antonschmitz.biomesize.BiomeSize;
import java.util.function.IntConsumer;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

/** A percentage slider in 1% steps. */
public class PercentSlider extends AbstractSliderButton {
    private final int min;
    private final int max;
    private final String labelKey;
    private final IntConsumer onChange;

    public PercentSlider(int width, int min, int max, String labelKey, String tooltipKey, int percent, IntConsumer onChange) {
        super(0, 0, width, 20, Component.empty(), (BiomeSize.clamp(percent, min, max) - min) / (double) (max - min));
        this.min = min;
        this.max = max;
        this.labelKey = labelKey;
        this.onChange = onChange;
        setTooltip(Tooltip.create(Component.translatable(tooltipKey)));
        updateMessage();
    }

    private int percent() {
        return min + (int) Math.round(value * (max - min));
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
