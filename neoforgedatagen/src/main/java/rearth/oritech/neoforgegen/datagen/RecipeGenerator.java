package rearth.oritech.neoforgegen.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.conditions.IConditionBuilder;

import rearth.oritech.neoforgegen.datagen.compat.CreateRecipeGenerator;
import rearth.oritech.init.ItemContent;

import java.util.concurrent.CompletableFuture;

public class RecipeGenerator extends RecipeProvider implements IConditionBuilder {
    public RecipeGenerator(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput recipeOutput) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ItemContent.BANANA)
            .requires(Items.COBBLESTONE)
            .unlockedBy(getHasName(Items.COBBLESTONE), has(Items.COBBLESTONE))
            .save(recipeOutput);
        CreateRecipeGenerator.generateRecipes(recipeOutput);
    }
}
