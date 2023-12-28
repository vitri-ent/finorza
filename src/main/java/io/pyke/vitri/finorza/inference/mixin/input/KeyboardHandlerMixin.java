package io.pyke.vitri.finorza.inference.mixin.input;

import io.pyke.vitri.finorza.inference.util.Controller;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.Set;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin implements KeyboardHandlerAccessor {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Unique
    private final Set<Integer> keysPressed = new HashSet<>();

    @Unique
    private boolean useOriginalMethod = false;

    @Shadow(prefix = "original$")
    public abstract void original$keyPress(long windowPointer, int key, int scanCode, int action, int modifiers);

    @Inject(method = "keyPress(JIIII)V", at = @At(value = "HEAD"), cancellable = true)
    private void injectKeyPress(long windowPointer, int key, int scanCode, int action, int modifiers, CallbackInfo ci) {
        if (key == GLFW.GLFW_KEY_PAGE_DOWN && action == 1) {
            Controller.getInstance().toggleHumanControl();
            return;
        }

        if (action == 0) {
            keysPressed.remove(key);
        } else {
            keysPressed.add(key);
        }

        if (!useOriginalMethod && Controller.getInstance().hasAgentControl() && key != GLFW.GLFW_KEY_ESCAPE) {
            ci.cancel();
        }
    }

    @Inject(method = "charTyped(JII)V", at = @At(value = "HEAD"), cancellable = true)
    private void injectCharTyped(long windowPointer, int codePoint, int modifiers, CallbackInfo ci) {
        if (!useOriginalMethod && Controller.getInstance().hasAgentControl()) {
            ci.cancel();
        }
    }

    @Override
    public void vitri$onKey(int key, int scanCode, int action, int modifiers) {
        try {
            useOriginalMethod = true;
            this.original$keyPress(this.minecraft.getWindow().getWindow(), key, scanCode, action, modifiers);
        } finally {
            useOriginalMethod = false;
        }
    }

    @Override
    public boolean vitri$isKeyPressed(int key) {
        return this.keysPressed.contains(key);
    }
}
