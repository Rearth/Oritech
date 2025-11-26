package rearth.oritech.api.recipe.util;

import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.api.recipe.GrinderRecipeBuilder;
import rearth.oritech.api.recipe.OritechRecipeGenerator;
import rearth.oritech.api.recipe.PulverizerRecipeBuilder;

import java.util.List;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

public class RecipeHelpers {
    
    public static void addDustRecipe(RecipeOutput exporter, Ingredient ingot, Item dust, String suffix) {
        addDustRecipe(exporter, ingot, dust, null, suffix);
    }
    
    public static void addDustRecipe(RecipeOutput exporter, Ingredient ingot, Item dust, @Nullable Item ingotSmelted, String suffix) {
        PulverizerRecipeBuilder.build().input(ingot).result(dust).export(exporter, suffix);
        GrinderRecipeBuilder.build().input(ingot).result(dust).time(140).export(exporter, suffix);
        if (ingotSmelted != null) {
            OritechRecipeGenerator.oreSmelting(exporter, List.of(dust), RecipeCategory.MISC, ingotSmelted, 1f, 200, Oritech.MOD_ID);
            OritechRecipeGenerator.oreBlasting(exporter, List.of(dust), RecipeCategory.MISC, ingotSmelted, 1f, 100, Oritech.MOD_ID);
        }
    }
    
    public static RecipeBuilder createInsulatedCableRecipe(RecipeCategory category, Item output, int count, Ingredient input, Ingredient insulation) {
        return ShapedRecipeBuilder.shaped(category, output, count).define('c', input).define('i', insulation).pattern("iii").pattern("ccc").pattern("iii");
    }
    
    public static RecipeBuilder createRotatedCableRecipe(RecipeCategory category, Item output, int count, Ingredient input, Ingredient insulation) {
        return ShapedRecipeBuilder.shaped(category, output, count).define('c', input).define('i', insulation)
                 .pattern("ici")
                 .pattern("ici")
                 .pattern("ici");
    }
    
    public static Ingredient of(ItemLike item) {
        return Ingredient.of(item);
    }
    
    public static Ingredient of(TagKey<Item> item) {
        return Ingredient.of(item);
    }
}