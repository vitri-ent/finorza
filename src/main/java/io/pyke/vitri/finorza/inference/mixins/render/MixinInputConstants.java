package io.pyke.vitri.finorza.inference.mixins.render;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.pyke.vitri.finorza.inference.api.IKeyboardHandler;
import io.pyke.vitri.finorza.inference.client.MouseCursor;

import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_HIDDEN;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_NORMAL;

@Mixin(InputConstants.class)
public class MixinInputConstants {
	/**
	 * Overwrite grabOrReleaseMouse to always hide the mouse.
	 *
	 * @author decahedron
	 * @reason We are already showing a cursor in menus via the {@link MouseCursor}.
	 */
	@Overwrite
	public static void grabOrReleaseMouse(long window, int cursorValue, double xPos, double yPos) {
		GLFW.glfwSetCursorPos(window, xPos, yPos);
		if (cursorValue == GLFW_CURSOR_NORMAL) {
			cursorValue = GLFW_CURSOR_HIDDEN;
		}
		GLFW.glfwSetInputMode(window, 208897, cursorValue);
	}

	@Inject(
		method = "isKeyDown(JI)Z", at = @At(value = "HEAD"), cancellable = true
	)
	private static void injectInputConstants(long handle, int key, CallbackInfoReturnable<Boolean> cir) {
		if (key == 340 || key == 344) {
			// left or right shift
			cir.setReturnValue(((IKeyboardHandler) Minecraft.getInstance().keyboardHandler).vitri$isKeyPressed(340));
		}
	}
}
