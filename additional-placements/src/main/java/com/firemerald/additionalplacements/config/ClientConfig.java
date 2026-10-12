package com.firemerald.additionalplacements.config;

import com.firemerald.additionalplacements.generation.GenerationType;
import com.firemerald.additionalplacements.generation.Registration;
import net.neoforged.neoforge.common.ModConfigSpec;

public class ClientConfig {
	public final ModConfigSpec.BooleanValue defaultPlacementLogicState, loginPlacementLogicStateMessage, togglePlacementLogicStateMessage, enablePlacementHighlight;
	public final ModConfigSpec.LongValue toggleQuickpressTime;
	public final ModConfigSpec.ConfigValue<String> gridColor, gridColorHCB, gridColorHC, previewColor, previewColorHCB, previewColorHC;
	private float[] gridColorVal = {.4f, 0, 0, 0}, gridColorHCBVal = {1, 0, 0, 0}, gridColorHCVal = {1, 0.88f, 1, 0.34f}, previewColorVal = {.4f, 1, 1, 1}, previewColorHCBVal = {1, 0, 0, 0}, previewColorHCVal = {1, .46f, .34f, 1}; //order intentionally left reversed to aid in capturing missed config load events

	public ClientConfig(ModConfigSpec.Builder builder) {
        builder.comment("Client settings").push("client");
        defaultPlacementLogicState = builder
        		.comment("Default enabled state for Additional Placement placement logic. Please note that this value takes effect any time you load a world or log in to a server.")
        		.define("default_placement_logic_state", true);
        loginPlacementLogicStateMessage = builder
        		.comment("Whether to display the logic placement enabled state when first initially loading up a world or logging in to a server. This does NOT stop it from showing when toggled via the keybind later.")
        		.define("login_placement_logic_state_message", true);
        togglePlacementLogicStateMessage = builder
        		.comment("Whether to display the logic placement enabled state when toggled with the keybind. This does NOT stop it from showing when you initially load up a world or log in to a server.")
        		.define("toggle_placement_logic_state_message", true);
        toggleQuickpressTime = builder
        		.comment("The length of time in milliseconds for which the placement toggle key must be held for it to automatically return to the previous state when the key is released. setting to 0 turns the key into hold only, setting it to a high value (such as 1000000) will make it generally behave as always a toggle")
        		.defineInRange("toggle_quickpress_time", 500L, 0, Long.MAX_VALUE);
        enablePlacementHighlight = builder
        		.comment("Whether to enable the rendering of the placement grid and/or preview.")
        		.define("enable_placement_highlights", true);
		gridColor = builder
				.comment("The color of the placement grid, in AARRGGBB hex format.")
				.define("grid_color", "66000000", APConfigs::isColorString);
		gridColorHCB = builder
				.comment("The color of the placement grid background in high contrast mode, in AARRGGBB hex format.")
				.define("grid_color_high_constrast_background", "FF000000", APConfigs::isColorString);
		gridColorHC = builder
				.comment("The color of the placement grid foreground in high contrast mode, in AARRGGBB hex format.")
				.define("grid_color_high_constrast_foreground", "FF57FFE1", APConfigs::isColorString);
        previewColor = builder
        		.comment("The color of the placement preview (currently used in stairs without mixed placement), in AARRGGBB hex format.")
        		.define("preview_color", "66FFFFFF", APConfigs::isColorString);
		previewColorHCB = builder
				.comment("The color of the placement preview background in high contrast mode (currently used in stairs without mixed placement), in AARRGGBB hex format.")
				.define("preview_color_high_constrast_background", "FF000000", APConfigs::isColorString);
		previewColorHC = builder
				.comment("The color of the placement preview foreground in high contrast mode (currently used in stairs without mixed placement), in AARRGGBB hex format.")
				.define("preview_color_high_constrast_foreground", "FFFF5775", APConfigs::isColorString);
        Registration.buildConfig(builder, GenerationType::buildClientConfig);
	}

	public void onConfigLoaded() {
		gridColorVal = APConfigs.parseColorString(gridColor);
		gridColorHCBVal = APConfigs.parseColorString(gridColorHCB);
		gridColorHCVal = APConfigs.parseColorString(gridColorHC);
		previewColorVal = APConfigs.parseColorString(previewColor);
		previewColorHCBVal = APConfigs.parseColorString(previewColorHCB);
		previewColorHCVal = APConfigs.parseColorString(previewColorHC);
		Registration.forEach(GenerationType::onClientConfigLoaded);
	}

	public float[] gridColor() {
		return gridColorVal;
	}

	public float[] gridColorHCB() {
		return gridColorHCBVal;
	}

	public float[] gridColorHC() {
		return gridColorHCVal;
	}

	public float[] previewColor() {
		return previewColorVal;
	}

	public float[] previewColorHCB() {
		return previewColorHCBVal;
	}

	public float[] previewColorHC() {
		return previewColorHCVal;
	}
}