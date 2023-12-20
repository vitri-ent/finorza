package io.pyke.vitri.finorza.inference.mixins;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.RecipeBookCategories;
import net.minecraft.world.item.crafting.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(ClientRecipeBook.class)
public class MixinClientRecipeBook {
	@Inject(
		method = "categorizeAndGroupRecipes(Ljava/lang/Iterable;)Ljava/util/Map;", at = @At(
		value = "INVOKE", shift = At.Shift.AFTER, target = "Ljava/util/ArrayList;add(Ljava/lang/Object;)Z"
	), locals = LocalCapture.CAPTURE_FAILHARD
	)
	private static void injectCategorize(
		Iterable<Recipe<?>> iterable, CallbackInfoReturnable<Map<RecipeBookCategories, List<List<Recipe<?>>>>> cir,
		ArrayList<Recipe<?>> list
	) {
		list.sort(Comparator.comparing(r -> r.getResultItem().toString()));
	}

	@Inject(
		method = "categorizeAndGroupRecipes(Ljava/lang/Iterable;)Ljava/util/Map;", at = @At("RETURN"),
		cancellable = true
	)
	private static void injectSorted(CallbackInfoReturnable<Map<RecipeBookCategories, List<List<Recipe<?>>>>> cir) {
		Map<RecipeBookCategories, List<List<Recipe<?>>>> map = cir.getReturnValue();
		for (List<List<Recipe<?>>> recipeList : map.values()) {
			for (List<Recipe<?>> subList : recipeList) {
				if (subList.size() > 1) {
					subList.sort(Comparator.comparing(r -> r.getResultItem().getItem().toString()));
				}
			}
			recipeList.sort(Comparator.comparing(r -> r.get(0).getResultItem().getItem().toString()));
		}
		cir.setReturnValue(map);
	}
}
