package io.pyke.vitri.finorza.inference.mixin.render;

import com.mojang.blaze3d.platform.InputConstants;
import io.pyke.vitri.finorza.inference.mixin.input.KeyboardHandlerAccessor;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static org.lwjgl.glfw.GLFW.GLFW_CURSOR;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_HIDDEN;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_NORMAL;

@Mixin(InputConstants.class)
public class InputConstantsMixin {
    @Overwrite // always hide the mouse, we are already showing a cursor in menus via MouseCursor
    public static void grabOrReleaseMouse(long window, int cursorValue, double xPos, double yPos) {
        GLFW.glfwSetCursorPos(window, xPos, yPos);
        GLFW.glfwSetInputMode(window, GLFW_CURSOR, cursorValue == GLFW_CURSOR_NORMAL ? GLFW_CURSOR_HIDDEN : cursorValue);
    }

    @Inject(method = "isKeyDown(JI)Z", at = @At(value = "HEAD"), cancellable = true)
    private static void injectInputConstants(long handle, int key, CallbackInfoReturnable<Boolean> cir) {
        if (key == 340 || key == 344) {
            // left or right shift
            cir.setReturnValue(((KeyboardHandlerAccessor) Minecraft.getInstance().keyboardHandler).vitri$isKeyPressed(340));
        }
    }
}
