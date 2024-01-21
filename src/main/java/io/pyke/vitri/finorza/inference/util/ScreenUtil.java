package io.pyke.vitri.finorza.inference.util;

import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

public final class ScreenUtil {
	public static boolean isAgentControllableScreen(Screen screen) {
		return screen == null || screen instanceof AbstractContainerScreen<?>;
	}
}
