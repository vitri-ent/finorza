package io.pyke.vitri.finorza.inference.gui;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.screens.OptionsSubScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.util.FormattedCharSequence;

import io.pyke.vitri.finorza.inference.config.Config;
import io.pyke.vitri.finorza.inference.config.ConfigManager;

public class ModOptionsScreen extends OptionsSubScreen {
	private final Screen previous;
	private OptionsList list;

	public ModOptionsScreen(Screen previous) {
		super(previous, Minecraft.getInstance().options, new TextComponent("Finorza Options"));
		this.previous = previous;
	}

	protected void init() {
		this.minecraft.keyboardHandler.setSendRepeatsToGui(true);
		this.list = new OptionsList(this.minecraft, this.width, this.height, 32, this.height - 32, 25);
		this.list.addSmall(Config.asOptions());
		this.children.add(this.list);
		this.addButton(
			new Button(this.width / 2 - 100, this.height - 27, 200, 20, CommonComponents.GUI_DONE, (button) -> {
				ConfigManager.save();
				this.minecraft.setScreen(this.previous);
			}));
	}

	public void render(PoseStack matrices, int mouseX, int mouseY, float delta) {
		this.renderBackground(matrices);
		this.list.render(matrices, mouseX, mouseY, delta);
		drawCenteredString(matrices, this.font, this.title, this.width / 2, 5, 0xFFFFFF);
		super.render(matrices, mouseX, mouseY, delta);
		List<FormattedCharSequence> tooltip = tooltipAt(this.list, mouseX, mouseY);
		if (tooltip != null) {
			this.renderTooltip(matrices, tooltip, mouseX, mouseY);
		}
	}

	public void removed() {
		ConfigManager.save();
	}
}
