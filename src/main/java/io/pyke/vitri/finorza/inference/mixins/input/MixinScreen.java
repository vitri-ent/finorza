package io.pyke.vitri.finorza.inference.mixins.input;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import io.pyke.vitri.finorza.inference.api.IKeyboardHandler;

@Mixin(Screen.class)
public class MixinScreen {
	/**
	 * @author decahedron
	 * @reason need this override for container GUIs
	 */
	@Overwrite
	public static boolean hasShiftDown() {
		IKeyboardHandler handler = ((IKeyboardHandler) Minecraft.getInstance().keyboardHandler);
		return handler.vitri$isKeyPressed(340) || handler.vitri$isKeyPressed(344);
	}
}
