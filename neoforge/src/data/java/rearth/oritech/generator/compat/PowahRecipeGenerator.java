package rearth.oritech.generator.compat;

import java.util.List;

import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.Tags;
import owmii.powah.block.energizing.EnergizingRecipe;
import rearth.oritech.Oritech;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.ItemContent;
import rearth.oritech.init.TagContent;

public class PowahRecipeGenerator {
    public static void generateRecipes(RecipeOutput exporter)     {
        exporter.accept(Oritech.id("compat/powah/energizing/fluxite"), new EnergizingRecipe(new ItemStack(ItemContent.FLUXITE), 12000, List.of(Ingredient.of(Tags.Items.GEMS_AMETHYST))), null);
        exporter.accept(Oritech.id("compat/powah/energizing/energite"), new EnergizingRecipe(new ItemStack(ItemContent.ENERGITE_INGOT), 20000, List.of(Ingredient.of(TagContent.NICKEL_INGOTS), Ingredient.of(ItemContent.FLUXITE))), null);
        exporter.accept(Oritech.id("compat/powah/energizing/uranite"), new EnergizingRecipe(new ItemStack(ItemContent.PLUTONIUM_DUST), 32000, List.of(Ingredient.of(BlockContent.URANIUM_CRYSTAL.asItem()))), null);
    }
}
