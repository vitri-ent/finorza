package io.pyke.vitri.finorza.inference.mixins.render;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.lwjgl.glfw.GLFW.GLFW_CURSOR;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_HIDDEN;

@Mixin(TitleScreen.class)
public class MixinTitleScreen {
	@Shadow
	private long fadeInStart;
	@Final
	@Shadow
	private boolean fading;

	@Inject(
		method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;IIF)V", at = @At(
		value = "HEAD"
	)
	)
	private void injectRender(PoseStack poseStack, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
		if (this.fadeInStart == 0L && this.fading) {
			Window window = Minecraft.getInstance().getWindow();
			GLFW.glfwSetInputMode(window.getWindow(), GLFW_CURSOR, GLFW_CURSOR_HIDDEN);
		}
	}
}
