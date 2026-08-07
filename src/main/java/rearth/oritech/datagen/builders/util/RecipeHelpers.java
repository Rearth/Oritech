package rearth.oritech.datagen.builders.util;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.datagen.builders.GrinderRecipeBuilder;
import rearth.oritech.datagen.RecipeGenerator;
import rearth.oritech.datagen.builders.PulverizerRecipeBuilder;

import java.util.List;

public class RecipeHelpers {

    public static void addDustRecipe(RecipeOutput exporter, Ingredient ingot, ItemLike dust, String suffix, HolderLookup.Provider registryAccess) {
        addDustRecipe(exporter, ingot, dust, null, suffix, registryAccess);
    }

    public static void addDustRecipe(RecipeOutput exporter, Ingredient ingot, ItemLike dust, @Nullable ItemLike ingotSmelted, String suffix, HolderLookup.Provider registryAccess) {
        addDustRecipe(exporter, ingot, dust, ingotSmelted, null, suffix, registryAccess);
    }
    
    public static void addDustRecipe(RecipeOutput exporter, Ingredient ingot, ItemLike dust, @Nullable ItemLike ingotSmelted, String prefix, String suffix, HolderLookup.Provider registryAccess) {
        new PulverizerRecipeBuilder(registryAccess).input(ingot).result(dust).export(exporter, prefix, suffix, Oritech.MOD_ID);
        new GrinderRecipeBuilder(registryAccess).input(ingot).result(dust).export(exporter, prefix, suffix, Oritech.MOD_ID);
        if (ingotSmelted != null) {
            RecipeGenerator.oreSmelting(exporter, List.of(dust), RecipeCategory.MISC, ingotSmelted, 1f, 200, Oritech.MOD_ID);
            RecipeGenerator.oreBlasting(exporter, List.of(dust), RecipeCategory.MISC, ingotSmelted, 1f, 100, Oritech.MOD_ID);
        }
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

}
