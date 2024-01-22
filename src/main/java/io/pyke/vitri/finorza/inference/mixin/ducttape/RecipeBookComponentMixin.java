package io.pyke.vitri.finorza.inference.mixin.ducttape;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RecipeBookComponent.class)
public class RecipeBookComponentMixin {
	@Shadow
	private EditBox searchBox;

	@Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;IIF)V", at = @At("HEAD"), cancellable = true)
	private void render(CallbackInfo ci) {
		if (searchBox == null) {
			ci.cancel();
		}
	}
}
