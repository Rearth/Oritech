package rearth.oritech.compat.datagen.recipe;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.conditions.ModLoadedCondition;
import owmii.powah.Powah;
import owmii.powah.block.energizing.EnergizingRecipe;
import rearth.oritech.Oritech;
import rearth.oritech.datagen.builders.CentrifugeRecipeBuilder;
import rearth.oritech.datagen.builders.LaserRecipeBuilder;
import rearth.oritech.datagen.builders.PulverizerRecipeBuilder;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.ItemContent;
import rearth.oritech.init.TagContent;

public class PowahCompatRecipeProvider extends RecipeProvider {
    private static final String PATH = "compat/powah";
    private final RecipeOutput modLoadedOutput;
    
    public PowahCompatRecipeProvider(RecipeOutput output, HolderLookup.Provider registries) {
        super(registries, output);

        this.modLoadedOutput = output.withConditions(new ModLoadedCondition(Powah.MOD_ID));
    }

    @Override
    protected void buildRecipes() {
        this.modLoadedOutput.accept(ResourceKey.create(Registries.RECIPE, Oritech.id(PATH + "/energizing/fluxite")),
            new EnergizingRecipe(new ItemStackTemplate(ItemContent.FLUXITE), 12000, List.of(Ingredient.of(this.items.get(Tags.Items.GEMS_AMETHYST).get()))), null);
        this.modLoadedOutput.accept(ResourceKey.create(Registries.RECIPE, Oritech.id(PATH + "/energizing/energite")),
            new EnergizingRecipe(new ItemStackTemplate(ItemContent.ENERGITE_INGOT), 20000, List.of(Ingredient.of(this.items.get(TagContent.NICKEL_INGOTS).get()), Ingredient.of(ItemContent.FLUXITE))), null);
        this.modLoadedOutput.accept(ResourceKey.create(Registries.RECIPE, Oritech.id(PATH + "/energizing/uranite")),
            new EnergizingRecipe(new ItemStackTemplate(ItemContent.PLUTONIUM_DUST), 32000, List.of(Ingredient.of(BlockContent.URANITE_CRYSTAL.asItem()))), null);
    }

    public static class Runner extends RecipeProvider.Runner {
        public Runner(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries);
        }

        @Override
        protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
            return new PowahCompatRecipeProvider(output, registries);
        }

        @Override
        public String getName() {
            return "Powah Oritech Compat";
        }
    }
    
}
