package rearth.oritech.api.recipe.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.api.recipe.GrinderRecipeBuilder;
import rearth.oritech.api.recipe.OritechRecipeGenerator;
import rearth.oritech.api.recipe.PulverizerRecipeBuilder;

import java.util.List;
import java.util.function.Supplier;

public class RecipeHelpers {

    public static void addDustRecipe(RecipeOutput exporter, Ingredient ingot, ItemLike dust, String suffix) {
        addDustRecipe(exporter, ingot, dust, null, suffix);
    }

    public static void addDustRecipe(RecipeOutput exporter, Ingredient ingot, ItemLike dust, @Nullable ItemLike ingotSmelted, String suffix) {
        PulverizerRecipeBuilder.build().input(ingot).result(dust).export(exporter, suffix);
        GrinderRecipeBuilder.build().input(ingot).result(dust).export(exporter, suffix);
        if (ingotSmelted != null) {
            OritechRecipeGenerator.oreSmelting(exporter, List.of(dust), RecipeCategory.MISC, ingotSmelted, 1f, 200, Oritech.MOD_ID);
            OritechRecipeGenerator.oreBlasting(exporter, List.of(dust), RecipeCategory.MISC, ingotSmelted, 1f, 100, Oritech.MOD_ID);
        }
    }

    public static void addDustRecipe(RecipeOutput exporter, Ingredient ingot, Supplier<? extends ItemLike> dust, String suffix) {
        addDustRecipe(exporter, ingot, dust.get(), null, suffix);
    }

    public static void addDustRecipe(RecipeOutput exporter, Ingredient ingot, Supplier<? extends ItemLike> dust, @Nullable Supplier<? extends ItemLike> ingotSmelted, String suffix) {
        addDustRecipe(exporter, ingot, dust.get(), ingotSmelted != null ? ingotSmelted.get() : null, suffix);
    }

    public static RecipeBuilder createInsulatedCableRecipe(RecipeCategory category, Item output, int count, Ingredient input, Ingredient insulation) {
        return ShapedRecipeBuilder.shaped(BuiltInRegistries.ITEM, category, output, count).define('c', input).define('i', insulation).pattern("iii").pattern("ccc").pattern("iii");
    }

    public static RecipeBuilder createRotatedCableRecipe(RecipeCategory category, Item output, int count, Ingredient input, Ingredient insulation) {
        return ShapedRecipeBuilder.shaped(BuiltInRegistries.ITEM, category, output, count).define('c', input).define('i', insulation)
                .pattern("ici")
                .pattern("ici")
                .pattern("ici");
    }

    public static Ingredient of(ItemLike item) {
        return Ingredient.of(item);
    }

    public static Ingredient of(TagKey<Item> item) {
        return Ingredient.of(BuiltInRegistries.ITEM.get(item).orElseThrow());
    }
}
