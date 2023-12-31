package io.pyke.vitri.finorza.inference.mixins.input;

import java.util.HashSet;
import java.util.Set;

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

import io.pyke.vitri.finorza.inference.api.IKeyboardHandler;
import io.pyke.vitri.finorza.inference.client.AgentControl;

@Mixin(KeyboardHandler.class)
public abstract class MixinKeyboardHandler implements IKeyboardHandler {
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
			AgentControl.setHumanControl(AgentControl.hasAgentControl());
			return;
		}

		if (action == 0) {
			keysPressed.remove(key);
		} else {
			keysPressed.add(key);
		}

		if (!useOriginalMethod && AgentControl.hasAgentControl() && key != GLFW.GLFW_KEY_ESCAPE) {
			ci.cancel();
		}
	}

	@Inject(method = "charTyped(JII)V", at = @At(value = "HEAD"), cancellable = true)
	private void injectCharTyped(long windowPointer, int codePoint, int modifiers, CallbackInfo ci) {
		if (!useOriginalMethod && AgentControl.hasAgentControl()) {
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

	public boolean vitri$isKeyPressed(int key) {
		return this.keysPressed.contains(key);
	}
}
