package io.pyke.vitri.finorza.inference.mixin.ducttape;

import java.util.List;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeButton;
import net.minecraft.world.item.crafting.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RecipeButton.class)
public abstract class RecipeButtonMixin {
	@Shadow
	protected abstract List<Recipe<?>> getOrderedRecipes();

	@Inject(method = "Lnet/minecraft/client/gui/screens/recipebook/RecipeButton;renderButton(Lcom/mojang/blaze3d/vertex/PoseStack;IIF)V", at = @At(value = "HEAD"), cancellable = true)
	private void render(CallbackInfo ci) {
		if (this.getOrderedRecipes().isEmpty()) {
			ci.cancel();
		}
	}
}
