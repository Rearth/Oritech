package rearth.oritech.api.recipe.util;

import net.minecraft.data.server.recipe.CraftingRecipeJsonBuilder;
import net.minecraft.data.server.recipe.RecipeExporter;
import net.minecraft.data.server.recipe.RecipeProvider;
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.book.RecipeCategory;
import net.minecraft.registry.tag.TagKey;
import rearth.oritech.Oritech;
import rearth.oritech.api.recipe.GrinderRecipeBuilder;
import rearth.oritech.api.recipe.PulverizerRecipeBuilder;
import java.util.List;
import org.jetbrains.annotations.Nullable;

public class RecipeHelpers {
    public static void addDustRecipe(RecipeExporter exporter, Ingredient ingot, Item dust, String suffix) {
        addDustRecipe(exporter, ingot, dust, null, suffix);
    }
    
    public static void addDustRecipe(RecipeExporter exporter, Ingredient ingot, Item dust, @Nullable Item ingotSmelted, String suffix) {
        PulverizerRecipeBuilder.build().input(ingot).result(dust).export(exporter, suffix);
        GrinderRecipeBuilder.build().input(ingot).result(dust).time(140).export(exporter, suffix);
        if (ingotSmelted != null) {
            RecipeProvider.offerSmelting(exporter, List.of(dust), RecipeCategory.MISC, ingotSmelted, 1f, 200, Oritech.MOD_ID);
            RecipeProvider.offerBlasting(exporter, List.of(dust), RecipeCategory.MISC, ingotSmelted, 1f, 100, Oritech.MOD_ID);
        }
    }

    public static CraftingRecipeJsonBuilder createInsulatedCableRecipe(RecipeCategory category, Item output, int count, Ingredient input, Ingredient insulation) {
        return ShapedRecipeJsonBuilder.create(category, output, count).input('c', input).input('i', insulation).pattern("iii").pattern("ccc").pattern("iii");
    }

    public static Ingredient of(ItemConvertible item) {
        return Ingredient.ofItems(item);
    }
    
    public static Ingredient of(TagKey<Item> item) {
        return Ingredient.fromTag(item);
    }
}