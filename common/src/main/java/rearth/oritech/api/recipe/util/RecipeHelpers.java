package rearth.oritech.api.recipe.util;

import net.minecraft.data.server.recipe.RecipeExporter;
import net.minecraft.data.server.recipe.RecipeProvider;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.book.RecipeCategory;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import rearth.oritech.Oritech;
import rearth.oritech.api.recipe.PulverizerRecipeBuilder;
import java.util.List;
import org.jetbrains.annotations.Nullable;

public class RecipeHelpers {
    public static void addDustRecipe(RecipeExporter exporter, Ingredient ingot, Item dust, String suffix) {
        addDustRecipe(exporter, ingot, dust, null, suffix);
    }
    
    public static void addDustRecipe(RecipeExporter exporter, Ingredient ingot, Item dust, @Nullable Item ingotSmelted, String suffix) {
        PulverizerRecipeBuilder.build().input(ingot).result(dust).addToGrinder().export(exporter, suffix);
        if (ingotSmelted != null) {
            RecipeProvider.offerSmelting(exporter, List.of(dust), RecipeCategory.MISC, ingotSmelted, 1f, 200, Oritech.MOD_ID);
            RecipeProvider.offerBlasting(exporter, List.of(dust), RecipeCategory.MISC, ingotSmelted, 1f, 100, Oritech.MOD_ID);
        }
    }

    public static Ingredient of(ItemConvertible item) {
        return Ingredient.ofItems(item);
    }
    
    public static Ingredient of(TagKey<Item> item) {
        return Ingredient.fromTag(item);
    }

    public static TagKey<Item> cItemTag(String path) {
      return TagKey.of(RegistryKeys.ITEM, Identifier.of("c", path));
    }
}