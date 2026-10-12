package com.firemerald.additionalplacements.client.gui.screen;

import com.firemerald.additionalplacements.client.gui.ConnectionErrorsList;
import com.firemerald.additionalplacements.util.MessageTree;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class ConnectionErrorsScreen extends Screen {
	private final MessageTree rootError;
	private final Screen nextScreen;
    private ConnectionErrorsList errorList;
    private Button okButton;

	public ConnectionErrorsScreen(MessageTree rootError, Screen nextScreen) {
		super(Component.translatable("msg.additionalplacements.configurationerrors"));
		this.rootError = rootError;
		this.nextScreen = nextScreen;
	}

    @Override
    public void init() {
        super.init();
        clearWidgets();
        errorList = new ConnectionErrorsList(this, 10, 10, width - 20, height - 40, rootError);
        okButton = Button.builder(CommonComponents.GUI_OK, (button) -> minecraft.setScreen(nextScreen)).bounds(10, height - 30, width - 20, 20).build();
        addWidget(errorList);
        addWidget(okButton);
        setFocused(errorList);
    }

    @Override
    public @NotNull Font getFont() {
        return font;
    }

	public Minecraft getMinecraft() {
		return this.minecraft;
	}
}
