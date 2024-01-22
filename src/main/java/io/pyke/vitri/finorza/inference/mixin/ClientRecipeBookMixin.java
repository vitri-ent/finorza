package io.pyke.vitri.finorza.inference.mixin;

import java.util.*;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.RecipeBookCategories;
import net.minecraft.world.item.crafting.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(ClientRecipeBook.class)
public class ClientRecipeBookMixin {
    /**
     * @author
     * @reason
     */
    @Overwrite
    private static Map<RecipeBookCategories, List<List<Recipe<?>>>> categorizeAndGroupRecipes(Iterable<Recipe<?>> iterable) {
        HashMap<RecipeBookCategories, List<List<Recipe<?>>>> map = Maps.newHashMap();
        HashBasedTable table = HashBasedTable.create();
        for (Recipe<?> recipe : iterable) {
            if (recipe.isSpecial()) continue;
            RecipeBookCategories recipeBookCategories2 = ClientRecipeBook.getCategory(recipe);
            String string = recipe.getGroup();
            if (string.isEmpty()) {
                map.computeIfAbsent(recipeBookCategories2, recipeBookCategories -> Lists.newArrayList()).add(
                        ImmutableList.of(recipe));
                continue;
            }
            ArrayList<Recipe<?>> list = (ArrayList<Recipe<?>>)table.get((Object)recipeBookCategories2, string);
            if (list == null) {
                list = Lists.newArrayList();
                table.put(recipeBookCategories2, string, list);
                map.computeIfAbsent(recipeBookCategories2, recipeBookCategories -> Lists.newArrayList()).add(list);
            }
            list.add(recipe);
            list.sort(Comparator.comparing(r -> r.getResultItem().toString()));
        }
        for (final List<List<Recipe<?>>> recipeList : map.values()) {
            for (final List<Recipe<?>> subList : recipeList) {
                if (subList.size() > 1) {
                    subList.sort(Comparator.comparing(r -> r.getResultItem().getItem().toString()));
                }
            }
            recipeList.sort(Comparator.comparing(r -> r.get(0).getResultItem().getItem().toString()));
        }
        return map;
    }
}
