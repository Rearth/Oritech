package rearth.oritech.neoforgegen.datagen.compat;

import rearth.oritech.Oritech;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.init.recipes.RecipeContent;
import com.simibubi.create.AllItems;

import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;

public class CreateRecipeGenerator {
    public static void generateRecipes(RecipeOutput exporter) { 
        var foundryDefaultSpeed = 200;

        var entry = new OritechRecipe(foundryDefaultSpeed, List.of(Ingredient.of(Items.COPPER_INGOT), Ingredient.of(AllItems.ZINC_INGOT.asItem())), List.of(AllItems.BRASS_INGOT.asStack(2)), RecipeContent.FOUNDRY, null, null);
        exporter.accept(Oritech.id("foundry/alloy/create_brass"), entry, null);
    }
}