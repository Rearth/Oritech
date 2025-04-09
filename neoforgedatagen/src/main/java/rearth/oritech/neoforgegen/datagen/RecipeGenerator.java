package rearth.oritech.neoforgegen.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.conditions.IConditionBuilder;
import net.neoforged.neoforge.common.conditions.WithConditions;

import rearth.oritech.neoforgegen.datagen.compat.CreateRecipeGenerator;
import rearth.oritech.init.ItemContent;

import java.util.concurrent.CompletableFuture;

public class RecipeGenerator extends RecipeProvider implements IConditionBuilder {
    PackOutput packOutput;
    CompletableFuture<HolderLookup.Provider> registries;

    public RecipeGenerator(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);

        this.packOutput = output;
        this.registries = registries;
    }

    @Override
    protected void buildRecipes(RecipeOutput recipeOutput) {
        CreateRecipeGenerator.generateRecipes(packOutput, registries, recipeOutput.withConditions(this.modLoaded("create")));
    }
}
