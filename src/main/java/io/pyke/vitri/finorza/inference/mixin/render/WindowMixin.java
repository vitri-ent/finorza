package io.pyke.vitri.finorza.inference.mixin.render;

import com.mojang.blaze3d.platform.DisplayData;
import com.mojang.blaze3d.platform.ScreenManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.platform.WindowEventHandler;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static org.lwjgl.glfw.GLFW.GLFW_DECORATED;
import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;

@Mixin(Window.class)
public abstract class WindowMixin implements WindowAccessor {
    @Shadow
    @Final
    private long window;
    @Shadow
    @Final
    private WindowEventHandler eventHandler;
    @Shadow
    private int framebufferHeight;
    @Shadow
    private int framebufferWidth;
    @Shadow
    private int height;
    @Shadow
    private int width;

    @Shadow
    protected abstract void onFramebufferResize(long window, int framebufferWidth, int framebufferHeight);

    @Shadow
    protected abstract void onResize(long window, int width, int height);

    @Shadow
    protected abstract void refreshFramebufferSize();

    @Shadow
    public abstract void updateDisplay();

    @Unique
    private boolean vitri$isFullscreen = false;

    @Inject(
            method = "<init>(Lcom/mojang/blaze3d/platform/WindowEventHandler;Lcom/mojang/blaze3d/platform/ScreenManager;Lcom/mojang/blaze3d/platform/DisplayData;Ljava/lang/String;Ljava/lang/String;)V",
            at = @At(
                    value = "INVOKE",
                    shift = At.Shift.BEFORE,
                    target = "Lorg/lwjgl/glfw/GLFW;glfwCreateWindow(IILjava/lang/CharSequence;JJ)J"
            )
    )
    public void injectCreate(
            WindowEventHandler eventHandler, ScreenManager screenManager, DisplayData displayData,
            String preferredFullscreenVideoMode, String title, CallbackInfo ci
    ) {
        GLFW.glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
    }

    @Overwrite
    private void onFocus(long window, boolean hasFocus) {
        if (window == this.window) { // make the game think the window is always focused, so we can run headless
            this.eventHandler.setWindowActive(true);
        }
    }

    @Inject(method = "calculateScale(IZ)I", at = @At(value = "RETURN"), cancellable = true)
    private void calculateScale(int guiScale, boolean forceUnicode, CallbackInfoReturnable<Integer> cir) {
        final double frameBufferToSizeRatio = (double) this.framebufferWidth / this.width;
        cir.setReturnValue((int) (cir.getReturnValue() * frameBufferToSizeRatio));
    }

    @Redirect(method = "setMode()V", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwGetWindowMonitor(J)J"))
    private long redirectGetWindowMonitor(long window) {
        return this.vitri$isFullscreen ? 1 : 0;
    }

    @Redirect(
            method = "setMode()V",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwSetWindowMonitor(JJIIIII)V")
    )
    private void redirectSetWindowMonitor(long window, long monitor, int xpos, int ypos, int width, int height, int refreshRate) {
        this.vitri$isFullscreen = monitor != 0;
        GLFW.glfwSetWindowAttrib(window, GLFW_DECORATED, this.vitri$isFullscreen ? GLFW_FALSE : GLFW_TRUE);
        GLFW.glfwSetWindowMonitor(window, 0, xpos, ypos, width, height, -1);
    }

    @Override
    public void vitri$resize(int newWidth, int newHeight) {
        if (this.width == newWidth && this.height == newHeight) {
            return;
        }

        GLFW.glfwSetWindowSize(this.window, newWidth, newHeight);
        this.onResize(this.window, newWidth, newHeight);
        this.onFramebufferResize(this.window, framebufferWidth, framebufferHeight);
        this.eventHandler.resizeDisplay();
        this.updateDisplay();
    }
}
